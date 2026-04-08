package com.qually.qually.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "audit_responses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_response_id")
    private Long auditResponseId;

    @Column(name = "question_answer")
    private String questionAnswer;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private AuditSession auditSession;

    @ManyToOne
    @JoinColumn(name = "question_id")
    private AuditQuestion auditQuestion;
}