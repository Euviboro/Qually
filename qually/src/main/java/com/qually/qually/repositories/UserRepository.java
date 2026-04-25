package com.qually.qually.repositories;

import com.qually.qually.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link User}.
 *
 * <p>{@code findAuditableUsersByClient} has been updated to filter by
 * {@code role.canBeAudited = true} instead of matching against a hardcoded
 * list of role name strings. This means adding a new auditable role only
 * requires setting its flag in the database — no code change needed.</p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUserEmail(String userEmail);

    List<User> findByRole_RoleName(String roleName);

    List<User> findByIsActiveTrue();

    List<User> findByRole_RoleNameAndIsActiveTrue(String roleName);

    /**
     * Returns active users whose role has {@code can_be_audited = true}
     * and who belong to the given client.
     *
     * <p>Replaces the previous version which matched against a hardcoded list
     * of role name strings. The flag-based approach means adding a new auditable
     * role (e.g. "Senior Agent") only requires an {@code UPDATE roles} in the DB.
     * </p>
     */
    @Query("""
        SELECT DISTINCT u FROM User u
        JOIN u.role r
        JOIN u.clients c
        WHERE r.canBeAudited = true
          AND c.clientId = :clientId
          AND u.isActive = true
    """)
    List<User> findAuditableUsersByClient(@Param("clientId") Integer clientId);

    List<User> findByFullNameContainingIgnoreCase(String fullName);
}
