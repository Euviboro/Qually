package com.qually.qually.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Response DTO for a {@link com.qually.qually.models.Lob}.
 *
 * <p>{@code teamLeaderEmail} and {@code teamLeaderName} have been removed —
 * the {@code lobs} table has no team leader column.</p>
 */
@Getter
@Builder
public class LobResponseDTO {
    private Integer lobId;
    private String lobName;
    private Integer clientId;
    private String clientName;
}
