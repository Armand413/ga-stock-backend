package com.ga.gestionstock.dto;

import com.ga.gestionstock.entity.*;
import java.time.Instant;

public record DemandeResponse(Long id, Long demandeurId, String demandeur, Long articleId, String article,
                              long quantite, String motif, StatutDemande statut, Instant creeLe,
                              Instant traiteeLe, String traiteePar, String reponse, Long mouvementId) {
    public static DemandeResponse of(Demande d) {
        return new DemandeResponse(d.getId(), d.getDemandeur().getId(), d.getDemandeur().getNom(),
                d.getArticle().getId(), d.getArticle().getNom(), d.getQuantite(), d.getMotif(), d.getStatut(),
                d.getCreeLe(), d.getTraiteeLe(), d.getTraiteePar() == null ? null : d.getTraiteePar().getNom(),
                d.getReponse(), d.getMouvement() == null ? null : d.getMouvement().getId());
    }
}
