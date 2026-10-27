package com.example.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the PresenceInterceptor with Spring MVC.
 * Excludes static resources to avoid unnecessary processing.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final PresenceInterceptor presenceInterceptor;

    public WebConfig(PresenceInterceptor presenceInterceptor) {
        this.presenceInterceptor = presenceInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(presenceInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/css/**", "/sounds/**", "/login", "/register", "/error");
    }
}
