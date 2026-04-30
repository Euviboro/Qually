package com.qually.qually.exceptions;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralised exception → HTTP response mapping for all REST controllers.
 *
 * <p>Changes in this version:</p>
 * <ul>
 *   <li>{@link ExpiredJwtException} → 401 with code {@code TOKEN_EXPIRED}.
 *       Signals the frontend to attempt a silent token refresh.</li>
 *   <li>{@link JwtException} → 401 with code {@code TOKEN_INVALID}.
 *       Signals the frontend to log out immediately.</li>
 *   <li>Rate limit (429) is handled directly in {@code AuthController}
 *       via Bucket4j — no exception is thrown, so no handler is needed here.</li>
 *   <li>{@link NumberFormatException} handler updated — {@code X-User-Id}
 *       is no longer used so the message is generalised.</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(
            EntityNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        String firstError = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst()
                .orElse("Validation error");
        return buildResponse(HttpStatus.BAD_REQUEST, firstError, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(
            IllegalStateException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), null);
    }

    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<Map<String, Object>> handleNumberFormat(
            NumberFormatException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST,
                "Invalid numeric value in request: " + ex.getMessage(), null);
    }

    /**
     * Handles expired JWT tokens that escape {@link com.qually.qually.security.JwtFilter}
     * — e.g. if a token expires between the filter running and the controller executing.
     *
     * <p>Returns a distinct {@code TOKEN_EXPIRED} code so the frontend 401 interceptor
     * knows to attempt a silent refresh rather than logging the user out immediately.</p>
     */
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<Map<String, Object>> handleExpiredJwt(ExpiredJwtException ex) {
        log.debug("Expired JWT token caught by exception handler");
        return buildResponse(HttpStatus.UNAUTHORIZED, "Access token has expired — please refresh",
                "TOKEN_EXPIRED");
    }

    /**
     * Handles malformed or tampered JWT tokens.
     * Returns {@code TOKEN_INVALID} so the frontend logs the user out immediately.
     */
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<Map<String, Object>> handleJwtException(JwtException ex) {
        log.warn("Invalid JWT token caught by exception handler — {}", ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED,
                "Access token is invalid — please log in again", "TOKEN_INVALID");
    }

    /**
     * Catch-all for any exception not handled above.
     * Message is intentionally vague — full stack trace is logged for debugging.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unhandled exception reached GlobalExceptionHandler", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred", null);
    }

    // ── Helpers ───────────────────────────────────────────────

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status,
                                                              String message,
                                                              String code) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status",    status.value());
        body.put("error",     status.getReasonPhrase());
        body.put("message",   message);
        if (code != null) body.put("code", code);
        return ResponseEntity.status(status).body(body);
    }
}