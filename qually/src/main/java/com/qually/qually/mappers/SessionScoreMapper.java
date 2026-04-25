package com.qually.qually.mappers;

import com.qually.qually.dto.response.SessionScoreResponseDTO;
import com.qually.qually.models.SessionScore;
import org.springframework.stereotype.Component;

@Component
public class SessionScoreMapper {

    public SessionScoreResponseDTO toDTO(SessionScore score) {
        return SessionScoreResponseDTO.builder()
                .scoreId(score.getScoreId())
                .category(score.getCategory().name())
                .score(score.getScore())
                .isPostDispute(score.getIsPostDispute())
                .calculatedAt(score.getCalculatedAt())
                .build();
    }
}
