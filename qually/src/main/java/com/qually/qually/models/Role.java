package com.qually.qually.models;

import com.qually.qually.models.enums.Department;
import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a role in the {@code roles} table.
 *
 * <p>Roles are a first-class entity rather than a hardcoded enum, allowing
 * new roles to be added without a code deployment. Each role belongs to a
 * {@link Department} and carries a {@code hierarchyLevel} where a lower
 * number means higher authority.</p>
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
}
