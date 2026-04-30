package com.qually.qually.controllers;

import com.qually.qually.dto.request.AuthRequestDTO;
import com.qually.qually.dto.response.AuthResponseDTO;
import com.qually.qually.models.User;
import com.qually.qually.repositories.UserRepository;
import com.qually.qually.security.JwtService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles authentication — login, token refresh, logout, and PIN change.
 *
 * <p>Tokens are issued as {@code httpOnly} cookies so JavaScript cannot
 * read or steal them. The CSRF token is handled separately by Spring
 * Security's {@code CookieCsrfTokenRepository}.</p>
 *
 * <p>Login is rate-limited to 5 attempts per IP per minute using Bucket4j.
 * Each IP gets its own token bucket stored in a {@link ConcurrentHashMap}.
 * This is an in-memory store — buckets reset on restart, which is acceptable
 * for a single-instance deployment. Switch to a Redis-backed store if the
 * app scales horizontally.</p>
 *
 * <p>Remove this controller when Microsoft Auth replaces the PIN login.
 * Only {@code /api/auth/logout} would be retained to clear cookies.</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private static final String ACCESS_TOKEN_COOKIE  = "access_token";
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final int    MAX_LOGIN_ATTEMPTS   = 5;

    private final UserRepository userRepository;
    private final JwtService     jwtService;
    private final PasswordEncoder passwordEncoder;

    /** Per-IP rate-limit buckets. */
    private final ConcurrentHashMap<String, Bucket> rateLimitBuckets =
            new ConcurrentHashMap<>();

    public AuthController(UserRepository userRepository,
                          JwtService jwtService,
                          PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.jwtService      = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    // ── Login ─────────────────────────────────────────────────

    /**
     * Validates credentials and issues access + refresh token cookies.
     *
     * <p>Rate-limited to {@value MAX_LOGIN_ATTEMPTS} attempts per IP per minute.
     * Returns 429 when the limit is exceeded.</p>
     *
     * @return {@link AuthResponseDTO} — UI metadata. Tokens are in cookies.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequestDTO dto,
                                   HttpServletRequest req,
                                   HttpServletResponse res) {
        String ip = getClientIp(req);

        // Rate limit check
        if (!getBucket(ip).tryConsume(1)) {
            log.warn("Rate limit exceeded on login from IP {}", ip);
            return ResponseEntity.status(429).body(Map.of(
                    "status",  429,
                    "code",    "RATE_LIMITED",
                    "message", "Too many login attempts. Please wait a minute and try again."));
        }

        // Look up user
        User user = userRepository.findByUserEmail(dto.getEmail().trim().toLowerCase())
                .orElse(null);

        // Validate — same error for unknown email and wrong PIN to prevent enumeration
        if (user == null
                || !Boolean.TRUE.equals(user.getIsActive())
                || user.getPinHash() == null
                || !passwordEncoder.matches(dto.getPin(), user.getPinHash())) {
            log.warn("Failed login attempt for email '{}' from IP {}", dto.getEmail(), ip);
            return ResponseEntity.status(401).body(Map.of(
                    "status",  401,
                    "code",    "INVALID_CREDENTIALS",
                    "message", "Invalid email or PIN."));
        }

        issueTokenCookies(user, res);

        log.info("User {} logged in from IP {}", user.getUserId(), ip);

        return ResponseEntity.ok(toAuthResponse(user));
    }

    // ── Refresh ───────────────────────────────────────────────

    /**
     * Validates the refresh token cookie and issues a new access token.
     * The refresh token cookie is also rotated for added security.
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest req,
                                     HttpServletResponse res) {
        String refreshToken = extractCookie(req, REFRESH_TOKEN_COOKIE);

        if (refreshToken == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "status",  401,
                    "code",    "NO_REFRESH_TOKEN",
                    "message", "No refresh token found."));
        }

        try {
            Integer userId = jwtService.extractUserId(refreshToken);
            User user = userRepository.findById(userId)
                    .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                    .orElse(null);

            if (user == null) {
                return ResponseEntity.status(401).body(Map.of(
                        "status",  401,
                        "code",    "USER_NOT_FOUND",
                        "message", "User no longer exists or is inactive."));
            }

            issueTokenCookies(user, res);
            log.debug("Tokens refreshed for user {}", userId);
            return ResponseEntity.ok(toAuthResponse(user));

        } catch (Exception ex) {
            log.warn("Invalid refresh token — {}", ex.getMessage());
            return ResponseEntity.status(401).body(Map.of(
                    "status",  401,
                    "code",    "INVALID_REFRESH_TOKEN",
                    "message", "Refresh token is invalid or expired. Please log in again."));
        }
    }

    // ── Logout ────────────────────────────────────────────────

    /**
     * Clears both token cookies by overwriting them with zero-age cookies.
     * The security context is also cleared server-side.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse res) {
        clearCookie(res, ACCESS_TOKEN_COOKIE);
        clearCookie(res, REFRESH_TOKEN_COOKIE);
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok().build();
    }

    // ── Change PIN ────────────────────────────────────────────

    /**
     * Allows an authenticated user to change their own PIN.
     * Sets {@code force_pin_change = false} on success.
     *
     * @param body Must contain {@code currentPin} and {@code newPin}.
     */
    @PostMapping("/change-pin")
    public ResponseEntity<?> changePin(@RequestBody Map<String, String> body,
                                       HttpServletRequest req,
                                       HttpServletResponse res) {
        Integer userId = (Integer) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();

        User user = userRepository.findById(userId)
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "status", 404, "message", "User not found."));
        }

        String currentPin = body.get("currentPin");
        String newPin     = body.get("newPin");

        if (currentPin == null || newPin == null || newPin.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400, "message", "currentPin and newPin are required."));
        }
        if (!passwordEncoder.matches(currentPin, user.getPinHash())) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", 401,
                    "code",   "WRONG_CURRENT_PIN",
                    "message", "Current PIN is incorrect."));
        }
        if (newPin.equals(currentPin)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400, "message", "New PIN must differ from the current PIN."));
        }

        user.setPinHash(passwordEncoder.encode(newPin));
        user.setForcePinChange(false);
        userRepository.save(user);

        // Re-issue cookies with updated user state
        issueTokenCookies(user, res);

        log.info("User {} changed their PIN", userId);
        return ResponseEntity.ok(toAuthResponse(user));
    }

    // ── Helpers ───────────────────────────────────────────────

    private void issueTokenCookies(User user, HttpServletResponse res) {
        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        res.addHeader("Set-Cookie", buildCookie(
                ACCESS_TOKEN_COOKIE,
                accessToken,
                jwtService.getAccessTokenExpiryMs() / 1000));

        res.addHeader("Set-Cookie", buildCookie(
                REFRESH_TOKEN_COOKIE,
                refreshToken,
                jwtService.getRefreshTokenExpiryMs() / 1000));
    }

    private String buildCookie(String name, String value, long maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite("Strict")
                .build()
                .toString();
    }

    private void clearCookie(HttpServletResponse res, String name) {
        res.addHeader("Set-Cookie", ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build()
                .toString());
    }

    private AuthResponseDTO toAuthResponse(User user) {
        return AuthResponseDTO.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .roleName(user.getRole() != null ? user.getRole().getRoleName() : null)
                .department(user.getRole() != null ? user.getRole().getDepartment() : null)
                .canBeAudited(user.getRole() != null
                        ? user.getRole().getCanBeAudited() : false)
                .canRaiseDispute(user.getRole() != null
                        ? user.getRole().getCanRaiseDispute() : false)
                .forcePinChange(Boolean.TRUE.equals(user.getForcePinChange()))
                .build();
    }

    private String extractCookie(HttpServletRequest req, String name) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) return null;
        return Arrays.stream(cookies)
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private String getClientIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private Bucket getBucket(String ip) {
        return rateLimitBuckets.computeIfAbsent(ip, key ->
                Bucket.builder()
                        .addLimit(Bandwidth.builder()
                                .capacity(MAX_LOGIN_ATTEMPTS)
                                .refillGreedy(MAX_LOGIN_ATTEMPTS, Duration.ofMinutes(1))
                                .build())
                        .build());
    }
}