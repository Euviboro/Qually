package com.qually.qually.models.enums;

/**
 * Represents the valid answer values for an audit question.
 *
 * <p>The database column {@code question_answer} and {@code new_answer} store
 * these as plain {@code VARCHAR} strings. This enum is used exclusively in
 * service and scoring logic where comparisons occur — the entity fields remain
 * {@code String} to avoid needing a custom Hibernate converter for the
 * {@code "N/A"} value (which is not a valid Java identifier for
 * {@link Enum#name()}).</p>
 *
 * <p>Usage pattern — instead of string literals:</p>
 * <pre>
 * // Before:
 * if ("NO".equals(response.getQuestionAnswer())) { ... }
 *
 * // After:
 * if (AuditAnswerType.NO.matches(response.getQuestionAnswer())) { ... }
 * // or parse first:
 * AuditAnswerType answer = AuditAnswerType.fromValue(response.getQuestionAnswer());
 * if (answer == AuditAnswerType.NO) { ... }
 * </pre>
 */
public enum AuditAnswerType {

    YES("YES"),
    NO("NO"),
    NA("N/A");

    private final String value;

    AuditAnswerType(String value) {
        this.value = value;
    }

    /** Returns the string value stored in the database and sent over the API. */
    public String getValue() {
        return value;
    }

    /**
     * Returns {@code true} when the given string matches this answer's DB value.
     * Null-safe — returns {@code false} for null input.
     *
     * @param candidate The raw string from the entity or DTO.
     */
    public boolean matches(String candidate) {
        return value.equals(candidate);
    }

    /**
     * Parses a raw DB/API string into the corresponding enum constant.
     *
     * @param value The string to parse (e.g. {@code "YES"}, {@code "N/A"}).
     * @return The matching {@link AuditAnswerType}.
     * @throws IllegalArgumentException if no constant matches the given value.
     */
    public static AuditAnswerType fromValue(String value) {
        for (AuditAnswerType type : values()) {
            if (type.value.equals(value)) return type;
        }
        throw new IllegalArgumentException(
                "Unknown answer value: '%s'. Expected one of: YES, NO, N/A".formatted(value));
    }
}
