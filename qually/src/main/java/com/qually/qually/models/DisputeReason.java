package com.qually.qually.models;

import jakarta.persistence.*;
import lombok.*;

/** Predefined reason for raising an audit dispute. Seeded at schema creation. */
@Entity
@Table(name = "dispute_reasons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputeReason {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reason_id")
    private Integer reasonId;

    @Column(name = "reason_text", nullable = false, length = 200)
    private String reasonText;
}
