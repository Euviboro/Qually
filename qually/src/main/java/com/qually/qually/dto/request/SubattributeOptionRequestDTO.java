package com.qually.qually.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubattributeOptionRequestDTO {

    @NotBlank(message = "Option label is required")
    private String optionLabel;
}