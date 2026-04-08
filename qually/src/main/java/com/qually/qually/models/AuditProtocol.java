package com.qually.qually.models;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "audit_protocols")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditProtocol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "protocol_id")
    private Integer protocolId;

    @Column(name = "protocol_name", nullable = false, length = 100)
    private String protocolName;

    @Column(name = "protocol_version", nullable = false)
    private Integer protocolVersion;

    @Column(name = "is_finalized", nullable = false)
    private Boolean isFinalized;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @OneToMany(mappedBy = "auditProtocol", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AuditQuestion> auditQuestions = new ArrayList<>();
}