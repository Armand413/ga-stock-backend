package com.ga.gestionstock.dto;

import com.ga.gestionstock.entity.Courriel;
import java.time.Instant;

public record CourrielResponse(Long id, Long demandeId, String destinataire, String sujet,
                               int tentatives, Instant prochaineTentative, String derniereErreur) {
    public static CourrielResponse of(Courriel c) {
        return new CourrielResponse(c.getId(), c.getDemande().getId(), c.getDestinataire(), c.getSujet(),
                c.getTentatives(), c.getProchaineTentative(), c.getDerniereErreur());
    }
}
