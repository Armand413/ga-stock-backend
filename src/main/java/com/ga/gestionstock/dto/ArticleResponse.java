package com.ga.gestionstock.dto;

import com.ga.gestionstock.entity.Article;

public record ArticleResponse(Long id, String reference, String nom, String unite, long quantite,
        long seuilAlerte, boolean stockBas, boolean actif) {
    public static ArticleResponse of(Article a) {
        return new ArticleResponse(a.getId(), a.getReference(), a.getNom(), a.getUnite(),
                a.getQuantite(), a.getSeuilAlerte(), a.stockBas(), a.isActif());
    }
}
