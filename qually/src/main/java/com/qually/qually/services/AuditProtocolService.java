package com.qually.qually.services;

import com.qually.qually.dto.request.SubattributeRequestDTO;
import com.qually.qually.dto.request.AuditProtocolRequestDTO;
import com.qually.qually.dto.request.AuditQuestionRequestDTO;
import com.qually.qually.dto.request.SubattributeOptionRequestDTO;
import com.qually.qually.dto.response.SubattributeResponseDTO;
import com.qually.qually.dto.response.AuditProtocolResponseDTO;
import com.qually.qually.dto.response.AuditQuestionResponseDTO;
import com.qually.qually.mappers.AuditProtocolMapper;
import com.qually.qually.models.AuditProtocol;
import com.qually.qually.models.Client;
import com.qually.qually.repositories.AuditProtocolRepository;
import com.qually.qually.repositories.ClientRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.qually.qually.models.AuditQuestion;
import com.qually.qually.models.Subattribute;
import com.qually.qually.models.SubattributeOption;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuditProtocolService {

    private final AuditProtocolRepository auditProtocolRepository;
    private final ClientRepository clientRepository;
    private final AuditProtocolMapper auditProtocolMapper;

    public AuditProtocolService(AuditProtocolRepository auditProtocolRepository,
                                ClientRepository clientRepository,
                                AuditProtocolMapper auditProtocolMapper) {
        this.auditProtocolRepository = auditProtocolRepository;
        this.clientRepository = clientRepository;
        this.auditProtocolMapper = auditProtocolMapper;
    }

    @Transactional
    public AuditProtocolResponseDTO createProtocol(AuditProtocolRequestDTO dto) {
        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new EntityNotFoundException("Client with ID %d not found".formatted(dto.getClientId())));

        auditProtocolRepository.findByProtocolNameAndClient_ClientId(dto.getProtocolName(), dto.getClientId())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("A protocol with the name '%s' already exists for this client".formatted(dto.getProtocolName()));
                });

        AuditProtocol protocol = AuditProtocol.builder()
                .protocolName(dto.getProtocolName())
                .protocolVersion(dto.getProtocolVersion())
                .isFinalized(false)
                .client(client)
                .build();

        return auditProtocolMapper.toDTO(auditProtocolRepository.save(protocol));
    }

    @Transactional(readOnly = true)
    public List<AuditProtocolResponseDTO> getAllProtocols(Integer clientId) {
        List<AuditProtocol> protocols = (clientId != null)
                ? auditProtocolRepository.findByClient_ClientId(clientId)
                : auditProtocolRepository.findAll();
        return protocols.stream().map(auditProtocolMapper::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public AuditProtocolResponseDTO getProtocolById(Integer id) {
        return auditProtocolRepository.findById(id)
                .map(auditProtocolMapper::toDTO)
                .orElseThrow(() -> new EntityNotFoundException("Protocol with ID %d not found".formatted(id)));
    }

    @Transactional
    public AuditProtocolResponseDTO updateProtocol(Integer id, AuditProtocolRequestDTO dto) {
        AuditProtocol protocol = auditProtocolRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Protocol with ID %d not found".formatted(id)));
        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new EntityNotFoundException("Client with ID %d not found".formatted(dto.getClientId())));

        auditProtocolRepository.findByProtocolNameAndClient_ClientId(dto.getProtocolName(), dto.getClientId())
                .filter(existing -> !existing.getProtocolId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("A protocol with the name '%s' already exists for this client".formatted(dto.getProtocolName()));
                });

        protocol.setProtocolName(dto.getProtocolName());
        protocol.setProtocolVersion(dto.getProtocolVersion());
        protocol.setClient(client);

        return auditProtocolMapper.toDTO(auditProtocolRepository.save(protocol));
    }

    @Transactional
    public AuditProtocolResponseDTO finalizeProtocol(Integer id) {
        AuditProtocol protocol = auditProtocolRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Protocol with ID %d not found".formatted(id)));

        protocol.setIsFinalized(true);
        return auditProtocolMapper.toDTO(auditProtocolRepository.save(protocol));
    }

    @Transactional
    public AuditProtocolResponseDTO createFullProtocol(AuditProtocolRequestDTO dto) {
        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new EntityNotFoundException("Client not found"));

        AuditProtocol protocol = AuditProtocol.builder()
                .protocolName(dto.getProtocolName())
                .protocolVersion(dto.getProtocolVersion())
                .isFinalized(false)
                .client(client)
                .auditQuestions(new ArrayList<>())
                .build();

        if (dto.getAuditQuestions() != null) {
            for (AuditQuestionRequestDTO qDto : dto.getAuditQuestions()) {
                AuditQuestion question = AuditQuestion.builder()
                        .questionText(qDto.getQuestionText())
                        .category(qDto.getCategory())
                        .auditProtocol(protocol)
                        .subattributes(new ArrayList<>())
                        .build();

                if (qDto.getSubattributes() != null) {
                    for (SubattributeRequestDTO sattrDto : qDto.getSubattributes()) {
                        Subattribute subattribute = Subattribute.builder()
                                .subattributeText(sattrDto.getAttributeText())
                                .auditQuestion(question)
                                .subattributeOptions(new ArrayList<>())
                                .build();

                        if (sattrDto.getSubattributeOptions() != null) {
                            for (SubattributeOptionRequestDTO oDto : sattrDto.getSubattributeOptions()) {
                                SubattributeOption subattributeOption = SubattributeOption.builder()
                                        .optionLabel(oDto.getOptionLabel())
                                        .subattribute(subattribute)
                                        .build();
                                subattribute.getSubattributeOptions().add(subattributeOption);
                            }
                        }
                        question.getSubattributes().add(subattribute);
                    }
                }
                protocol.getAuditQuestions().add(question);
            }
        }

        return auditProtocolMapper.toDTO(auditProtocolRepository.save(protocol));
    }

}