package com.qually.qually.models;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents an application user.
 *
 * <p><strong>Schema alignment:</strong></p>
 * <ul>
 *   <li>PK is now {@code user_id} (auto-increment int). Previously {@code user_email}
 *       was the PK; it is now a plain unique column.</li>
 *   <li>The inline {@code UserRole} enum column has been replaced by a {@link Role}
 *       FK ({@code role_id}), matching the separate {@code roles} table.</li>
 *   <li>{@code manager_id} self-referencing FK added (nullable).</li>
 * </ul>
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "user_email", nullable = false, unique = true, length = 100)
    private String userEmail;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    /**
     * The user's role, defined in the {@code roles} table.
     * Nullable — a user may not have been assigned a role yet.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    /**
     * Optional reference to this user's direct manager.
     * Nullable — top-level users have no manager.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;
}
