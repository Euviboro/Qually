package com.qually.qually.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SessionScoreResponseDTO {
    private Integer scoreId;
    private String category;
    private Short score;
    /** {@code false} = original; {@code true} = post-dispute recalculation. */
    private Boolean isPostDispute;
    private LocalDateTime calculatedAt;
}
