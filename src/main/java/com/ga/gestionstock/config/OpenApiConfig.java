package com.ga.gestionstock.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI openAPI() {
        return new OpenAPI().info(new Info().title("Gestion des consommables GA").version("1.0")
                .description("Connexion Active Directory, demandes de consommables, historique personnel et notifications par mail. Gestion reservee aux administrateurs. Connexion via /api/auth/login, puis bouton Authorize avec accessToken."))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("Opaque token")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
