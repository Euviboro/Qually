package com.qually.qually.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
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

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter               jwtFilter;
    private final CorsConfigurationSource corsConfigurationSource;
    private final CustomOAuth2UserService oAuth2UserService;
    private final OAuthSuccessHandler     oAuthSuccessHandler;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public SecurityConfig(JwtFilter jwtFilter,
                          CorsConfigurationSource corsConfigurationSource,
                          CustomOAuth2UserService oAuth2UserService,
                          OAuthSuccessHandler oAuthSuccessHandler) {
        this.jwtFilter             = jwtFilter;
        this.corsConfigurationSource = corsConfigurationSource;
        this.oAuth2UserService     = oAuth2UserService;
        this.oAuthSuccessHandler   = oAuthSuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/me", "/api/auth/logout").permitAll()
                        .requestMatchers("/oauth2/**", "/login/**").permitAll()
                        .anyRequest().authenticated())

                .oauth2Login(oauth2 -> oauth2
                        .loginPage(frontendUrl + "/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                // oidcUserService — not userService — because Azure AD
                                // returns an OIDC token (openid scope). Spring routes
                                // OIDC providers through OidcUserService, not
                                // DefaultOAuth2UserService.
                                .oidcUserService(oAuth2UserService))
                        .successHandler(oAuthSuccessHandler)
                        .failureUrl(frontendUrl + "/login?error=true"))

                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

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

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (request.getRequestURI().startsWith("/api/")) {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json");
                                response.getWriter().write(
                                        "{\"status\":401,\"code\":\"UNAUTHORIZED\"," +
                                                "\"message\":\"Authentication required\"}");
                            } else {
                                response.sendRedirect("/oauth2/authorization/azure");
                            }
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

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}