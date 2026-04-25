package com.qually.qually.exceptions;

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
 * <p>Changes from the previous version:</p>
 * <ul>
 *   <li>{@link IllegalStateException} now maps to 403 Forbidden instead of
 *       falling through to the generic 500 handler. This is the exception
 *       type thrown by {@code DisputeService} for all permission denials
 *       (wrong department, insufficient hierarchy, no client access).</li>
 *   <li>{@link NumberFormatException} now maps to 400 Bad Request instead of
 *       500. Thrown by {@code DisputeController} and {@code ResultsController}
 *       when {@code X-User-Id} or a numeric request param is non-numeric.</li>
 *   <li>The generic {@link Exception} handler now logs the full stack trace
 *       before returning 500, so production errors on Render are debuggable.</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(EntityNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        String firstError = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst()
                .orElse("Validation error");
        return buildResponse(HttpStatus.BAD_REQUEST, firstError);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Maps permission denial errors to 403 Forbidden.
     *
     * <p>{@code DisputeService} uses {@link IllegalStateException} for all
     * permission checks: wrong department, insufficient hierarchy level, no
     * client access. Previously these fell through to the generic 500 handler,
     * making permission errors indistinguishable from server crashes.</p>
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(
            IllegalStateException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    /**
     * Maps malformed numeric inputs to 400 Bad Request.
     *
     * <p>Thrown by {@code Integer.parseInt(header)} in controllers when the
     * {@code X-User-Id} header or a numeric query param contains non-numeric
     * characters. Previously unhandled → 500.</p>
     */
    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<Map<String, Object>> handleNumberFormat(
            NumberFormatException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST,
                "Invalid numeric value in request: " + ex.getMessage());
    }

    /**
     * Catch-all for any exception not handled above.
     *
     * <p>The message returned to the client is intentionally vague to avoid
     * leaking implementation details. The full stack trace is logged at ERROR
     * level so the cause is visible in Render's log stream.</p>
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unhandled exception reached GlobalExceptionHandler", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred");
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status,
                                                               String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
