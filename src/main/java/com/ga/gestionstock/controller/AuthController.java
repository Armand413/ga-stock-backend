package com.ga.gestionstock.controller;

import com.ga.gestionstock.dto.*;
import com.ga.gestionstock.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth = auth; }
    @PostMapping("/login")
    @Operation(summary = "Se connecter avec ses identifiants", security = {})
    public ConnexionResponse connexion(@Valid @RequestBody ConnexionRequest input) { return auth.connexion(input); }
    @GetMapping("/me")
    public UtilisateurResponse moi(@AuthenticationPrincipal UtilisateurConnecte utilisateur) { return auth.moi(utilisateur.id()); }
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deconnexion(@RequestHeader("Authorization") String header) { auth.deconnexion(header.substring(7)); }
    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void motDePasse(@AuthenticationPrincipal UtilisateurConnecte utilisateur, @Valid @RequestBody MotDePasseRequest input) {
        auth.changerMotDePasse(utilisateur.id(), input);
    }
}

