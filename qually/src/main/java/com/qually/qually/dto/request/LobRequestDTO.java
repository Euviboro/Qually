package com.qually.qually.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class LobRequestDTO {

    @NotBlank(message = "Lob name is required")
    private String lobName;

    @NotNull(message = "Client ID is required")
    private Integer clientId;

    @NotNull(message = "Team leader email is required")
    @Email(message = "Must be a valid email")
    private String teamLeaderEmail;
}
