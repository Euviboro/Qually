package com.qually.qually.mappers;

import com.qually.qually.dto.request.UserRequestDTO;
import com.qually.qually.dto.response.UserResponseDTO;
import com.qually.qually.models.Client;
import com.qually.qually.models.Role;
import com.qually.qually.models.User;
import com.qually.qually.repositories.ClientRepository;
import com.qually.qually.repositories.RoleRepository;
import com.qually.qually.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserMapper {

    public UserResponseDTO toDTO(User user) {
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

    public User toEntity(UserRequestDTO userRequestDTO, Role role, User manager, List<Client> clients) {

        User user = User.builder()
                .userEmail(userRequestDTO.getUserEmail())
                .fullName(userRequestDTO.getFullName())
                .role(role)
                .manager(manager)
                .isActive(true)
                .build();
        user.setClients(clients);

        return user;
    }

}
