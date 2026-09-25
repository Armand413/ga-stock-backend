package com.ga.gestionstock;

import com.ga.gestionstock.dto.DecisionDemandeRequest;
import com.ga.gestionstock.entity.StatutDemande;
import com.ga.gestionstock.service.DemandeService;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.*;

class DemandeApiTests extends ApiTestSupport {
    @Autowired JdbcTemplate jdbc;
    @Autowired DemandeService demandes;
    String userToken;
    String autreToken;
    long userId;

    @BeforeEach
    void utilisateurs() throws Exception {
        String username = unique();
        userId = utilisateur(username, "LECTEUR").path("id").asLong();
        userToken = body(connexion(username, PASSWORD)).path("accessToken").asString();
        String autre = unique();
        utilisateur(autre, "LECTEUR");
        autreToken = body(connexion(autre, PASSWORD)).path("accessToken").asString();
    }
    long demander(long articleId, long quantite) throws Exception {
        var result = req("POST", "/api/demandes", Map.of("articleId", articleId, "quantite", quantite,
                "motif", "Besoin du service", "demandeurId", adminId), userToken);
        assertThat(result.statusCode()).isEqualTo(201);
        assertThat(body(result).path("demandeurId").asLong()).isEqualTo(userId);
        return body(result).path("id").asLong();
    }
    long mails(long id) { return jdbc.queryForObject("select count(*) from courriels where demande_id = ?", Long.class, id); }

    @Test
    void demandeEtApprobationAvecMailsEtHistoriquePrive() throws Exception {
        long article = article(1);
        mouvement(article, "ENTREE", 10);
        long id = demander(article, 3);
        assertThat(mails(id)).isEqualTo(2);
        assertThat(body(req("GET", "/api/articles/" + article, null, adminToken)).path("quantite").asLong()).isEqualTo(10);
        var decision = Map.of("statut", "APPROUVEE", "reponse", "Trois rames accordees.");
        assertThat(req("PATCH", "/api/demandes/" + id + "/decision", decision, userToken).statusCode()).isEqualTo(403);
        assertThat(req("PATCH", "/api/demandes/" + id + "/decision", decision, adminToken).statusCode()).isEqualTo(200);
        assertThat(mails(id)).isEqualTo(3);
        assertThat(body(req("GET", "/api/articles/" + article, null, adminToken)).path("quantite").asLong()).isEqualTo(7);
        var historique = body(req("GET", "/api/mes-consommations", null, userToken));
        assertThat(historique.path("totalElements").asLong()).isEqualTo(1);
        assertThat(historique.path("content").get(0).path("quantite").asLong()).isEqualTo(3);
        assertThat(body(req("GET", "/api/mes-consommations?utilisateurId=" + userId, null, autreToken))
                .path("totalElements").asLong()).isZero();
        assertThat(req("GET", "/api/demandes/" + id, null, autreToken).statusCode()).isEqualTo(404);
        assertThat(body(req("GET", "/api/mes-demandes?demandeurId=" + userId, null, autreToken))
                .path("totalElements").asLong()).isZero();
        assertThat(req("PATCH", "/api/demandes/" + id + "/decision", decision, adminToken).statusCode()).isEqualTo(200);
        assertThat(mails(id)).isEqualTo(3);
        assertThat(body(req("GET", "/api/articles/" + article, null, adminToken)).path("quantite").asLong()).isEqualTo(7);
    }

    @Test
    void refusEtAnnulationNeModifientPasLeStock() throws Exception {
        long article = article(0);
        long id = demander(article, 5);
        var decision = Map.of("statut", "REFUSEE", "reponse", "Article non disponible.");
        assertThat(req("PATCH", "/api/demandes/" + id + "/decision", decision, adminToken).statusCode()).isEqualTo(200);
        assertThat(mails(id)).isEqualTo(3);
        assertThat(body(req("GET", "/api/mes-consommations", null, userToken)).path("totalElements").asLong()).isZero();
        assertThat(req("PATCH", "/api/demandes/" + id + "/annulation", null, userToken).statusCode()).isEqualTo(409);
        long annulable = demander(article, 2);
        assertThat(req("PATCH", "/api/demandes/" + annulable + "/annulation", null, autreToken).statusCode()).isEqualTo(404);
        assertThat(req("PATCH", "/api/demandes/" + annulable + "/annulation", null, userToken).statusCode()).isEqualTo(200);
        assertThat(mails(annulable)).isEqualTo(4);
        assertThat(req("PATCH", "/api/demandes/" + annulable + "/annulation", null, userToken).statusCode()).isEqualTo(200);
        assertThat(mails(annulable)).isEqualTo(4);
    }

    @Test
    void stockInsuffisantLaisseLaDemandeEnAttenteSansMailDeDecision() throws Exception {
        long article = article(0);
        long id = demander(article, 2);
        assertThat(req("PATCH", "/api/demandes/" + id + "/decision",
                Map.of("statut", "APPROUVEE", "reponse", "Accord"), adminToken).statusCode()).isEqualTo(409);
        assertThat(body(req("GET", "/api/demandes/" + id, null, userToken)).path("statut").asString()).isEqualTo("EN_ATTENTE");
        assertThat(mails(id)).isEqualTo(2);
        assertThat(body(req("GET", "/api/mouvements?articleId=" + article, null, adminToken)).path("totalElements").asLong()).isZero();
    }

    @Test
    void droitsEtValidationDesDemandes() throws Exception {
        for (String path : List.of("/api/mouvements", "/api/demandes", "/api/utilisateurs", "/api/courriels", "/api/tableau-bord", "/api/articles/export", "/api/alertes"))
            assertThat(req("GET", path, null, userToken).statusCode()).as(path).isEqualTo(403);
        assertThat(req("POST", "/api/demandes", Map.of(), userToken).statusCode()).isEqualTo(400);
        long id = article(0);
        assertThat(req("POST", "/api/demandes", Map.of("articleId", id, "quantite", 0), userToken).statusCode()).isEqualTo(400);
        req("PATCH", "/api/articles/" + id + "/etat", Map.of("actif", false), adminToken);
        assertThat(req("POST", "/api/demandes", Map.of("articleId", id, "quantite", 1), userToken).statusCode()).isEqualTo(404);
        assertThat(req("GET", "/api/mes-demandes?size=101", null, userToken).statusCode()).isEqualTo(400);
    }

    @Test
    void approbationsConcurrentesNeDebitentQuUneFois() throws Exception {
        long article = article(0);
        mouvement(article, "ENTREE", 10);
        long id = demander(article, 4);
        CountDownLatch start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Long> action = () -> {
                start.await(5, TimeUnit.SECONDS);
                return demandes.decider(id, new DecisionDemandeRequest(StatutDemande.APPROUVEE, "Accord"), adminId).mouvementId();
            };
            var first = pool.submit(action);
            var second = pool.submit(action);
            start.countDown();
            assertThat(first.get(20, TimeUnit.SECONDS)).isEqualTo(second.get(20, TimeUnit.SECONDS));
        }
        assertThat(mails(id)).isEqualTo(3);
        assertThat(body(req("GET", "/api/articles/" + article, null, adminToken)).path("quantite").asLong()).isEqualTo(6);
    }
}
