package com.qually.qually.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration.
 *
 * <p>{@code allowedHeaders} is now an explicit list instead of {@code "*"}.
 * Using {@code "*"} with {@code allowCredentials(true)} is a misconfiguration
 * in strict CORS contexts and exposes more headers than the application needs.
 * The two headers listed here are the only ones the frontend sends:
 * {@code Content-Type} for JSON payloads and {@code X-User-Id} for the
 * mock authentication context.</p>
 *
 * <p>When real authentication is added, append {@code Authorization} here.</p>
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "X-User-Id")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
