package com.ga.gestionstock;

import com.ga.gestionstock.dto.MouvementRequest;
import com.ga.gestionstock.entity.TypeMouvement;
import com.ga.gestionstock.service.StockService;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

class StockConcurrencyTests extends ApiTestSupport {
    @Autowired StockService stock;

    @Test
    void deuxSortiesSimultaneesNeConsommentPasLeMemeStock() throws Exception {
        long id = article(0);
        mouvement(id, "ENTREE", 10);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> sortie = () -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Synchronisation impossible");
                try {
                    stock.enregistrer(id, new MouvementRequest(TypeMouvement.SORTIE, 7L, "Test", null), adminId);
                    return true;
                } catch (ResponseStatusException ex) {
                    assertThat(ex.getStatusCode().value()).isEqualTo(409);
                    return false;
                }
            };
            var a = executor.submit(sortie);
            var b = executor.submit(sortie);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(a.get(20, TimeUnit.SECONDS)).isNotEqualTo(b.get(20, TimeUnit.SECONDS));
        }
        assertThat(stock.article(id).quantite()).isEqualTo(3);
        assertThat(stock.historique(id, null, null, null, 0, 20).totalElements()).isEqualTo(2);
    }
}
