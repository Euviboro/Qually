package com.qually.qually.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "audited_processes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditedProcess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audited_process_id")
    private Integer auditedProcessId;

    @Column(name = "audited_process_name", nullable = false, length = 100)
    private String auditedProcessName;
}