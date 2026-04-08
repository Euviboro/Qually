package com.qually.qually.models;

import com.qually.qually.models.enums.AuditLogicType;
import com.qually.qually.models.enums.AuditStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "interaction_id")
    private String interactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_status", nullable = false)
    private AuditStatus auditStatus;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "protocol_id", nullable = false)
    private AuditProtocol auditProtocol;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auditor_email", referencedColumnName = "user_email", nullable = false)
    private User auditor;

    @Column(name = "started_at", insertable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_logic_type", nullable = false)
    private AuditLogicType auditLogicType;
}