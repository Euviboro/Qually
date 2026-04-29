package com.qually.qually.mappers;

import com.qually.qually.dto.request.LobRequestDTO;
import com.qually.qually.dto.response.LobResponseDTO;
import com.qually.qually.models.Client;
import com.qually.qually.models.Lob;
import org.springframework.stereotype.Component;

/**
 * Maps between {@link Lob} entities and their DTOs.
 * Extracted from the inline conversion that previously lived
 * in {@code LobService.toDTO()}.
 */
@Component
public class LobMapper {

    public LobResponseDTO toDTO(Lob lob) {
        return LobResponseDTO.builder()
                .lobId(lob.getLobId())
                .lobName(lob.getLobName())
                .clientId(lob.getClient().getClientId())
                .clientName(lob.getClient().getClientName())
                .build();
    }

    public Lob toEntity(LobRequestDTO dto, Client client) {
        return Lob.builder()
                .lobName(dto.getLobName())
                .client(client)
                .build();
    }
}