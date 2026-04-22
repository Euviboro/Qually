package com.qually.qually.services;

import com.qually.qually.dto.request.UserRequestDTO;
import com.qually.qually.dto.request.UserUpdateRequestDTO;
import com.qually.qually.dto.response.UserResponseDTO;
import com.qually.qually.models.Role;
import com.qually.qually.models.User;
import com.qually.qually.repositories.RoleRepository;
import com.qually.qually.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for CRUD operations on {@link User} entities.
 *
 * <p>Key schema-alignment changes from the previous version:</p>
 * <ul>
 *   <li>All lookups use {@code user_id} (Integer) instead of {@code user_email}
 *       as the primary key.</li>
 *   <li>{@code role} is now a {@link Role} entity FK, resolved from
 *       {@link RoleRepository} by {@code roleId}.</li>
 *   <li>{@code manager} is a self-referencing {@link User} FK, resolved by
 *       {@code managerId}.</li>
 * </ul>
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    /**
     * Creates a new user.
     *
     * @param dto User creation payload.
     * @return The persisted user.
     * @throws IllegalArgumentException if a user with the same email already exists.
     * @throws EntityNotFoundException  if the provided {@code roleId} or {@code managerId}
     *                                  do not match existing records.
     */
    @Transactional
    public UserResponseDTO createUser(UserRequestDTO dto) {
        if (userRepository.findByUserEmail(dto.getUserEmail()).isPresent()) {
            throw new IllegalArgumentException(
                    "A user with the email '%s' already exists".formatted(dto.getUserEmail()));
        }

        Role role = resolveRole(dto.getRoleId());
        User manager = resolveManager(dto.getManagerId());

        User user = User.builder()
                .userEmail(dto.getUserEmail())
                .fullName(dto.getFullName())
                .role(role)
                .manager(manager)
                .build();

        return toDTO(userRepository.save(user));
    }

    /**
     * Returns all users, optionally filtered by role name.
     *
     * @param roleName When non-null, returns only users with this role name.
     * @return Matching users.
     */
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers(String roleName) {
        List<User> users = (roleName != null && !roleName.isBlank())
                ? userRepository.findByRole_RoleName(roleName)
                : userRepository.findAll();
        return users.stream().map(this::toDTO).toList();
    }

    /**
     * Returns a single user by their integer ID.
     *
     * @param id User ID.
     * @return The user DTO.
     * @throws EntityNotFoundException if no user with this ID exists.
     */
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Integer id) {
        return userRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User with ID %d not found".formatted(id)));
    }

    /**
     * Updates a user's mutable fields.
     *
     * @param id  User ID.
     * @param dto Fields to update.
     * @return The updated user DTO.
     * @throws EntityNotFoundException if the user, role, or manager do not exist.
     */
    @Transactional
    public UserResponseDTO updateUser(Integer id, UserUpdateRequestDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User with ID %d not found".formatted(id)));

        user.setFullName(dto.getFullName());
        if (dto.getRoleId() != null)    user.setRole(resolveRole(dto.getRoleId()));
        if (dto.getManagerId() != null) user.setManager(resolveManager(dto.getManagerId()));

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

    private UserResponseDTO toDTO(User user) {
        return UserResponseDTO.builder()
                .userId(user.getUserId())
                .userEmail(user.getUserEmail())
                .fullName(user.getFullName())
                .roleId(user.getRole() != null ? user.getRole().getRoleId() : null)
                .roleName(user.getRole() != null ? user.getRole().getRoleName() : null)
                .department(user.getRole() != null ? user.getRole().getDepartment() : null)
                .managerId(user.getManager() != null ? user.getManager().getUserId() : null)
                .managerName(user.getManager() != null ? user.getManager().getFullName() : null)
                .build();
    }
}
