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

    /**
     * @param roleName              Optional role name filter.
     * @param activeOnly            When true, returns only active users.
     * @param clientId              Required when {@code auditable=true} or
     *                              {@code calibrationEligible=true}.
     * @param auditable             When true, returns auditable users for clientId.
     * @param calibrationEligible   When true, returns calibration-eligible users
     *                              (all QA + OPERATIONS TL/Supervisor for clientId).
     */
    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers(
            @RequestParam(required = false) String  roleName,
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly,
            @RequestParam(required = false) Integer clientId,
            @RequestParam(required = false, defaultValue = "false") Boolean auditable,
            @RequestParam(required = false, defaultValue = "false") Boolean calibrationEligible) {

        if (Boolean.TRUE.equals(calibrationEligible) && clientId != null) {
            return ResponseEntity.ok(userService.getCalibrationEligibleUsers(clientId));
        }
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
     * next login. The plain PIN is returned once — the caller must share
     * it with the user securely.
     */
    @PutMapping("/{id}/reset-pin")
    public ResponseEntity<Map<String, String>> resetPin(@PathVariable Integer id) {
        String generatedPin = userService.resetPin(id);
        return ResponseEntity.ok(Map.of(
                "pin",     generatedPin,
                "message", "PIN reset successfully. Share this PIN with the user — it will not be shown again."));
    }
}