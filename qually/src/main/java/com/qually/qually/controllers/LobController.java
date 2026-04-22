package com.qually.qually.controllers;

import com.qually.qually.dto.request.LobRequestDTO;
import com.qually.qually.dto.response.LobResponseDTO;
import com.qually.qually.services.LobService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for {@link com.qually.qually.models.Lob} resources.
 *
 * <p>{@code teamLeaderEmail} query param removed from {@code GET /api/teams} —
 * the {@code lobs} table has no team leader column.</p>
 */
@RestController
@RequestMapping("/api/teams")
public class LobController {

    private final LobService lobService;

    public LobController(LobService lobService) {
        this.lobService = lobService;
    }

    @PostMapping
    public ResponseEntity<LobResponseDTO> createLob(@Valid @RequestBody LobRequestDTO dto) {
        return ResponseEntity.ok(lobService.createLob(dto));
    }

    @GetMapping
    public ResponseEntity<List<LobResponseDTO>> getAllLobs(
            @RequestParam(required = false) Integer clientId) {
        return ResponseEntity.ok(lobService.getAllLobs(clientId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LobResponseDTO> getLobById(@PathVariable Integer id) {
        return ResponseEntity.ok(lobService.getLobById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LobResponseDTO> updateLob(@PathVariable Integer id,
                                                    @Valid @RequestBody LobRequestDTO dto) {
        return ResponseEntity.ok(lobService.updateLob(id, dto));
    }
}
