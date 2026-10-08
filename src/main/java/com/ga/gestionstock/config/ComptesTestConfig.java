package com.ga.gestionstock.config;

import com.ga.gestionstock.dto.UtilisateurRequest;
import com.ga.gestionstock.entity.Role;
import com.ga.gestionstock.repository.UtilisateurRepository;
import com.ga.gestionstock.service.UtilisateurService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;

/** Comptes de demonstration, uniquement avec le profil local-test explicite. */
@Configuration
@Profile("local-test")
@ConditionalOnProperty(name = "app.auth.mode", havingValue = "local")
public class ComptesTestConfig {
    @Bean
    ApplicationRunner initialiserComptesTest(UtilisateurRepository utilisateurs, UtilisateurService service) {
        return args -> {
            for (var compte : new UtilisateurRequest[] {
                    new UtilisateurRequest("admin.ga", "Administrateur GA", "123456789", Role.ADMIN, "admin@example.test"),
                    new UtilisateurRequest("armand.ga", "Armand GA", "123456789", Role.LECTEUR, "armand@example.test")
            }) {
                // Ne pas ecraser un compte ni son mot de passe lors des redemarrages.
                if (utilisateurs.findByIdentifiant(compte.identifiant()).isEmpty()) service.creer(compte);
            }
        };
    }
}
