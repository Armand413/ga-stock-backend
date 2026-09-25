package com.ga.gestionstock;

import com.ga.gestionstock.dto.DemandeRequest;
import com.ga.gestionstock.service.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CourrielTests extends ApiTestSupport {
    @MockitoBean MailTransport transport;
    @Autowired DemandeService demandes;
    @Autowired CourrielService courriels;
    @Autowired JdbcTemplate jdbc;

    @Test
    void panneSmtpPuisRepriseSansPerteNiNouvelEnvoiApresSucces() throws Exception {
        String username = unique();
        long userId = utilisateur(username, "LECTEUR").path("id").asLong();
        long id = demandes.creer(new DemandeRequest(article(0), 1L, "Test"), userId).id();
        long mailId = jdbc.queryForObject("select min(id) from courriels where demande_id = ?", Long.class, id);
        doThrow(new org.springframework.mail.MailSendException("smtp password should never be stored"))
                .when(transport).envoyer(anyString(), anyString(), anyString());
        courriels.envoyer(mailId);
        assertThat(jdbc.queryForObject("select tentatives from courriels where id = ?", Integer.class, mailId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select derniere_erreur from courriels where id = ?", String.class, mailId)).doesNotContain("password");
        assertThat(jdbc.queryForObject("select count(*) from demandes where id = ?", Integer.class, id)).isEqualTo(1);
        doNothing().when(transport).envoyer(anyString(), anyString(), anyString());
        courriels.reessayer(mailId);
        courriels.envoyer(mailId);
        courriels.envoyer(mailId);
        verify(transport, times(2)).envoyer(anyString(), anyString(), anyString());
        assertThat(jdbc.queryForObject("select count(*) from courriels where id = ? and envoye_le is not null", Integer.class, mailId)).isEqualTo(1);
        assertThat(req("POST", "/api/courriels/" + mailId + "/reessayer", Map.of(), adminToken).statusCode()).isEqualTo(409);
    }
}
