package com.qually.qually.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequestDTO {

    @NotBlank(message = "Full name is required")
    private String fullName;

    /** Optional — allows reassigning the user to a different role. */
    private Integer roleId;

    /** Optional — allows changing the user's manager. */
    private Integer managerId;
}
