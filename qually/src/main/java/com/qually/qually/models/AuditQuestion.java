package com.qually.qually.models;

import com.qually.qually.models.enums.CopcCategory;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "audit_questions")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuditQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Integer questionId;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private CopcCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "protocol_id", nullable = false)
    private AuditProtocol auditProtocol;

    @OneToMany(mappedBy = "auditQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Subattribute> subattributes = new ArrayList<>();
}