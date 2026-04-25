package com.qually.qually.controllers;

import com.qually.qually.models.Role;
import com.qually.qually.repositories.RoleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exposes the predefined roles list for dropdowns across the app.
 * Roles are managed at the database level — QA users can assign
 * existing roles but cannot create new ones via the UI.
 */
@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleRepository roleRepository;

    public RoleController(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    /**
     * Returns all roles ordered by department then hierarchy level.
     * Used to populate the role selector in the user management form.
     */
    @GetMapping
    public ResponseEntity<List<Role>> getAllRoles() {
        List<Role> roles = roleRepository.findAll().stream()
                .sorted((a, b) -> {
                    int deptCmp = a.getDepartment().name()
                            .compareTo(b.getDepartment().name());
                    if (deptCmp != 0) return deptCmp;
                    return Integer.compare(a.getHierarchyLevel(), b.getHierarchyLevel());
                })
                .toList();
        return ResponseEntity.ok(roles);
    }
}
