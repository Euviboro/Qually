package com.qually.qually.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Request payload for {@code POST /api/calibration/rounds}.
 *
 * <p>The creator is sourced from the JWT security context — not from this DTO.
 * The creator is automatically enrolled as a participant on the backend.</p>
 */
@Getter
@Setter
public class CalibrationRoundRequestDTO {

    @NotBlank(message = "Round name is required")
    private String roundName;

    @NotNull(message = "Client ID is required")
    private Integer clientId;

    @NotNull(message = "Protocol ID is required")
    private Integer protocolId;

    @NotNull(message = "Question ID is required")
    private Integer questionId;

    /**
     * Interaction IDs (calls/chats) for this round.
     * Typically 1–3. Must be unique within the request.
     * These are independent from audit session interaction IDs.
     */
    @NotEmpty(message = "At least one interaction ID is required")
    @Size(max = 10, message = "A round may have at most 10 interaction IDs")
    private List<@NotBlank(message = "Interaction ID must not be blank") String> interactionIds;

    /**
     * User IDs of all participants — including the expert and the creator.
     * The creator is added automatically even if omitted, but it is good
     * practice to include them explicitly.
     */
    @NotEmpty(message = "At least one participant is required")
    private List<Integer> participantUserIds;

    /**
     * The user ID of the expert. Must be present in {@code participantUserIds}.
     * The expert's answer becomes the reference for calibration comparison.
     */
    @NotNull(message = "Expert user ID is required")
    private Integer expertUserId;
}