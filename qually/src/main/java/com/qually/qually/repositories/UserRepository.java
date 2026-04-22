package com.qually.qually.repositories;

import com.qually.qually.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link User}.
 *
 * <p>The generic PK type has changed from {@code String} (user_email) to
 * {@code Integer} (user_id) to match the DB schema.</p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUserEmail(String userEmail);

    List<User> findByRole_RoleName(String roleName);

    List<User> findByFullNameContainingIgnoreCase(String fullName);
}
