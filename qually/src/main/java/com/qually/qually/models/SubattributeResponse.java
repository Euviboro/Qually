package com.qually.qually.models;

import jakarta.persistence.*;
import lombok.*;

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
    private Long attributeResponseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_response_id", nullable = false)
    private AuditResponse auditResponse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subattribute_id", nullable = false)
    private Subattribute subattribute;

    @Column(name = "subattribute_answer", nullable = false, length = 100)
    private String answerValue;
}