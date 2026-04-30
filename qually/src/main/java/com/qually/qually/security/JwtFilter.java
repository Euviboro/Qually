package com.qually.qually.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Intercepts every HTTP request, extracts the access token from the
 * {@code access_token} httpOnly cookie, validates it via {@link JwtService},
 * and populates the Spring Security context with the authenticated user's ID.
 *
 * <p>The filter never blocks requests directly — blocking is Spring Security's
 * responsibility based on what {@code SecurityConfig} requires and what this
 * filter puts in the security context.</p>
 *
 * <p>Two special 401 error codes are returned to allow the frontend to
 * distinguish between expired and invalid tokens:</p>
 * <ul>
 *   <li>{@code TOKEN_EXPIRED} — the frontend should attempt a silent refresh</li>
 *   <li>{@code TOKEN_INVALID} — the frontend should log out immediately</li>
 * </ul>
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    private static final String ACCESS_TOKEN_COOKIE = "access_token";

    /** Paths that never require a token — skip validation entirely. */
    private static final List<String> SKIP_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/logout"
    );

    private final JwtService jwtService;

    public JwtFilter(JwtService jwtService) {
        this.jwtService   = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Skip OPTIONS preflight — browsers send these with no cookies and
        // Spring Security must return 200 for CORS to work
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip auth endpoints — no token needed, no point parsing
        String path = request.getRequestURI();
        if (SKIP_PATHS.stream().anyMatch(path::startsWith)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract access token cookie
        String token = extractCookie(request, ACCESS_TOKEN_COOKIE);

        if (token == null) {
            // No cookie present — let the request through unauthenticated.
            // SecurityConfig will reject it if the endpoint requires auth.
            filterChain.doFilter(request, response);
            return;
        }

        // Validate and populate security context
        try {
            Integer userId = jwtService.extractUserId(token);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,   // principal — controllers read this
                            null,     // credentials — not needed post-auth
                            List.of() // authorities — authorization is in services
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Authenticated request for user {} — {}", userId, path);

        } catch (ExpiredJwtException ex) {
            log.debug("Expired access token on request to {}", path);
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "TOKEN_EXPIRED",
                    "Access token has expired — please refresh");
            return;

        } catch (JwtException ex) {
            log.warn("Invalid JWT on request to {} — {}", path, ex.getMessage());
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "TOKEN_INVALID",
                    "Access token is invalid — please log in again");
            return;
        }

        filterChain.doFilter(request, response);
    }

    // ── Helpers ───────────────────────────────────────────────

    /**
     * Extracts a named cookie value from the request.
     *
     * @return The cookie value, or {@code null} if the cookie is absent.
     */
    private String extractCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        return Arrays.stream(cookies)
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * Writes a JSON error body and sets the response status.
     * Used instead of throwing an exception so the response is fully under
     * our control rather than being re-processed by Spring's error handling.
     */
    private void writeError(HttpServletResponse response,
                            int status,
                            String code,
                            String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"status\":%d,\"code\":\"%s\",\"message\":\"%s\"}"
                        .formatted(status, code, message)
        );
    }
}