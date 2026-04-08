package com.qually.qually.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LobResponseDTO {
    private Integer lobId;
    private String lobName;
    private Integer clientId;
    private String clientName;
    private String teamLeaderEmail;
    private String teamLeaderName;
}
