package com.qually.qually.repositories;

import com.qually.qually.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUserEmail(String userEmail);

    List<User> findByRole_RoleName(String roleName);

    List<User> findByIsActiveTrue();

    List<User> findByRole_RoleNameAndIsActiveTrue(String roleName);

    /**
     * Returns active users whose role has {@code can_be_audited = true}
     * and who belong to the given client.
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

    /**
     * Returns all users who report directly to the given manager.
     * Used by {@link com.qually.qually.services.DisputeService#getInbox}
     * to find QA subordinates when building the inbox for a QA user above
     * the specialist level.
     */
    List<User> findByManager_UserId(Integer managerId);

    List<User> findByFullNameContainingIgnoreCase(String fullName);

    @Query(value = """
    WITH RECURSIVE subordinates AS (
        SELECT user_id FROM users WHERE manager_id = :userId
        UNION ALL
        SELECT u.user_id FROM users u
        JOIN subordinates s ON u.manager_id = s.user_id
    )
    SELECT user_id FROM subordinates
    """, nativeQuery = true)
    List<Integer> findAllSubordinateIds(@Param("userId") Integer userId);
}