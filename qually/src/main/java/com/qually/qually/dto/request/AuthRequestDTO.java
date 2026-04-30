package com.qually.qually.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Request payload for {@code POST /api/auth/login}.
 */
@Getter
@Setter
public class AuthRequestDTO {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "PIN is required")
    private String pin;
}