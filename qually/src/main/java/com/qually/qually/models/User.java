package com.qually.qually.models;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    /**
     * Whether this user is active. Inactive users are hidden from all
     * dropdowns and cannot log in. Historical records (sessions, disputes)
     * referencing this user are preserved.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * BCrypt hash of the user's PIN.
     * Seeded on first startup from the user's first name (lowercase, trimmed).
     * Set to null until DataSeeder runs. Removed entirely when Microsoft Auth
     * replaces the PIN login flow.
     */
    @Column(name = "pin_hash", length = 60)
    private String pinHash;

    /**
     * When true, the user is redirected to the Change PIN page after login
     * and cannot access the rest of the app until they set a new PIN.
     * Set to true by default for all new users and when an admin resets a PIN.
     */
    @Column(name = "force_pin_change", nullable = false)
    @Builder.Default
    private Boolean forcePinChange = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_clients",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "client_id")
    )
    @Builder.Default
    private List<Client> clients = new ArrayList<>();
}