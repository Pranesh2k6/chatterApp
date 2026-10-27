package com.example.chat.config;

import com.example.chat.repository.UserRepository;
import com.example.chat.service.PresenceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Intercepts every authenticated request and updates the user's presence.
 * Uses UserRepository directly (not UserService) to avoid circular deps.
 * The PresenceService internally throttles DB writes to once per 10 seconds.
 */
@Component
public class PresenceInterceptor implements HandlerInterceptor {

    private final PresenceService presenceService;
    private final UserRepository userRepository;

    public PresenceInterceptor(PresenceService presenceService, UserRepository userRepository) {
        this.presenceService = presenceService;
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {
            String username = auth.getName();
            userRepository.findByUsername(username).ifPresent(user ->
                presenceService.heartbeat(user.getId())
            );
        }
        return true; // always continue
    }
}
