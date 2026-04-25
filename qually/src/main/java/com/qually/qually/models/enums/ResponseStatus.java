package com.qually.qually.models.enums;

/**
 * Lifecycle status of a single {@link com.qually.qually.models.AuditResponse}.
 *
 * <ul>
 *   <li>{@link #ANSWERED} — default state after session submission.</li>
 *   <li>{@link #FLAGGED}  — member (or TL+) has raised an informal flag.
 *       No paper trail — if rejected by TL it returns to ANSWERED.</li>
 *   <li>{@link #DISPUTED} — Team Leader has formally raised the dispute.
 *       An entry now exists in {@code audit_disputes} and the parent session
 *       moves to {@code DISPUTED}.</li>
 *   <li>{@link #RESOLVED} — QA has resolved the dispute.</li>
 * </ul>
 */
public enum ResponseStatus {
    ANSWERED,
    FLAGGED,
    DISPUTED,
    RESOLVED
}
