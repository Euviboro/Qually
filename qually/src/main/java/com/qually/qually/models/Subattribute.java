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

    @OneToMany(mappedBy = "subattribute", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubattributeOption> subattributeOptions = new ArrayList<>();
}