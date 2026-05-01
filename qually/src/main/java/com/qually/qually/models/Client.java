package com.qually.qually.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "client_id")
    private Integer clientId;

    @Column(name = "client_name", nullable = false, unique = true)
    private String clientName;

    /**
     * Short uppercase abbreviation used in auto-generated calibration round names.
     * Example: "DSV", "LSG", "AMZN". Must be unique across clients.
     * Must be set before a calibration round can be created for this client.
     */
    @Column(name = "client_abbreviation", length = 10, unique = true)
    private String clientAbbreviation;
}