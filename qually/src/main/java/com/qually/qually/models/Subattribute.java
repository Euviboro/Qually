package com.qually.qually.models;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "subattributes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subattribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subattribute_id")
    private Integer subattributeId;

    @Column(name = "subattribute_text", nullable = false)
    private String subattributeText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private AuditQuestion auditQuestion;

    /**
     * Marks this subattribute as the accountability selector for its parent question.
     *
     * <p>In ACCOUNTABILITY protocols exactly one subattribute per question should have
     * this flag set. When a question is answered NO, the auditor must pick one of this
     * subattribute's options. Options flagged {@code isCompanyAccountable = true} count
     * against the score; all other options excuse the NO (treated as N/A in scoring).</p>
     *
     * <p>Has no effect in STANDARD protocols — the field is stored but ignored.</p>
     */
    @Column(name = "is_accountability", nullable = false)
    @Builder.Default
    private boolean isAccountability = false;

    @OneToMany(mappedBy = "subattribute", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubattributeOption> subattributeOptions = new ArrayList<>();
}