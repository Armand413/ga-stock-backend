package com.ga.gestionstock.dto;

public record TableauBordResponse(long articlesActifs, long articlesArchives,
        long articlesStockBas, long articlesEnRupture,
        long alertesNonAcquittees, long mouvements) {
}
