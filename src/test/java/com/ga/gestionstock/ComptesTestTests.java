package com.ga.gestionstock;

import com.ga.gestionstock.dto.ConnexionRequest;
import com.ga.gestionstock.entity.Role;
import com.ga.gestionstock.repository.UtilisateurRepository;
import com.ga.gestionstock.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:comptes-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password="
})
@ActiveProfiles("local-test")
class ComptesTestTests {
    @Autowired AuthService auth;
    @Autowired UtilisateurRepository utilisateurs;

    @Test
    void comptesConnectablesAvecLesRolesDemandesEtMotsDePasseHashes() {
        for (String identifiant : new String[] {"armand.ga", "admin.ga"}) {
            var session = auth.connexion(new ConnexionRequest(identifiant, "123456789"));
            assertThat(session.utilisateur().role()).isEqualTo(identifiant.equals("admin.ga") ? Role.ADMIN : Role.LECTEUR);
            assertThat(session.utilisateur().origine()).isEqualTo("LOCAL");
            assertThat(utilisateurs.findByIdentifiant(identifiant).orElseThrow().getMotDePasse())
                    .startsWith("$2").isNotEqualTo("123456789");
        }
    }
}
