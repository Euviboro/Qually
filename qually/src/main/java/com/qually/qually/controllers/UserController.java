package com.qually.qually.controllers;

import com.qually.qually.dto.request.UserRequestDTO;
import com.qually.qually.dto.request.UserUpdateRequestDTO;
import com.qually.qually.dto.response.UserResponseDTO;
import com.qually.qually.services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    /**
     * @param roleName   Optional role name filter.
     * @param activeOnly When true, returns only active users. Defaults to false.
     * @param clientId   When set alongside auditable=true, filters to that client.
     * @param auditable  When true, returns only users with auditable roles for the given client.
     */
    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers(
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly,
            @RequestParam(required = false) Integer clientId,
            @RequestParam(required = false, defaultValue = "false") Boolean auditable) {

        if (Boolean.TRUE.equals(auditable) && clientId != null) {
            return ResponseEntity.ok(userService.getAuditableUsers(clientId));
        }
        return ResponseEntity.ok(userService.getAllUsers(roleName, activeOnly));
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

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<UserResponseDTO> deactivateUser(@PathVariable Integer id) {
        return ResponseEntity.ok(userService.deactivateUser(id));
    }

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<UserResponseDTO> reactivateUser(@PathVariable Integer id) {
        return ResponseEntity.ok(userService.reactivateUser(id));
    }
}
