package com.qually.qually.services;

import com.qually.qually.dto.request.UserRequestDTO;
import com.qually.qually.dto.request.UserUpdateRequestDTO;
import com.qually.qually.dto.response.UserResponseDTO;
import com.qually.qually.models.User;
import com.qually.qually.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponseDTO createUser(UserRequestDTO dto) {
        if (userRepository.existsById(dto.getUserEmail())) {
            throw new IllegalArgumentException("A user with the email '%s' already exists".formatted(dto.getUserEmail()));
        }
        User user = User.builder()
                .userEmail(dto.getUserEmail())
                .fullName(dto.getFullName())
                .role(dto.getRole())
                .build();
        return toDTO(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers(String role) {
        List<User> users = (role != null && !role.isBlank())
                ? userRepository.findByRole(role)
                : userRepository.findAll();
        return users.stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getUserByEmail(String email) {
        return userRepository.findById(email)
                .map(this::toDTO)
                .orElseThrow(() -> new EntityNotFoundException("User with email '%s' not found".formatted(email)));
    }

    @Transactional
    public UserResponseDTO updateUser(String email, UserUpdateRequestDTO dto) {
        User user = userRepository.findById(email)
                .orElseThrow(() -> new EntityNotFoundException("User with email '%s' not found".formatted(email)));
        user.setFullName(dto.getFullName());
        user.setRole(dto.getRole());
        return toDTO(userRepository.save(user));
    }

    private UserResponseDTO toDTO(User user) {
        return UserResponseDTO.builder()
                .userEmail(user.getUserEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }
}