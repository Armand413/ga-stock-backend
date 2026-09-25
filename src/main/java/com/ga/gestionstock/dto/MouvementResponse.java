package com.ga.gestionstock.dto;

import com.ga.gestionstock.entity.*;
import java.time.Instant;

public record MouvementResponse(Long id, Long articleId, String referenceArticle, String nomArticle,
        TypeMouvement type, long quantite, long stockApres, Long utilisateurId,
        String responsable, String beneficiaire, String motif, Instant date) {
    public static MouvementResponse of(Mouvement m) {
        return new MouvementResponse(m.getId(), m.getArticle().getId(), m.getArticle().getReference(),
                m.getArticle().getNom(), m.getType(), m.getQuantite(), m.getStockApres(),
                m.getUtilisateur() == null ? null : m.getUtilisateur().getId(), m.getResponsable(),
                m.getBeneficiaire(), m.getMotif(), m.getDate());
    }
}
