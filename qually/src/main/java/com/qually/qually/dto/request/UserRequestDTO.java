package com.qually.qually.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Request payload for creating a new user.
 *
 * <p>The inline {@code UserRole} enum has been replaced by {@code roleId}
 * (FK to {@code roles.role_id}). {@code managerId} has been added to support
 * the self-referencing manager hierarchy.</p>
 */
@Getter
@Setter
public class UserRequestDTO {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email")
    private String userEmail;

    @NotBlank(message = "Full name is required")
    private String fullName;

    /** Optional FK to {@code roles.role_id}. */
    private Integer roleId;

    /** Optional FK to {@code users.user_id} of this user's manager. */
    private Integer managerId;
}
