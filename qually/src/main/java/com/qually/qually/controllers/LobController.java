package com.qually.qually.controllers;

import com.qually.qually.dto.request.LobRequestDTO;
import com.qually.qually.dto.response.LobResponseDTO;
import com.qually.qually.services.LobService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class LobController {

    private final LobService teamService;

    public LobController(LobService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    public ResponseEntity<LobResponseDTO> createLob(@Valid @RequestBody LobRequestDTO dto) {
        return ResponseEntity.ok(teamService.createLob(dto));
    }

    @GetMapping
    public ResponseEntity<List<LobResponseDTO>> getAllTeams(
            @RequestParam(required = false) Integer clientId,
            @RequestParam(required = false) String teamLeaderEmail) {
        return ResponseEntity.ok(teamService.getAllLobs(clientId, teamLeaderEmail));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LobResponseDTO> getLobById(@PathVariable Integer id) {
        return ResponseEntity.ok(teamService.getLobById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LobResponseDTO> updateLob(@PathVariable Integer id,
                                                    @Valid @RequestBody LobRequestDTO dto) {
        return ResponseEntity.ok(teamService.updateLob(id, dto));
    }
}