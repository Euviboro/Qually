package com.qually.qually.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClientResponseDTO {
    private Integer clientId;
    private String clientName;
}