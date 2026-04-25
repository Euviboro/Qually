package com.qually.qually.models;

import com.qually.qually.models.enums.Department;
import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a role in the {@code roles} table.
 *
 * <p><strong>Capability flags (items 6 + 7):</strong></p>
 * <ul>
 *   <li>{@code canBeAudited} — when {@code true}, users with this role may be
 *       selected as {@code memberAuditedUser} on an audit session. Replaces the
 *       hardcoded string list {@code ["Team Member", "Supervisor", "Team Leader"]}
 *       in {@code UserService} and the magic hierarchy number in
 *       {@code ResultsService}.</li>
 *   <li>{@code canRaiseDispute} — when {@code true}, users with this role may
 *       formally raise a dispute against a flagged response. Replaces the
 *       hardcoded {@code TEAM_LEADER_HIERARCHY = 6} constant in
 *       {@code DisputeService}.</li>
 * </ul>
 *
 * <p>Adding a new role only requires setting its flags correctly in the database
 * — no code deployment is needed. Business rules read as self-documenting
 * predicates: {@code user.getRole().isCanBeAudited()}.</p>
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Integer roleId;

    @Column(name = "role_name", nullable = false, length = 100)
    private String roleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "department", nullable = false)
    private Department department;

    /** Lower number = higher authority within the department. */
    @Column(name = "hierarchy_level", nullable = false)
    private Integer hierarchyLevel;

    /**
     * When {@code true}, users with this role may be selected as the
     * member being audited on a session.
     * Seeded values: Team Member, Supervisor, Team Leader.
     */
    @Column(name = "can_be_audited", nullable = false)
    @Builder.Default
    private Boolean canBeAudited = false;

    /**
     * When {@code true}, users with this role may formally raise a
     * dispute against a flagged audit response.
     * Seeded values: Team Leader, Supervisor, Account Manager, Operations Manager.
     */
    @Column(name = "can_raise_dispute", nullable = false)
    @Builder.Default
    private Boolean canRaiseDispute = false;
}
