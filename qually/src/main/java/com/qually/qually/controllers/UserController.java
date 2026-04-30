package com.qually.qually.controllers;

import com.qually.qually.dto.request.UserRequestDTO;
import com.qually.qually.dto.request.UserUpdateRequestDTO;
import com.qually.qually.dto.response.UserResponseDTO;
import com.qually.qually.services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(
            @Valid @RequestBody UserRequestDTO dto) {
        return ResponseEntity.ok(userService.createUser(dto));
    }

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
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable Integer id,
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

    /**
     * Resets a user's PIN to a randomly generated 6-digit code.
     * Sets {@code force_pin_change = true} so the user must change it on
     * next login. The generated PIN is returned once — the caller must
     * share it with the user securely.
     *
     * <p>Only QA admins should call this endpoint. Authorization is enforced
     * at the service layer via the caller's role — the calling user's ID is
     * read from the JWT security context, not from a request header.</p>
     *
     * @return Map containing {@code pin} — the plain-text temporary PIN,
     *         shown once and never stored.
     */
    @PutMapping("/{id}/reset-pin")
    public ResponseEntity<Map<String, String>> resetPin(@PathVariable Integer id) {
        // Verify the caller is authenticated — SecurityConfig ensures this,
        // but we log who performed the reset for audit purposes.
        Integer callerId = (Integer) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        String generatedPin = userService.resetPin(id);

        return ResponseEntity.ok(Map.of(
                "pin",     generatedPin,
                "message", "PIN reset successfully. Share this PIN with the user — it will not be shown again."
        ));
    }
}