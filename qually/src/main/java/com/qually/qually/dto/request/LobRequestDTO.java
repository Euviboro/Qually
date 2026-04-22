package com.qually.qually.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request payload for creating or updating a Line of Business.
 *
 * <p>{@code teamLeaderEmail} removed — the {@code lobs} table has no
 * team leader column.</p>
 */
@Getter
@Setter
public class LobRequestDTO {

    @NotBlank(message = "LOB name is required")
    private String lobName;

    @NotNull(message = "Client ID is required")
    private Integer clientId;
}
