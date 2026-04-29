package com.qually.qually.controllers;

import com.qually.qually.dto.response.RoleResponseDTO;
import com.qually.qually.services.RoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for {@link com.qually.qually.models.Role}.
 *
 * <p>Previously called {@code RoleRepository} directly — now goes through
 * {@link RoleService} following the standard controller → service → repository
 * architecture.</p>
 */
@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public ResponseEntity<List<RoleResponseDTO>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleResponseDTO> getRoleById(@PathVariable Integer id) {
        return ResponseEntity.ok(roleService.getRoleById(id));
    }
}