package com.qually.qually.services;

import com.qually.qually.dto.request.AuditedProcessRequestDTO;
import com.qually.qually.dto.response.AuditedProcessResponseDTO;
import com.qually.qually.models.AuditedProcess;
import com.qually.qually.repositories.AuditedProcessRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditedProcessService {

    private final AuditedProcessRepository auditedProcessRepository;

    public AuditedProcessService(AuditedProcessRepository auditedProcessRepository) {
        this.auditedProcessRepository = auditedProcessRepository;
    }

    @Transactional
    public AuditedProcessResponseDTO createProcess(AuditedProcessRequestDTO dto) {
        auditedProcessRepository.findByAuditedProcessName(dto.getAuditedProcessName())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("A process with the name '%s' already exists".formatted(dto.getAuditedProcessName()));
                });

        AuditedProcess process = AuditedProcess.builder()
                .auditedProcessName(dto.getAuditedProcessName())
                .build();

        return toDTO(auditedProcessRepository.save(process));
    }

    @Transactional(readOnly = true)
    public List<AuditedProcessResponseDTO> getAllProcesses() {
        return auditedProcessRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public AuditedProcessResponseDTO getProcessById(Integer id) {
        return auditedProcessRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new EntityNotFoundException("Audited process with ID %d not found".formatted(id)));
    }

    @Transactional
    public AuditedProcessResponseDTO updateProcess(Integer id, AuditedProcessRequestDTO dto) {
        AuditedProcess process = auditedProcessRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Audited process with ID %d not found".formatted(id)));

        auditedProcessRepository.findByAuditedProcessName(dto.getAuditedProcessName())
                .filter(existing -> !existing.getAuditedProcessId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("A process with the name '%s' already exists".formatted(dto.getAuditedProcessName()));
                });

        process.setAuditedProcessName(dto.getAuditedProcessName());
        return toDTO(auditedProcessRepository.save(process));
    }

    private AuditedProcessResponseDTO toDTO(AuditedProcess process) {
        return AuditedProcessResponseDTO.builder()
                .auditedProcessId(process.getAuditedProcessId())
                .auditedProcessName(process.getAuditedProcessName())
                .build();
    }
}