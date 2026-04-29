package com.qually.qually.services;

import com.qually.qually.dto.request.LobRequestDTO;
import com.qually.qually.dto.response.LobResponseDTO;
import com.qually.qually.mappers.LobMapper;
import com.qually.qually.models.Client;
import com.qually.qually.models.Lob;
import com.qually.qually.repositories.ClientRepository;
import com.qually.qually.repositories.LobRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LobService {

    private static final Logger log = LoggerFactory.getLogger(LobService.class);

    private final LobRepository lobRepository;
    private final ClientRepository clientRepository;
    private final LobMapper lobMapper;

    public LobService(LobRepository lobRepository,
                      ClientRepository clientRepository,
                      LobMapper lobMapper) {
        this.lobRepository    = lobRepository;
        this.clientRepository = clientRepository;
        this.lobMapper        = lobMapper;
    }

    @Transactional
    public LobResponseDTO createLob(LobRequestDTO dto) {
        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Client with ID %d not found".formatted(dto.getClientId())));

        lobRepository.findByLobNameAndClient_ClientId(dto.getLobName(), dto.getClientId())
                .ifPresent(existing -> {
                    log.warn("Duplicate LOB name '{}' rejected for client {}",
                            dto.getLobName(), dto.getClientId());
                    throw new IllegalArgumentException(
                            "A LOB named '%s' already exists for this client"
                                    .formatted(dto.getLobName()));
                });

        Lob saved = lobRepository.save(lobMapper.toEntity(dto, client));
        log.info("LOB {} '{}' created for client {}", saved.getLobId(), saved.getLobName(), client.getClientId());
        return lobMapper.toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<LobResponseDTO> getAllLobs(Integer clientId) {
        List<Lob> lobs = (clientId != null)
                ? lobRepository.findByClient_ClientId(clientId)
                : lobRepository.findAll();
        return lobs.stream().map(lobMapper::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public LobResponseDTO getLobById(Integer id) {
        return lobRepository.findById(id)
                .map(lobMapper::toDTO)
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
                            "A LOB named '%s' already exists for this client"
                                    .formatted(dto.getLobName()));
                });

        lob.setLobName(dto.getLobName());
        lob.setClient(client);
        Lob saved = lobRepository.save(lob);
        log.info("LOB {} updated — name '{}', client {}", id, dto.getLobName(), dto.getClientId());
        return lobMapper.toDTO(saved);
    }

    @Transactional
    public void deleteLob(Integer id) {
        if (!lobRepository.existsById(id)) {
            throw new EntityNotFoundException("LOB with ID %d not found".formatted(id));
        }
        lobRepository.deleteById(id);
        log.info("LOB {} deleted", id);
    }
}