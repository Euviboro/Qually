package com.qually.qually.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubattributeOptionRequestDTO {

    /**
     * ID of the parent subattribute.
     *
     * <p>Required only in the <strong>standalone flow</strong>
     * ({@code SubattributeOptionService}). Null in the nested flow
     * (question create/update) — the mapper ignores it there and resolves
     * the parent from the method parameter instead.</p>
     *
     * <p>Not annotated with {@code @NotNull} intentionally: Bean Validation
     * cannot distinguish the two flows at the DTO level. The standalone service
     * performs its own null check and throws {@code IllegalArgumentException}
     * when this field is missing.</p>
     */
    private Integer subattributeId;

    /**
     * Display label for this answer choice (e.g. "Company", "Agent", "External").
     * Required in both flows.
     */
    @NotBlank(message = "Option label is required")
    private String optionLabel;

    /**
     * Whether selecting this option means the company is accountable for the failure.
     * Only meaningful on options that belong to an accountability subattribute in an
     * ACCOUNTABILITY protocol. Defaults to {@code false}.
     */
    private boolean isCompanyAccountable;
}