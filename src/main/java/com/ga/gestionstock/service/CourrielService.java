package com.ga.gestionstock.service;

import com.ga.gestionstock.dto.*;
import com.ga.gestionstock.repository.CourrielRepository;
import jakarta.validation.constraints.*;
import java.time.Instant;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Service
@Validated
public class CourrielService {
    private final CourrielRepository courriels;
    private final MailTransport transport;
    public CourrielService(CourrielRepository courriels, MailTransport transport) {
        this.courriels = courriels;
        this.transport = transport;
    }

    @Transactional(timeout = 30)
    public void envoyer(Long id) {
        var c = courriels.verrouiller(id).orElseThrow();
        if (c.getEnvoyeLe() != null || c.getTentatives() >= 8 || c.getProchaineTentative().isAfter(Instant.now())) return;
        try {
            transport.envoyer(c.getDestinataire(), c.getSujet(), c.getContenu());
            c.envoye();
        } catch (RuntimeException ex) {
            // Ne pas stocker une erreur SMTP brute pouvant contenir des secrets ou du contenu prive.
            c.echec("Envoi non confirme par le serveur SMTP. Verifier la connexion et la configuration.");
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<CourrielResponse> enAttente(@Min(0) int page, @Min(1) @Max(100) int size) {
        return PageResponse.of(courriels.findByEnvoyeLeIsNull(PageRequest.of(page, size, Sort.by("id")))
                .map(CourrielResponse::of));
    }

    @Transactional
    public void reessayer(Long id) {
        var c = courriels.verrouiller(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Courriel introuvable."));
        if (c.getEnvoyeLe() != null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Courriel deja envoye.");
        c.reessayer();
    }
}
