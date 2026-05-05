package com.qually.qually.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "subattribute_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubattributeOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subattribute_option_id")
    private Long subattributeOptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subattribute_id", nullable = false)
    private Subattribute subattribute;

    @Column(name = "option_label", length = 100)
    private String optionLabel;

    /**
     * Whether selecting this option means the company (LSG) is accountable for the failure.
     *
     * <p>Only meaningful when the parent {@link Subattribute#isAccountability()}
     * is {@code true} and the protocol uses {@code AuditLogicType.ACCOUNTABILITY}.</p>
     *
     * <p>Scoring rule: a NO answer whose accountability subattribute response points to an
     * option with {@code isCompanyAccountable = true} counts against the score (score = 0).
     * A NO answer pointing to an option with {@code isCompanyAccountable = false} (e.g.
     * "Agent" or "External") is excused — treated as N/A for scoring purposes.</p>
     */
    @Column(name = "is_company_accountable", nullable = false)
    @Builder.Default
    private boolean isCompanyAccountable = false;
}