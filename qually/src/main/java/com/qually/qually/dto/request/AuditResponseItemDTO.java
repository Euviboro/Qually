package com.qually.qually.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * A single question answer within a bulk response submission.
 *
 * <p>{@code subattributeAnswers} has been added to carry the subattribute
 * option selections made when a question is answered NO. These were
 * previously tracked in the frontend but silently dropped before reaching
 * the backend — they are now persisted to {@code subattribute_responses}.</p>
 *
 * <p>The list is optional (null or empty for YES and N/A answers). The
 * backend ignores subattribute answers when {@code questionAnswer} is
 * not NO, so sending them is harmless but unnecessary.</p>
 */
@Getter
@Setter
public class AuditResponseItemDTO {

    @NotNull(message = "Question ID is required")
    private Integer questionId;

    /** YES, NO, or N/A. */
    @NotNull(message = "Question answer is required")
    private String questionAnswer;

    /**
     * Subattribute option selections for this response.
     * Only meaningful when {@code questionAnswer = "NO"}.
     * Null or empty for YES and N/A answers.
     */
    private List<SubattributeAnswerItemDTO> subattributeAnswers;
}
