package com.qually.qually.models.enums;

/**
 * Lifecycle status of a single {@link com.qually.qually.models.AuditResponse}.
 *
 * <ul>
 *   <li>{@link #ANSWERED} — default state after session submission.</li>
 *   <li>{@link #DISPUTED} — Team Leader has formally raised the dispute.
 *       An entry now exists in {@code audit_disputes} and the parent session
 *       moves to {@code DISPUTED}.</li>
 *   <li>{@link #RESOLVED} — QA has resolved the dispute.</li>
 * </ul>
 */
public enum ResponseStatus {
    ANSWERED,
    DISPUTED,
    RESOLVED
}
