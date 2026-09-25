package com.ga.gestionstock;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class StockApiTests extends ApiTestSupport {
    @Test
    void stockHistoriqueEtRefusSansEffet() throws Exception {
        long id = article(5);
        assertThat(mouvement(id, "ENTREE", 10).statusCode()).isEqualTo(201);
        var sortie = mouvement(id, "SORTIE", 5);
        assertThat(sortie.statusCode()).isEqualTo(201);
        assertThat(body(sortie).path("utilisateurId").asLong()).isEqualTo(adminId);
        assertThat(body(sortie).path("responsable").asString()).isEqualTo("Administrateur Test");
        assertThat(mouvement(id, "SORTIE", 6).statusCode()).isEqualTo(409);
        var article = body(req("GET", "/api/articles/" + id, null, adminToken));
        assertThat(article.path("quantite").asLong()).isEqualTo(5);
        assertThat(article.path("stockBas").asBoolean()).isTrue();
        var historique = body(req("GET", "/api/mouvements?articleId=" + id, null, adminToken));
        assertThat(historique.path("totalElements").asLong()).isEqualTo(2);
        assertThat(historique.path("content").get(0).path("stockApres").asLong()).isEqualTo(5);
    }

    @Test
    void cycleAlerteAcquittementReapprovisionnementEtArchivage() throws Exception {
        long id = article(2);
        var liste = body(req("GET", "/api/alertes?size=100", null, adminToken)).path("content");
        long alerteId = -1;
        for (var a : liste) if (a.path("articleId").asLong() == id) alerteId = a.path("id").asLong();
        assertThat(alerteId).isPositive();
        var acquittee = req("PATCH", "/api/alertes/" + alerteId + "/acquittement", null, adminToken);
        assertThat(acquittee.statusCode()).isEqualTo(200);
        assertThat(body(acquittee).path("acquitteeLe").isNull()).isFalse();
        assertThat(body(req("PATCH", "/api/alertes/" + alerteId + "/acquittement", null, adminToken))
                .path("acquitteeLe")).isEqualTo(body(acquittee).path("acquitteeLe"));
        mouvement(id, "ENTREE", 10);
        assertThat(req("PATCH", "/api/articles/" + id + "/etat", Map.of("actif", false), adminToken).statusCode()).isEqualTo(409);
        mouvement(id, "SORTIE", 10);
        var all = body(req("GET", "/api/alertes?inclureResolues=true&size=100", null, adminToken)).path("content");
        int nombre = 0;
        int ouvertes = 0;
        for (var a : all) if (a.path("articleId").asLong() == id) {
            nombre++;
            if (a.path("resolueLe").isNull()) ouvertes++;
        }
        assertThat(nombre).isEqualTo(2);
        assertThat(ouvertes).isEqualTo(1);
        assertThat(req("PATCH", "/api/articles/" + id + "/etat", Map.of("actif", false), adminToken).statusCode()).isEqualTo(200);
        assertThat(mouvement(id, "ENTREE", 1).statusCode()).isEqualTo(409);
        assertThat(body(req("GET", "/api/mouvements?articleId=" + id, null, adminToken)).path("totalElements").asLong()).isEqualTo(2);
        assertThat(req("PATCH", "/api/articles/" + id + "/etat", Map.of("actif", true), adminToken).statusCode()).isEqualTo(200);
        assertThat(mouvement(id, "ENTREE", 3).statusCode()).isEqualTo(201);
    }

    @Test
    void validationDoublonsEtArticleAbsent() throws Exception {
        assertThat(req("POST", "/api/articles", Map.of(), adminToken).statusCode()).isEqualTo(400);
        long id = article(0);
        var article = body(req("GET", "/api/articles/" + id, null, adminToken));
        var duplicate = Map.of("reference", article.path("reference").asString(), "nom", "Test", "unite", "piece", "seuilAlerte", 0);
        assertThat(req("POST", "/api/articles", duplicate, adminToken).statusCode()).isEqualTo(409);
        assertThat(mouvement(id, "ENTREE", 0).statusCode()).isEqualTo(400);
        assertThat(mouvement(id, "SORTIE", -1).statusCode()).isEqualTo(400);
        assertThat(mouvement(id, "INCONNU", 1).statusCode()).isEqualTo(400);
        assertThat(req("GET", "/api/articles?size=0", null, adminToken).statusCode()).isEqualTo(400);
        assertThat(req("GET", "/api/articles?page=-1", null, adminToken).statusCode()).isEqualTo(400);
        assertThat(req("GET", "/api/articles/999999999", null, adminToken).statusCode()).isEqualTo(404);
        assertThat(req("GET", "/api/mouvements?debut=2026-12-31T00:00:00Z&fin=2026-01-01T00:00:00Z", null, adminToken).statusCode()).isEqualTo(400);
    }

    @Test
    void filtresEtModificationSeuil() throws Exception {
        long id = article(0);
        mouvement(id, "ENTREE", 5);
        mouvement(id, "SORTIE", 1);
        String reference = unique();
        var updated = req("PUT", "/api/articles/" + id,
                Map.of("reference", reference, "nom", "Encre speciale", "unite", "cartouche", "seuilAlerte", 4), adminToken);
        assertThat(updated.statusCode()).isEqualTo(200);
        assertThat(body(updated).path("stockBas").asBoolean()).isTrue();
        assertThat(body(req("GET", "/api/articles?recherche=" + reference + "&actif=true&stockBas=true", null, adminToken))
                .path("totalElements").asLong()).isEqualTo(1);
        assertThat(body(req("GET", "/api/mouvements?articleId=" + id + "&type=SORTIE", null, adminToken))
                .path("totalElements").asLong()).isEqualTo(1);
        assertThat(body(req("GET", "/api/tableau-bord", null, adminToken)).path("articlesActifs").asLong()).isPositive();
    }

    @Test
    void debordementNeModifieNiStockNiHistorique() throws Exception {
        long id = article(0);
        assertThat(mouvement(id, "ENTREE", Long.MAX_VALUE).statusCode()).isEqualTo(201);
        assertThat(mouvement(id, "ENTREE", 1).statusCode()).isEqualTo(409);
        assertThat(body(req("GET", "/api/articles/" + id, null, adminToken)).path("quantite").asLong()).isEqualTo(Long.MAX_VALUE);
        assertThat(body(req("GET", "/api/mouvements?articleId=" + id, null, adminToken)).path("totalElements").asLong()).isEqualTo(1);
    }

    @Test
    void exportCsvNeutraliseLesFormules() throws Exception {
        var input = Map.of("reference", unique(), "nom", "=1+1", "unite", "piece", "seuilAlerte", 0);
        assertThat(req("POST", "/api/articles", input, adminToken).statusCode()).isEqualTo(201);
        var csv = req("GET", "/api/articles/export?recherche=" + input.get("reference"), null, adminToken);
        assertThat(csv.statusCode()).isEqualTo(200);
        assertThat(csv.body()).contains("\"'=1+1\"");
        assertThat(csv.headers().firstValue("Content-Disposition").orElseThrow()).contains("stock.csv");
    }
}
