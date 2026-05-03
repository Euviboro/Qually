package com.qually.qually.services;

import com.qually.qually.dto.request.ClientRequestDTO;
import com.qually.qually.dto.response.ClientResponseDTO;
import com.qually.qually.mappers.ClientMapper;
import com.qually.qually.models.Client;
import com.qually.qually.repositories.ClientRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClientService {

    private static final Logger log = LoggerFactory.getLogger(ClientService.class);

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;

    public ClientService(ClientRepository clientRepository,
                         ClientMapper clientMapper) {
        this.clientRepository = clientRepository;
        this.clientMapper     = clientMapper;
    }

    @Transactional
    public ClientResponseDTO createClient(ClientRequestDTO dto) {
        if (clientRepository.existsByClientName(dto.getClientName())) {
            log.warn("Duplicate client name '{}' rejected", dto.getClientName());
            throw new IllegalArgumentException(
                    "A client named '%s' already exists".formatted(dto.getClientName()));
        }

        Client saved = clientRepository.save(clientMapper.toEntity(dto));
        log.info("Client {} '{}' created", saved.getClientId(), saved.getClientName());
        return clientMapper.toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ClientResponseDTO> getAllClients() {
        return clientRepository.findAll().stream().map(clientMapper::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public ClientResponseDTO getClientById(Integer id) {
        return clientRepository.findById(id)
                .map(clientMapper::toDTO)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Client with ID %d not found".formatted(id)));
    }

    @Transactional
    public ClientResponseDTO updateClient(Integer id, ClientRequestDTO dto) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Client with ID %d not found".formatted(id)));

        clientRepository.findByClientName(dto.getClientName())
                .filter(existing -> !existing.getClientId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "A client named '%s' already exists".formatted(dto.getClientName()));
                });

        client.setClientName(dto.getClientName());
        Client saved = clientRepository.save(client);
        log.info("Client {} updated — name '{}'", id, dto.getClientName());
        return clientMapper.toDTO(saved);
    }

    @Transactional
    public void deleteClient(Integer id) {
        if (!clientRepository.existsById(id)) {
            throw new EntityNotFoundException("Client with ID %d not found".formatted(id));
        }
        clientRepository.deleteById(id);
        log.info("Client {} deleted", id);
    }
}