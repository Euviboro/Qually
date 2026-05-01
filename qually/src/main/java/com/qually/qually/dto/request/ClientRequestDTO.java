package com.qually.qually.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClientRequestDTO {

    @NotBlank(message = "Client name is required")
    private String clientName;

    /**
     * Short uppercase abbreviation for calibration round name generation.
     * Optional — can be set later via an update. Must be 2–10 uppercase
     * letters when provided.
     */
    @Size(min = 2, max = 10, message = "Abbreviation must be between 2 and 10 characters")
    @Pattern(regexp = "^[A-Z0-9]*$", message = "Abbreviation must contain only uppercase letters and digits")
    private String clientAbbreviation;
}