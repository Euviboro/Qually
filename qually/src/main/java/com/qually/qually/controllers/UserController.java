package com.qually.qually.controllers;

import com.qually.qually.dto.request.UserRequestDTO;
import com.qually.qually.dto.request.UserUpdateRequestDTO;
import com.qually.qually.dto.response.UserResponseDTO;
import com.qually.qually.services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for {@link com.qually.qually.models.User} resources.
 *
 * <p>Path variable changed from {@code /{email}} to {@code /{id}} because
 * the PK is now the auto-increment {@code user_id} (Integer), not the email.
 * The query param for role filter is renamed to {@code roleName} to match
 * the new {@code Role} entity's {@code roleName} field.</p>
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserRequestDTO dto) {
        return ResponseEntity.ok(userService.createUser(dto));
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers(
            @RequestParam(required = false) String roleName) {
        return ResponseEntity.ok(userService.getAllUsers(roleName));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Integer id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable Integer id,
                                                      @Valid @RequestBody UserUpdateRequestDTO dto) {
        return ResponseEntity.ok(userService.updateUser(id, dto));
    }
}
