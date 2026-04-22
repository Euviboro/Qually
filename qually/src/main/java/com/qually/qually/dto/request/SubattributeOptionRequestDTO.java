package com.qually.qually.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Request DTO for a single answer-choice option within a subattribute.
 *
 * <p>This DTO is shared by two distinct execution paths that have different
 * requirements for {@code subattributeId}:</p>
 *
 * <ul>
 *   <li><strong>Nested flow</strong> — used inside {@code SubattributeRequestDTO}
 *       when creating or updating a question via {@code AuditQuestionController}.
 *       {@code SubattributeOptionMapper.toEntity(dto, parent)} receives the parent
 *       {@code Subattribute} directly as a parameter and never reads
 *       {@code subattributeId}. The field is {@code null} in this path and that
 *       is correct.</li>
 *   <li><strong>Standalone flow</strong> — used by {@code SubattributeOptionService
 *       .createSubattributeOption}, which calls {@code subattributeRepository
 *       .findById(dto.getSubattributeId())} to resolve the parent. The field
 *       must be non-null here, and that is enforced by an explicit null check in
 *       the service rather than by Bean Validation.</li>
 * </ul>
 *
 * <p><strong>Why the annotation was removed:</strong> the previous
 * {@code @NotNull(groups = OnIndividualSave.class)} annotation caused Bean
 * Validation to reject every question-create request that included subattribute
 * options, because the nested path never sends {@code subattributeId}. Moving the
 * null guard into the service keeps the standalone contract intact without
 * polluting the nested path with an invalid constraint.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
     * Display label for this answer choice (e.g. "Yes", "No", "N/A").
     * Required in both flows.
     */
    @NotBlank(message = "Option label is required")
    private String optionLabel;
}