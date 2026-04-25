package com.qually.qually.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request payload for the standalone subattribute response endpoint.
 *
 * <p>Previously held {@code attributeId} (FK to subattributes) and
 * {@code answerValue} (free text). The schema change replaced
 * {@code subattribute_answer} and {@code subattribute_id} with a single
 * FK to {@code subattribute_options}, so the only thing needed from the
 * caller is which option was selected.</p>
 */
@Getter
@Setter
public class SubattributeResponseRequestDTO {

    @NotNull(message = "Audit response ID is required")
    private Long auditResponseId;

    /**
     * FK to {@code subattribute_options.subattribute_option_id}.
     * The subattribute itself is derived from the option.
     */
    @NotNull(message = "Subattribute option ID is required")
    private Long subattributeOptionId;
}
