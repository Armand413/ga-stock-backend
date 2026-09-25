package com.ga.gestionstock.dto;

import com.ga.gestionstock.entity.Alerte;
import java.time.Instant;
public record AlerteResponse(Long id, Long articleId, String nomArticle, long quantite, long seuilAlerte,
                             Instant creeLe, Instant resolueLe, Instant acquitteeLe, String acquitteePar) {
    public static AlerteResponse of(Alerte a) {
        return new AlerteResponse(a.getId(), a.getArticle().getId(), a.getArticle().getNom(),
                a.getArticle().getQuantite(), a.getArticle().getSeuilAlerte(), a.getCreeLe(),
                a.getResolueLe(), a.getAcquitteeLe(),
                a.getAcquitteePar() == null ? null : a.getAcquitteePar().getNom());
    }
}

