package com.qually.qually.models;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "lob_name", nullable = false)
    private String lobName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_leader_email", referencedColumnName = "user_email")
    private User teamLeader;

}
