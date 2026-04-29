package com.qually.qually.services;

import com.qually.qually.dto.response.RoleResponseDTO;
import com.qually.qually.models.Role;
import com.qually.qually.repositories.RoleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service layer for {@link Role} — currently read-only reference data.
 *
 * <p>Roles are seeded via SQL and managed by administrators directly in the
 * database. This service provides the API-facing read methods and the DTO
 * conversion that previously happened directly in {@code RoleController}.</p>
 *
 * <p>No logging is added here — read-only reference data has nothing
 * meaningful to trace at the service level.</p>
 */
@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleResponseDTO> getAllRoles() {
        return roleRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public RoleResponseDTO getRoleById(Integer id) {
        return roleRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Role with ID %d not found".formatted(id)));
    }

    // ── Helpers ───────────────────────────────────────────────

    private RoleResponseDTO toDTO(Role role) {
        return RoleResponseDTO.builder()
                .roleId(role.getRoleId())
                .roleName(role.getRoleName())
                .department(role.getDepartment())
                .hierarchyLevel(role.getHierarchyLevel())
                .canBeAudited(role.getCanBeAudited())
                .canRaiseDispute(role.getCanRaiseDispute())
                .build();
    }
}