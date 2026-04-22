package com.qually.qually.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "subattribute_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubattributeOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subattribute_option_id")
    private Long subattributeOptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subattribute_id", nullable = false)
    private Subattribute subattribute;

    @Column(name = "option_label", length = 70)
    private String optionLabel;
}
