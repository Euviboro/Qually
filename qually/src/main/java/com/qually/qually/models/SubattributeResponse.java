package com.qually.qually.models;

import jakarta.persistence.*;
import lombok.*;

/**
 * Records which option a user selected for a specific subattribute
 * when answering a question with NO.
 *
 * <p>The entity maps directly to the {@code subattribute_responses} table
 * which has three columns: {@code subattribute_response_id},
 * {@code audit_response_id}, and {@code subattribute_option_id}.</p>
 *
 * <p>The subattribute itself is derivable by joining
 * {@code subattribute_options → subattributes}, so it is not stored
 * redundantly here. The selected option label is derivable the same way.</p>
 */
@Entity
@Table(name = "subattribute_responses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubattributeResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subattribute_response_id")
    private Long subattributeResponseId;

    /**
     * The audit response (YES/NO/N/A answer) this subattribute selection
     * belongs to. A subattribute response only exists when the parent
     * answer is NO.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_response_id", nullable = false)
    private AuditResponse auditResponse;

    /**
     * The specific option the auditor selected for this subattribute.
     * The option carries its label and its parent subattribute reference,
     * so nothing else needs to be stored here.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subattribute_option_id", nullable = false)
    private SubattributeOption selectedOption;
}
