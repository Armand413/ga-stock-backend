package com.ga.gestionstock.config;

import com.ga.gestionstock.repository.CourrielRepository;
import com.ga.gestionstock.service.CourrielService;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class CourrielScheduler {
    private final CourrielRepository courriels;
    private final CourrielService service;
    public CourrielScheduler(CourrielRepository courriels, CourrielService service) {
        this.courriels = courriels;
        this.service = service;
    }
    @Scheduled(fixedDelayString = "${app.mail.intervalle:30000}", initialDelay = 10000)
    public void distribuer() {
        for (Long id : courriels.aEnvoyer(Instant.now(), PageRequest.of(0, 10))) service.envoyer(id);
    }
}
