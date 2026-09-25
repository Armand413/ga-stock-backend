package com.ga.gestionstock.dto;

import java.time.Instant;

public record ConnexionResponse(String accessToken, String tokenType, Instant expiration,
        UtilisateurResponse utilisateur) {
}
