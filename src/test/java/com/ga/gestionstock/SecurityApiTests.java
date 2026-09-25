package com.ga.gestionstock;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.*;

class SecurityApiTests extends ApiTestSupport {
    @Autowired JdbcTemplate jdbc;

    @Test
    void authentificationEtDocumentation() throws Exception {
        assertThat(req("GET", "/api/articles", null, null).statusCode()).isEqualTo(401);
        assertThat(req("GET", "/api/articles", null, "invalide").statusCode()).isEqualTo(401);
        assertThat(connexion("inexistant", PASSWORD).statusCode()).isEqualTo(401);
        assertThat(req("GET", "/api/auth/me", null, adminToken).statusCode()).isEqualTo(200);
        var me = body(req("GET", "/api/auth/me", null, adminToken));
        assertThat(me.has("motDePasse")).isFalse();
        assertThat(req("GET", "/actuator/health", null, null).statusCode()).isEqualTo(200);
        var spec = req("GET", "/v3/api-docs", null, null);
        assertThat(spec.statusCode()).isEqualTo(200);
        assertThat(body(spec).path("paths").has("/api/articles")).isTrue();
        assertThat(req("GET", "/swagger-ui/index.html", null, null).statusCode()).isEqualTo(200);
    }

    @Test
    void lecteurConsulteMaisNeModifiePas() throws Exception {
        String username = unique();
        utilisateur(username, "LECTEUR");
        String token = body(connexion(username, PASSWORD)).path("accessToken").asString();
        assertThat(req("GET", "/api/catalogue", null, token).statusCode()).isEqualTo(200);
        assertThat(req("GET", "/api/articles", null, token).statusCode()).isEqualTo(403);
        assertThat(req("POST", "/api/articles", Map.of(), token).statusCode()).isEqualTo(403);
        assertThat(req("GET", "/api/utilisateurs", null, token).statusCode()).isEqualTo(403);
        assertThat(req("POST", "/api/auth/logout", null, token).statusCode()).isEqualTo(204);
        assertThat(req("GET", "/api/articles", null, token).statusCode()).isEqualTo(401);
    }

    @Test
    void utilisateurSimpleNeGereNiStockNiComptes() throws Exception {
        String username = unique();
        utilisateur(username, "LECTEUR");
        String token = body(connexion(username, PASSWORD)).path("accessToken").asString();
        long id = article(0);
        var result = req("POST", "/api/articles/" + id + "/mouvements",
                Map.of("type", "ENTREE", "quantite", 2, "responsable", "Identite usurpee"), token);
        assertThat(result.statusCode()).isEqualTo(403);
        assertThat(req("POST", "/api/utilisateurs", Map.of(), token).statusCode()).isEqualTo(403);
    }

    @Test
    void desactivationRevoqueLesSessions() throws Exception {
        String username = unique();
        var user = utilisateur(username, "LECTEUR");
        String token = body(connexion(username, PASSWORD)).path("accessToken").asString();
        assertThat(req("PUT", "/api/utilisateurs/" + user.path("id").asLong(),
                Map.of("nom", "Utilisateur Test", "role", "LECTEUR", "actif", false), adminToken).statusCode()).isEqualTo(200);
        assertThat(req("GET", "/api/articles", null, token).statusCode()).isEqualTo(401);
        assertThat(connexion(username, PASSWORD).statusCode()).isEqualTo(401);
    }

    @Test
    void changementMotDePasseRevoqueToutesLesSessions() throws Exception {
        String username = unique();
        utilisateur(username, "LECTEUR");
        String token = body(connexion(username, PASSWORD)).path("accessToken").asString();
        String token2 = body(connexion(username, PASSWORD)).path("accessToken").asString();
        var result = req("PUT", "/api/auth/password", Map.of("ancienMotDePasse", PASSWORD,
                "nouveauMotDePasse", "Secret"), token);
        assertThat(result.statusCode()).isEqualTo(204);
        assertThat(req("GET", "/api/articles", null, token).statusCode()).isEqualTo(401);
        assertThat(req("GET", "/api/articles", null, token2).statusCode()).isEqualTo(401);
        assertThat(connexion(username, PASSWORD).statusCode()).isEqualTo(401);
        assertThat(connexion(username, "Secret").statusCode()).isEqualTo(200);
    }

    @Test
    void jetonExpireRefuseEtEmpreinteStockee() throws Exception {
        String username = unique();
        var user = utilisateur(username, "LECTEUR");
        String token = body(connexion(username, PASSWORD)).path("accessToken").asString();
        long id = user.path("id").asLong();
        String hash = jdbc.queryForObject("select empreinte from sessions_acces where utilisateur_id = ?", String.class, id);
        assertThat(hash).hasSize(64).isNotEqualTo(token);
        jdbc.update("update sessions_acces set expiration = TIMESTAMP '2000-01-01 00:00:00' where utilisateur_id = ?", id);
        assertThat(req("GET", "/api/articles", null, token).statusCode()).isEqualTo(401);
    }

    @Test
    void echecsConnexionPersistesEtBlocageTemporaire() throws Exception {
        String username = unique();
        var user = utilisateur(username, "LECTEUR");
        for (int i = 0; i < 5; i++) assertThat(connexion(username, "mauvais-secret").statusCode()).isEqualTo(401);
        assertThat(connexion(username, PASSWORD).statusCode()).isEqualTo(401);
        assertThat(jdbc.queryForObject("select echecs_connexion from utilisateurs where id = ?", Integer.class,
                user.path("id").asLong())).isEqualTo(5);
        jdbc.update("update utilisateurs set bloque_jusqua = TIMESTAMP '2000-01-01 00:00:00' where id = ?", user.path("id").asLong());
        assertThat(connexion(username, PASSWORD).statusCode()).isEqualTo(200);
    }

    @Test
    void dernierAdministrateurProtegeEtIdentifiantsUniques() throws Exception {
        assertThat(req("PUT", "/api/utilisateurs/" + adminId, Map.of("nom", "Administrateur Test",
                "role", "LECTEUR", "actif", true), adminToken).statusCode()).isEqualTo(409);
        String username = unique();
        utilisateur(username, "LECTEUR");
        var duplicate = req("POST", "/api/utilisateurs", Map.of("identifiant", username.toUpperCase(Locale.ROOT),
                "nom", "Doublon", "motDePasse", PASSWORD, "role", "LECTEUR"), adminToken);
        assertThat(duplicate.statusCode()).isEqualTo(409);
        assertThat(req("POST", "/api/utilisateurs", Map.of("identifiant", unique(), "nom", "Test",
                "motDePasse", "court", "role", "LECTEUR"), adminToken).statusCode()).isEqualTo(400);
        assertThat(req("POST", "/api/utilisateurs", Map.of("identifiant", unique(), "nom", "Test",
                "motDePasse", "Secret", "role", "LECTEUR"), adminToken).statusCode()).isEqualTo(201);
    }

    @Test
    void reinitialisationAdministrateurEtChangementRoleRevoquentLesSessions() throws Exception {
        String username = unique();
        var user = utilisateur(username, "ADMIN");
        long id = user.path("id").asLong();
        String token = body(connexion(username, PASSWORD)).path("accessToken").asString();
        assertThat(req("PUT", "/api/utilisateurs/" + id,
                Map.of("nom", "Utilisateur Test", "role", "LECTEUR", "actif", true), adminToken).statusCode()).isEqualTo(200);
        assertThat(req("GET", "/api/articles", null, token).statusCode()).isEqualTo(401);
        String nextToken = body(connexion(username, PASSWORD)).path("accessToken").asString();
        assertThat(req("POST", "/api/articles", Map.of(), nextToken).statusCode()).isEqualTo(403);
        assertThat(req("PUT", "/api/utilisateurs/" + id + "/password",
                Map.of("nouveauMotDePasse", "Autre6"), adminToken).statusCode()).isEqualTo(204);
        assertThat(req("GET", "/api/articles", null, nextToken).statusCode()).isEqualTo(401);
        assertThat(connexion(username, "Autre6").statusCode()).isEqualTo(200);
    }

    @Test
    void corsLimiteAuxOriginesConfigurees() throws Exception {
        var allowed = java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://localhost:" + port + "/api/articles"))
                .header("Origin", "http://localhost:4200").header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization").method("OPTIONS", java.net.http.HttpRequest.BodyPublishers.noBody()).build();
        var response = client.send(allowed, java.net.http.HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).contains("http://localhost:4200");
        var denied = java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://localhost:" + port + "/api/articles"))
                .header("Origin", "https://inconnu.example").header("Access-Control-Request-Method", "GET")
                .method("OPTIONS", java.net.http.HttpRequest.BodyPublishers.noBody()).build();
        assertThat(client.send(denied, java.net.http.HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(403);
    }
}
