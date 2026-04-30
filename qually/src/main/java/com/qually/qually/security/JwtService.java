package com.qually.qually.security;

import com.qually.qually.models.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Generates and validates JWTs for the Qually authentication flow.
 *
 * <p>The signing secret is read from the {@code JWT_SECRET} environment
 * variable at startup. The application refuses to start if the variable
 * is missing or shorter than 32 characters (256 bits — the minimum safe
 * length for HMAC-SHA256).</p>
 *
 * <p>Two token types are issued:</p>
 * <ul>
 *   <li><strong>Access token</strong> — 30 minutes, carries userId, email,
 *       department, and role. Sent as an {@code httpOnly} cookie on every
 *       authenticated request.</li>
 *   <li><strong>Refresh token</strong> — 8 hours, carries only userId.
 *       Exchanged for a new access token when the access token expires.</li>
 * </ul>
 *
 * <p>Tokens are stateless — there is no server-side token store. The only
 * way to invalidate all tokens at once is to rotate {@code JWT_SECRET}.</p>
 *
 * <p>Remove this service when Microsoft Auth replaces the PIN login flow.
 * The rest of the application does not reference this class directly —
 * only {@link JwtFilter} and {@link com.qually.qually.controllers.AuthController}
 * depend on it.</p>
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    /** 30 minutes in milliseconds. */
    private static final long ACCESS_TOKEN_EXPIRY_MS  = 30L  * 60 * 1000;
    /** 8 hours in milliseconds. */
    private static final long REFRESH_TOKEN_EXPIRY_MS = 8L   * 60 * 60 * 1000;
    /** Minimum secret length for HMAC-SHA256 (256 bits). */
    private static final int  MIN_SECRET_LENGTH       = 32;

    private SecretKey secretKey;

    /**
     * Reads {@code JWT_SECRET} from the environment and derives the signing
     * key. Called automatically by Spring after the bean is constructed.
     *
     * @throws IllegalStateException if the variable is missing or too short.
     */
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @PostConstruct
    public void init() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET is not configured. Set the JWT_SECRET environment variable.");
        }
        if (jwtSecret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least %d characters. Current length: %d."
                            .formatted(MIN_SECRET_LENGTH, jwtSecret.length()));
        }
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        log.info("JwtService initialised — access token expiry {}m, refresh token expiry {}h",
                ACCESS_TOKEN_EXPIRY_MS / 60_000,
                REFRESH_TOKEN_EXPIRY_MS / 3_600_000);
    }

    // ── Token generation ──────────────────────────────────────

    /**
     * Generates a signed access token for the given user.
     *
     * <p>Claims included: {@code sub} (userId), {@code email},
     * {@code department}, {@code role}. Expires in 30 minutes.</p>
     *
     * @param user The authenticated user.
     * @return Signed compact JWT string.
     */
    public String generateAccessToken(User user) {
        String department = (user.getRole() != null && user.getRole().getDepartment() != null)
                ? user.getRole().getDepartment().name()
                : null;
        String roleName = (user.getRole() != null)
                ? user.getRole().getRoleName()
                : null;

        return Jwts.builder()
                .subject(user.getUserId().toString())
                .claim("email",      user.getUserEmail())
                .claim("department", department)
                .claim("role",       roleName)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRY_MS))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Generates a signed refresh token for the given user.
     *
     * <p>Contains only {@code sub} (userId) — no other claims — to minimise
     * information exposure. Expires in 8 hours.</p>
     *
     * @param user The authenticated user.
     * @return Signed compact JWT string.
     */
    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .subject(user.getUserId().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRY_MS))
                .signWith(secretKey)
                .compact();
    }

    // ── Token validation ──────────────────────────────────────

    /**
     * Parses and validates a JWT string.
     *
     * @param token The compact JWT string to validate.
     * @return The verified {@link Claims} payload.
     * @throws io.jsonwebtoken.ExpiredJwtException if the token has expired.
     * @throws JwtException if the token is malformed or the signature is invalid.
     */
    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ── Convenience extractors ────────────────────────────────

    /**
     * Extracts the {@code userId} from a validated token.
     *
     * @param token The compact JWT string.
     * @return The user ID as an {@link Integer}.
     * @throws JwtException if the token is invalid or expired.
     */
    public Integer extractUserId(String token) {
        return Integer.parseInt(validateToken(token).getSubject());
    }

    /**
     * Returns the access token expiry in milliseconds.
     * Used by {@link com.qually.qually.controllers.AuthController} to set
     * the {@code Max-Age} attribute on the access token cookie.
     */
    public long getAccessTokenExpiryMs() {
        return ACCESS_TOKEN_EXPIRY_MS;
    }

    /**
     * Returns the refresh token expiry in milliseconds.
     * Used by {@link com.qually.qually.controllers.AuthController} to set
     * the {@code Max-Age} attribute on the refresh token cookie.
     */
    public long getRefreshTokenExpiryMs() {
        return REFRESH_TOKEN_EXPIRY_MS;
    }
}