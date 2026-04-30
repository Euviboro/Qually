package com.qually.qually.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security configuration for Qually.
 *
 * <p>Key decisions:</p>
 * <ul>
 *   <li>Stateless session — no HttpSession created or used</li>
 *   <li>CSRF enabled via readable XSRF-TOKEN cookie so the frontend
 *       can attach X-XSRF-Token to mutating requests</li>
 *   <li>JWT validation delegated to {@link JwtFilter}</li>
 *   <li>All 401 and 403 responses return JSON — no HTML redirects</li>
 *   <li>Security headers enforced for public-facing deployment</li>
 * </ul>
 *
 * <p>Replace this class when Microsoft Auth arrives — swap the JWT filter
 * for an OAuth2 resource server configuration. Everything else in the app
 * remains unchanged.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtFilter jwtFilter,
                          CorsConfigurationSource corsConfigurationSource) {
        this.jwtFilter              = jwtFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // ── CSRF ──────────────────────────────────────────────
        // CookieCsrfTokenRepository writes XSRF-TOKEN as a readable (non-httpOnly)
        // cookie. The frontend reads it and sends it back as X-XSRF-Token on
        // POST / PUT / PATCH / DELETE. The access_token cookie stays httpOnly.
        CookieCsrfTokenRepository csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();

        http
                // ── Session — stateless ───────────────────────────
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ── CORS — delegates to existing CorsConfig bean ──
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // ── CSRF ──────────────────────────────────────────
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepo)
                        .csrfTokenRequestHandler(csrfHandler)
                        // Auth endpoints are exempt — no token available yet at login
                        .ignoringRequestMatchers("/api/auth/login", "/api/auth/refresh",
                                "/api/auth/logout"))

                // ── Endpoint authorization ────────────────────────
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated())

                // ── JWT filter ────────────────────────────────────
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

                // ── Security response headers ─────────────────────
                .headers(headers -> headers
                        .contentTypeOptions(content -> {})   // X-Content-Type-Options: nosniff
                        .frameOptions(frame -> frame.deny())  // X-Frame-Options: DENY
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31_536_000))     // 1 year
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "default-src 'self'; " +
                                                "script-src 'self'; " +
                                                "style-src 'self' 'unsafe-inline'; " +
                                                "img-src 'self' data:; " +
                                                "font-src 'self'; " +
                                                "connect-src 'self'; " +
                                                "frame-ancestors 'none'")))

                // ── Exception handling — JSON responses only ──────
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"status\":401,\"code\":\"UNAUTHORIZED\"," +
                                            "\"message\":\"Authentication required\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"status\":403,\"code\":\"FORBIDDEN\"," +
                                            "\"message\":\"You do not have permission to access this resource\"}");
                        }))

                // ── Disable Spring's generated login page ─────────
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }

    /**
     * BCrypt password encoder used for hashing and verifying PINs.
     * Work factor 12 — high enough to be resistant to brute force,
     * low enough to be imperceptible on login.
     * Exposed as a bean so {@link com.qually.qually.controllers.AuthController}
     * and {@link com.qually.qually.services.UserService} can inject it.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}