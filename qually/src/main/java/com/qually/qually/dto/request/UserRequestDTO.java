package com.qually.qually.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserRequestDTO {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email")
    private String userEmail;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotNull(message = "Role is required")
    private Integer roleId;

    private Integer managerId;

    /** Client IDs to assign to this user via user_clients. */
    private List<Integer> clientIds;
}
