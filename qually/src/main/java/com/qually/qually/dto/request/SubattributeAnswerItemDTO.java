package com.qually.qually.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * A single subattribute option selection within a bulk response submission.
 *
 * <p>Sent by the frontend when a question is answered NO and the auditor
 * selects an option for one or more sub-criteria. The option ID is sufficient
 * to derive the subattribute, its label, and its parent question — no other
 * fields are needed.</p>
 */
@Getter
@Setter
public class SubattributeAnswerItemDTO {

    /**
     * The ID of the {@code SubattributeOption} the auditor selected.
     * FK to {@code subattribute_options.subattribute_option_id}.
     */
    @NotNull(message = "Subattribute option ID is required")
    private Long subattributeOptionId;
}
