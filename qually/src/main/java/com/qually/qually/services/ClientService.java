package com.qually.qually.services;

import com.qually.qually.dto.request.ClientRequestDTO;
import com.qually.qually.dto.response.ClientResponseDTO;
import com.qually.qually.models.Client;
import com.qually.qually.repositories.ClientRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional
    public ClientResponseDTO createClient(ClientRequestDTO dto) {
        if (clientRepository.findByClientName(dto.getClientName()).isPresent()) {
            throw new IllegalArgumentException("A client with the name %s already exists".formatted(dto.getClientName()));
        }
        Client client = Client.builder()
                .clientName(dto.getClientName())
                .build();
        return toDTO(clientRepository.save(client));
    }

    @Transactional(readOnly = true)
    public List<ClientResponseDTO> getAllClients() {
        return clientRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClientResponseDTO getClientById(Integer id) {
        return clientRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new EntityNotFoundException("Client with ID %d not found".formatted(id)));
    }

    @Transactional
    public ClientResponseDTO updateClient(Integer id, ClientRequestDTO dto) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Client with ID %d not found".formatted(id)));

        clientRepository.findByClientName(dto.getClientName())
                .filter(existing -> !existing.getClientId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("A client with the name %s already exists".formatted(dto.getClientName()));
                });

        client.setClientName(dto.getClientName());
        return toDTO(clientRepository.save(client));
    }

    private ClientResponseDTO toDTO(Client client) {
        return ClientResponseDTO.builder()
                .clientId(client.getClientId())
                .clientName(client.getClientName())
                .build();
    }
}