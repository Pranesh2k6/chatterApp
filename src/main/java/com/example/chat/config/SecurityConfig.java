package com.example.chat.config;

import com.example.chat.model.User;
import com.example.chat.repository.UserRepository;
import com.example.chat.service.CustomUserDetailsService;
import com.example.chat.service.PresenceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import java.util.Optional;

/**
 * Spring Security configuration.
 *
 * Uses UserRepository directly (not UserService) to avoid a circular
 * dependency: SecurityConfig → UserService → PasswordEncoder → SecurityConfig.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final PresenceService presenceService;
    private final UserRepository userRepository;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          PresenceService presenceService,
                          UserRepository userRepository) {
        this.userDetailsService = userDetailsService;
        this.presenceService = presenceService;
        this.userRepository = userRepository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(authProvider())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/register", "/css/**", "/sounds/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/chat", true)
                .failureUrl("/login?error=true")
                .successHandler(loginSuccessHandler())
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler(logoutSuccessHandler())
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            // Allow same-origin iframes (sidebar, messages pane)
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            );

        return http.build();
    }

    // ── Login success: mark user online ────────────────────

    private AuthenticationSuccessHandler loginSuccessHandler() {
        return (HttpServletRequest request,
                HttpServletResponse response,
                Authentication authentication) -> {
            String username = authentication.getName();
            Optional<User> user = userRepository.findByUsername(username);
            user.ifPresent(u -> presenceService.setOnline(u.getId()));
            response.sendRedirect("/chat");
        };
    }

    // ── Logout success: mark user offline ──────────────────

    private LogoutSuccessHandler logoutSuccessHandler() {
        return (HttpServletRequest request,
                HttpServletResponse response,
                Authentication authentication) -> {
            if (authentication != null) {
                String username = authentication.getName();
                Optional<User> user = userRepository.findByUsername(username);
                user.ifPresent(u -> presenceService.setOffline(u.getId()));
            }
            response.sendRedirect("/login?logout=true");
        };
    }
}
