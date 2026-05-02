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
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security configuration for Qually.
 *
 * <p>Key decisions:</p>
 * <ul>
 *   <li>Stateless session — no HttpSession created or used.</li>
 *   <li>CSRF disabled — {@code SameSite=Strict} on the {@code access_token}
 *       cookie is the CSRF defense. The browser will never send the cookie
 *       on cross-site requests, eliminating the attack vector entirely.
 *       This is the OWASP-recommended approach for SPAs with same-site
 *       cookie deployment. No CSRF token required.</li>
 *   <li>JWT validation delegated to {@link JwtFilter}.</li>
 *   <li>All 401 and 403 responses return JSON — no HTML redirects.</li>
 *   <li>Security headers enforced for public-facing deployment.</li>
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
        this.jwtFilter               = jwtFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // ── Session — stateless ───────────────────────────
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ── CORS ──────────────────────────────────────────
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // ── CSRF — disabled ───────────────────────────────
                // SameSite=Strict on the access_token cookie prevents the browser
                // from sending it on any cross-site request, making CSRF impossible.
                // No token-based CSRF protection is needed for same-site SPA deployments.
                .csrf(csrf -> csrf.disable())

                // ── Endpoint authorization ────────────────────────
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated())

                // ── JWT filter ────────────────────────────────────
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

                // ── Security response headers ─────────────────────
                .headers(headers -> headers
                        .contentTypeOptions(content -> {})
                        .frameOptions(frame -> frame.deny())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31_536_000))
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "default-src 'self'; " +
                                                "script-src 'self'; " +
                                                "style-src 'self' 'unsafe-inline'; " +
                                                "img-src 'self' data:; " +
                                                "font-src 'self'; " +
                                                "connect-src 'self'; " +
                                                "frame-ancestors 'none'")))

                // ── Exception handling — JSON only ────────────────
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

                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }

    /**
     * BCrypt password encoder — work factor 12.
     * Injected into {@link com.qually.qually.controllers.AuthController}
     * and {@link com.qually.qually.services.UserService}.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}