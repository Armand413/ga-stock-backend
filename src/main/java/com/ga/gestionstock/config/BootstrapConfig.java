package com.ga.gestionstock.config;

import com.ga.gestionstock.dto.UtilisateurRequest;
import com.ga.gestionstock.entity.Role;
import com.ga.gestionstock.repository.UtilisateurRepository;
import com.ga.gestionstock.service.UtilisateurService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;

@Configuration
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.auth.mode", havingValue = "local")
public class BootstrapConfig {
    @Bean
    ApplicationRunner initialiserAdministrateur(UtilisateurRepository utilisateurs, UtilisateurService service,
            @Value("${app.bootstrap.identifiant:}") String identifiant,
            @Value("${app.bootstrap.nom:Administrateur GA}") String nom,
            @Value("${app.bootstrap.mot-de-passe:}") String password) {
        return args -> {
            if (utilisateurs.count() != 0) return;
            if (identifiant.isBlank() || password.isBlank())
                throw new IllegalStateException("Premier demarrage : renseignez ADMIN_USERNAME et ADMIN_PASSWORD (6 caracteres minimum).");
            service.creer(new UtilisateurRequest(identifiant, nom, password, Role.ADMIN, null));
        };
    }
}
