package com.qually.qually.models;

import jakarta.persistence.*;
import lombok.*;

/**
 * Line of Business associated with a client.
 *
 * <p><strong>Schema alignment:</strong> the {@code teamLeader} {@link User}
 * reference has been removed. The {@code lobs} table only has
 * {@code lob_id}, {@code lob_name}, and {@code client_id}.</p>
 */
@Entity
@Table(name = "lobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lob_id")
    private Integer lobId;

    @Column(name = "lob_name", nullable = false, length = 100)
    private String lobName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;
}
