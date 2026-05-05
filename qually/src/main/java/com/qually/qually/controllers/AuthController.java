package com.qually.qually.controllers;

import com.qually.qually.dto.response.AuthResponseDTO;
import com.qually.qually.models.User;
import com.qually.qually.repositories.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication endpoints — OAuth2 edition.
 *
 * <p>Login is now handled entirely by Spring Security's OAuth2 client
 * (Microsoft Entra ID). This controller only exposes two endpoints:</p>
 *
 * <ul>
 *   <li>{@code GET /api/auth/me} — returns the current user's display data
 *       by reading the user ID from the JWT in the {@code access_token} cookie.
 *       Called by the React app on startup to hydrate {@code AuthContext}.</li>
 *   <li>{@code POST /api/auth/logout} — clears both JWT cookies and the
 *       security context. The browser is then without a valid token and any
 *       subsequent API call will return 401.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String ACCESS_TOKEN_COOKIE  = "access_token";
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Returns the currently authenticated user's display data.
     *
     * <p>The {@link com.qually.qually.security.JwtFilter} has already validated
     * the {@code access_token} cookie and set the user ID as the principal.
     * We simply look up the user and return their DTO.</p>
     *
     * <p>The React app calls this on startup. If the cookie is missing or expired
     * the filter returns 401 before this method is reached, and the frontend
     * redirects to {@code /login}.</p>
     */
    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null
                || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof Integer userId)) {
            return ResponseEntity.status(401).build();
        }

        return userRepository.findById(userId)
                .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                .map(u -> ResponseEntity.ok(toAuthResponse(u)))
                .orElse(ResponseEntity.status(401).build());
    }

    /**
     * Clears both JWT cookies and the server-side security context.
     * The browser is immediately unauthenticated — no Microsoft session
     * is touched (the user remains signed in to Microsoft).
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        clearCookie(response, ACCESS_TOKEN_COOKIE);
        clearCookie(response, REFRESH_TOKEN_COOKIE);
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok().build();
    }

    // ── Helpers ───────────────────────────────────────────────

    private AuthResponseDTO toAuthResponse(User user) {
        return AuthResponseDTO.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .roleName(user.getRole() != null ? user.getRole().getRoleName() : null)
                .department(user.getRole() != null ? user.getRole().getDepartment().name() : null)
                .canBeAudited(user.getRole() != null && user.getRole().getCanBeAudited())
                .canRaiseDispute(user.getRole() != null && user.getRole().getCanRaiseDispute())
                .build();
    }

    private void clearCookie(HttpServletResponse response, String name) {
        response.addHeader("Set-Cookie", ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build()
                .toString());
    }
}