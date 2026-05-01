package com.qually.qually.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClientResponseDTO {
    private Integer clientId;
    private String  clientName;
    /**
     * Short uppercase abbreviation used in calibration round names.
     * May be null if not yet set for this client.
     */
    private String  clientAbbreviation;
}