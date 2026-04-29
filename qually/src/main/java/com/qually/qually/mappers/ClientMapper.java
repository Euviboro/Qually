package com.qually.qually.mappers;

import com.qually.qually.dto.request.ClientRequestDTO;
import com.qually.qually.dto.response.ClientResponseDTO;
import com.qually.qually.models.Client;
import org.springframework.stereotype.Component;

/**
 * Maps between {@link Client} entities and their DTOs.
 * Extracted from the inline conversion that previously lived
 * in {@code ClientService.toDTO()}.
 */
@Component
public class ClientMapper {

    public ClientResponseDTO toDTO(Client client) {
        return ClientResponseDTO.builder()
                .clientId(client.getClientId())
                .clientName(client.getClientName())
                .build();
    }

    public Client toEntity(ClientRequestDTO dto) {
        return Client.builder()
                .clientName(dto.getClientName())
                .build();
    }
}