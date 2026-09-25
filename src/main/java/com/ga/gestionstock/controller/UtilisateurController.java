package com.ga.gestionstock.controller;

import com.ga.gestionstock.dto.*;
import com.ga.gestionstock.service.UtilisateurService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/utilisateurs")
public class UtilisateurController {
    private final UtilisateurService utilisateurs;
    public UtilisateurController(UtilisateurService utilisateurs) { this.utilisateurs = utilisateurs; }
    @GetMapping
    public PageResponse<UtilisateurResponse> lister(@RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return utilisateurs.lister(page, size);
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UtilisateurResponse creer(@Valid @RequestBody UtilisateurRequest input) { return utilisateurs.creer(input); }
    @PutMapping("/{id}")
    public UtilisateurResponse modifier(@PathVariable Long id, @Valid @RequestBody UtilisateurUpdateRequest input) {
        return utilisateurs.modifier(id, input);
    }

    @PutMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reinitialiser(@PathVariable Long id, @Valid @RequestBody ReinitialisationMotDePasseRequest input) {
        utilisateurs.reinitialiserMotDePasse(id, input);
    }
}
