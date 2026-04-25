package com.qually.qually.services;

import com.qually.qually.dto.request.UserRequestDTO;
import com.qually.qually.dto.request.UserUpdateRequestDTO;
import com.qually.qually.dto.response.UserResponseDTO;
import com.qually.qually.models.*;
import com.qually.qually.repositories.*;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ClientRepository clientRepository;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       ClientRepository clientRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.clientRepository = clientRepository;
    }

    @Transactional
    public UserResponseDTO createUser(UserRequestDTO dto) {
        if (userRepository.findByUserEmail(dto.getUserEmail()).isPresent()) {
            log.warn("Duplicate email rejected: {}", dto.getUserEmail());
            throw new IllegalArgumentException(
                    "A user with email '%s' already exists".formatted(dto.getUserEmail()));
        }
        Role role = resolveRole(dto.getRoleId());
        User manager = resolveManager(dto.getManagerId());
        List<Client> clients = resolveClients(dto.getClientIds());

        User user = User.builder()
                .userEmail(dto.getUserEmail())
                .fullName(dto.getFullName())
                .role(role)
                .manager(manager)
                .isActive(true)
                .build();
        user.setClients(clients);
        User saved = userRepository.save(user);

        log.info("User {} created — email '{}', role '{}'",
                saved.getUserId(), saved.getUserEmail(),
                role != null ? role.getRoleName() : "none");

        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers(String roleName, Boolean activeOnly) {
        List<User> users;
        if (roleName != null && !roleName.isBlank()) {
            users = Boolean.TRUE.equals(activeOnly)
                    ? userRepository.findByRole_RoleNameAndIsActiveTrue(roleName)
                    : userRepository.findByRole_RoleName(roleName);
        } else {
            users = Boolean.TRUE.equals(activeOnly)
                    ? userRepository.findByIsActiveTrue()
                    : userRepository.findAll();
        }
        return users.stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Integer id) {
        return userRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User with ID %d not found".formatted(id)));
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAuditableUsers(Integer clientId) {
        return userRepository.findAuditableUsersByClient(clientId)
                .stream().map(this::toDTO).toList();
    }

    @Transactional
    public UserResponseDTO updateUser(Integer id, UserUpdateRequestDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User with ID %d not found".formatted(id)));
        user.setFullName(dto.getFullName());
        if (dto.getRoleId() != null)    user.setRole(resolveRole(dto.getRoleId()));
        if (dto.getManagerId() != null) user.setManager(resolveManager(dto.getManagerId()));
        if (dto.getClientIds() != null) user.setClients(resolveClients(dto.getClientIds()));
        log.info("User {} updated", id);
        return toDTO(userRepository.save(user));
    }

    @Transactional
    public UserResponseDTO deactivateUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User with ID %d not found".formatted(id)));
        user.setIsActive(false);
        log.info("User {} ({}) deactivated", id, user.getUserEmail());
        return toDTO(userRepository.save(user));
    }

    @Transactional
    public UserResponseDTO reactivateUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User with ID %d not found".formatted(id)));
        user.setIsActive(true);
        log.info("User {} ({}) reactivated", id, user.getUserEmail());
        return toDTO(userRepository.save(user));
    }

    // ── Helpers ───────────────────────────────────────────────

    private Role resolveRole(Integer roleId) {
        if (roleId == null) return null;
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Role with ID %d not found".formatted(roleId)));
    }

    private User resolveManager(Integer managerId) {
        if (managerId == null) return null;
        return userRepository.findById(managerId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Manager with ID %d not found".formatted(managerId)));
    }

    private List<Client> resolveClients(List<Integer> clientIds) {
        if (clientIds == null || clientIds.isEmpty()) return List.of();
        return clientIds.stream()
                .map(id -> clientRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Client with ID %d not found".formatted(id))))
                .toList();
    }

    private UserResponseDTO toDTO(User user) {
        Role role = user.getRole();
        return UserResponseDTO.builder()
                .userId(user.getUserId())
                .userEmail(user.getUserEmail())
                .fullName(user.getFullName())
                .roleId(role != null ? role.getRoleId() : null)
                .roleName(role != null ? role.getRoleName() : null)
                .department(role != null ? role.getDepartment() : null)
                .hierarchyLevel(role != null ? role.getHierarchyLevel() : null)
                .canBeAudited(role != null ? role.getCanBeAudited() : false)
                .canRaiseDispute(role != null ? role.getCanRaiseDispute() : false)
                .managerId(user.getManager() != null ? user.getManager().getUserId() : null)
                .managerName(user.getManager() != null ? user.getManager().getFullName() : null)
                .clientIds(user.getClients().stream().map(Client::getClientId).toList())
                .isActive(user.getIsActive())
                .build();
    }
}
