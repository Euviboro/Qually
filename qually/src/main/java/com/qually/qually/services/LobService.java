package com.qually.qually.services;

import com.qually.qually.dto.request.LobRequestDTO;
import com.qually.qually.dto.response.LobResponseDTO;
import com.qually.qually.models.Client;
import com.qually.qually.models.Lob;
import com.qually.qually.repositories.ClientRepository;
import com.qually.qually.repositories.LobRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for CRUD operations on {@link Lob} entities.
 *
 * <p>{@code UserRepository} dependency and all team-leader logic have been
 * removed. The {@code lobs} table only has {@code lob_id}, {@code lob_name},
 * and {@code client_id}.</p>
 */
@Service
public class LobService {

    private final LobRepository lobRepository;
    private final ClientRepository clientRepository;

    public LobService(LobRepository lobRepository, ClientRepository clientRepository) {
        this.lobRepository = lobRepository;
        this.clientRepository = clientRepository;
    }

    @Transactional
    public LobResponseDTO createLob(LobRequestDTO dto) {
        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Client with ID %d not found".formatted(dto.getClientId())));

        lobRepository.findByLobNameAndClient_ClientId(dto.getLobName(), dto.getClientId())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "A LOB named '%s' already exists for this client".formatted(dto.getLobName()));
                });

        Lob lob = Lob.builder()
                .lobName(dto.getLobName())
                .client(client)
                .build();
        return toDTO(lobRepository.save(lob));
    }

    @Transactional(readOnly = true)
    public List<LobResponseDTO> getAllLobs(Integer clientId) {
        List<Lob> lobs;
        if (clientId != null) {
            Client client = clientRepository.findById(clientId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Client with ID %d not found".formatted(clientId)));
            lobs = lobRepository.findByClient(client);
        } else {
            lobs = lobRepository.findAll();
        }
        return lobs.stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public LobResponseDTO getLobById(Integer id) {
        return lobRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new EntityNotFoundException(
                        "LOB with ID %d not found".formatted(id)));
    }

    @Transactional
    public LobResponseDTO updateLob(Integer id, LobRequestDTO dto) {
        Lob lob = lobRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "LOB with ID %d not found".formatted(id)));
        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Client with ID %d not found".formatted(dto.getClientId())));

        lobRepository.findByLobNameAndClient_ClientId(dto.getLobName(), dto.getClientId())
                .filter(existing -> !existing.getLobId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "A LOB named '%s' already exists for this client".formatted(dto.getLobName()));
                });

        lob.setLobName(dto.getLobName());
        lob.setClient(client);
        return toDTO(lobRepository.save(lob));
    }

    private LobResponseDTO toDTO(Lob lob) {
        return LobResponseDTO.builder()
                .lobId(lob.getLobId())
                .lobName(lob.getLobName())
                .clientId(lob.getClient().getClientId())
                .clientName(lob.getClient().getClientName())
                .build();
    }
}
