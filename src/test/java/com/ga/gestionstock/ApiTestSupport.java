package com.ga.gestionstock;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class ApiTestSupport {
    @LocalServerPort int port;
    protected String adminToken;
    protected long adminId;
    protected final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    protected final JsonMapper json = new JsonMapper();
    static final String PASSWORD = "Test-Only-Password-2026";

    @BeforeEach
    void connecterAdmin() throws Exception {
        var login = connexion("admin-test", PASSWORD);
        assertThat(login.statusCode()).isEqualTo(200);
        adminToken = body(login).path("accessToken").asString();
        adminId = body(login).path("utilisateur").path("id").asLong();
    }

    protected HttpResponse<String> req(String method, String path, Object data, String token) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).timeout(Duration.ofSeconds(20));
        if (token != null) request.header("Authorization", "Bearer " + token);
        if (data != null) request.header("Content-Type", "application/json");
        request.method(method, data == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(data)));
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
    protected JsonNode body(HttpResponse<String> response) { return json.readTree(response.body()); }
    protected HttpResponse<String> connexion(String identifiant, String password) throws Exception {
        return req("POST", "/api/auth/login", Map.of("identifiant", identifiant, "motDePasse", password), null);
    }
    protected String unique() { return UUID.randomUUID().toString(); }
    protected long article(long seuil) throws Exception {
        var response = req("POST", "/api/articles", Map.of("reference", unique(), "nom", "Papier A4", "unite", "rame", "seuilAlerte", seuil), adminToken);
        assertThat(response.statusCode()).isEqualTo(201);
        return body(response).path("id").asLong();
    }
    protected HttpResponse<String> mouvement(long id, String type, long quantite) throws Exception {
        return req("POST", "/api/articles/" + id + "/mouvements", Map.of("type", type, "quantite", quantite,
                "beneficiaire", "Service comptabilite", "motif", "Test"), adminToken);
    }
    protected JsonNode utilisateur(String identifiant, String role) throws Exception {
        var response = req("POST", "/api/utilisateurs", Map.of("identifiant", identifiant,
                "nom", "Utilisateur Test", "motDePasse", PASSWORD, "role", role,
                "email", identifiant + "@example.test"), adminToken);
        assertThat(response.statusCode()).isEqualTo(201);
        return body(response);
    }
}
