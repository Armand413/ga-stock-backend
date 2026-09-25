package com.ga.gestionstock.dto;

import com.ga.gestionstock.entity.*;

public record UtilisateurResponse(Long id, String identifiant, String nom, Role role, boolean actif, String email, String origine) {
    public static UtilisateurResponse of(Utilisateur u) {
        return new UtilisateurResponse(u.getId(), u.getIdentifiant(), u.getNom(), u.getRole(), u.isActif(), u.getEmail(), u.getOrigine());
    }
}
