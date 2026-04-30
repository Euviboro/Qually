package com.qually.qually.services;

import com.qually.qually.dto.request.UserRequestDTO;
import com.qually.qually.dto.request.UserUpdateRequestDTO;
import com.qually.qually.dto.response.UserResponseDTO;
import com.qually.qually.mappers.UserMapper;
import com.qually.qually.models.*;
import com.qually.qually.repositories.*;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository  userRepository;
    private final RoleRepository  roleRepository;
    private final ClientRepository clientRepository;
    private final UserMapper      userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       ClientRepository clientRepository,
                       UserMapper userMapper,
                       PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.roleRepository  = roleRepository;
        this.clientRepository = clientRepository;
        this.userMapper      = userMapper;
        this.passwordEncoder = passwordEncoder;
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

        User saved = userRepository.save(userMapper.toEntity(dto, role, manager, clients));

        log.info("User {} created — email '{}', role '{}'",
                saved.getUserId(), saved.getUserEmail(),
                role != null ? role.getRoleName() : "none");

        return userMapper.toDTO(saved);
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
        return users.stream().map(userMapper::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Integer id) {
        return userRepository.findById(id)
                .map(userMapper::toDTO)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User with ID %d not found".formatted(id)));
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAuditableUsers(Integer clientId) {
        return userRepository.findAuditableUsersByClient(clientId)
                .stream().map(userMapper::toDTO).toList();
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
        return userMapper.toDTO(userRepository.save(user));
    }

    @Transactional
    public UserResponseDTO deactivateUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User with ID %d not found".formatted(id)));
        user.setIsActive(false);
        log.info("User {} ({}) deactivated", id, user.getUserEmail());
        return userMapper.toDTO(userRepository.save(user));
    }

    @Transactional
    public UserResponseDTO reactivateUser(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User with ID %d not found".formatted(id)));
        user.setIsActive(true);
        log.info("User {} ({}) reactivated", id, user.getUserEmail());
        return userMapper.toDTO(userRepository.save(user));
    }

    /**
     * Resets a user's PIN to a randomly generated 6-digit code and forces
     * them to change it on next login.
     *
     * <p>The plain PIN is returned exactly once — it is never stored.
     * The caller (QA admin) must share it with the user securely. The
     * Settings page displays it in a one-time modal with a copy button.</p>
     *
     * @param id The user whose PIN is being reset.
     * @return The plain-text temporary PIN — shown once, then discarded.
     */
    @Transactional
    public String resetPin(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User with ID %d not found".formatted(id)));

        String plainPin = generatePin();
        user.setPinHash(passwordEncoder.encode(plainPin));
        user.setForcePinChange(true);
        userRepository.save(user);

        log.info("PIN reset for user {} ({}) by admin", id, user.getUserEmail());
        return plainPin;
    }

    // ── Helpers ───────────────────────────────────────────────

    /**
     * Generates a cryptographically random 6-digit PIN.
     * Uses {@link SecureRandom} — not {@link java.util.Random} which is
     * predictable and unsuitable for security-sensitive values.
     */
    private String generatePin() {
        SecureRandom random = new SecureRandom();
        int pin = 100_000 + random.nextInt(900_000); // 100000–999999
        return String.valueOf(pin);
    }

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
        if (clientIds == null || clientIds.isEmpty()) return new ArrayList<>();
        return clientIds.stream()
                .map(id -> clientRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Client with ID %d not found".formatted(id))))
                .collect(Collectors.toList());
    }
}