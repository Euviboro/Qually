package com.qually.qually.mappers;

import com.qually.qually.dto.response.CalibrationSessionResponseDTO;
import com.qually.qually.models.CalibrationSession;
import org.springframework.stereotype.Component;

/**
 * Maps {@link CalibrationSession} entities to
 * {@link CalibrationSessionResponseDTO}.
 *
 * <p>The expert answer is injected by the caller ({@link CalibrationGroupMapper})
 * rather than derived here — the session entity has no knowledge of who the
 * expert is or what they answered. The caller passes the expert answer only
 * when the round is closed and the caller is viewing their own session.</p>
 */
@Component
public class CalibrationSessionMapper {

    /**
     * Maps a session to a DTO.
     *
     * @param session     The session entity.
     * @param expertAnswer The expert's answer for this group — {@code null}
     *                    when the round is open or when mapping other
     *                    participants' sessions in the manager view.
     */
    public CalibrationSessionResponseDTO toDTO(CalibrationSession session,
                                               String expertAnswer) {
        return CalibrationSessionResponseDTO.builder()
                .calibrationSessionId(session.getCalibrationSessionId())
                .userId(session.getUser().getUserId())
                .userFullName(session.getUser().getFullName())
                .calibrationAnswer(session.getCalibrationAnswer())
                .isCalibrated(session.getIsCalibrated())
                .expertAnswer(expertAnswer)
                .answeredAt(session.getAnsweredAt())
                .build();
    }
}