package com.ga.gestionstock.config;

import com.ga.gestionstock.service.AnnuaireClient;
import java.net.URI;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Configuration
@ConditionalOnProperty(name = "app.auth.mode", havingValue = "ad")
public class ActiveDirectoryConfig {
    @Bean
    AnnuaireClient annuaireClient(@Value("${app.ad.domaine:}") String domaine,
            @Value("${app.ad.url:}") String url, @Value("${app.ad.base-dn:}") String baseDn,
            @Value("${app.ad.groupe-utilisateurs:}") String groupeUtilisateurs,
            @Value("${app.ad.groupe-administrateurs:}") String groupeAdministrateurs) {
        if (domaine.isBlank() || baseDn.isBlank() || groupeUtilisateurs.isBlank() || groupeAdministrateurs.isBlank())
            throw new IllegalStateException("Configurer AD_DOMAIN, AD_BASE_DN, AD_USER_GROUP_DN et AD_ADMIN_GROUP_DN localement.");
        URI uri = URI.create(url);
        if (!"ldaps".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null)
            throw new IllegalStateException("AD_URL doit etre une adresse ldaps://serveur:636 avec certificat approuve par Java.");
        if (groupeUtilisateurs.equalsIgnoreCase(groupeAdministrateurs))
            throw new IllegalStateException("Les groupes AD utilisateurs et administrateurs doivent etre distincts.");
        var provider = new ActiveDirectoryLdapAuthenticationProvider(domaine, url, baseDn);
        provider.setConvertSubErrorCodesToExceptions(true);
        provider.setUseAuthenticationRequestCredentials(false);
        provider.setUserDetailsContextMapper(new AdUserMapper(groupeUtilisateurs, groupeAdministrateurs));
        provider.setContextEnvironmentProperties(Map.of("com.sun.jndi.ldap.connect.timeout", "5000",
                "com.sun.jndi.ldap.read.timeout", "5000", "java.naming.ldap.attributes.binary", "objectGUID"));
        return (identifiant, password) -> {
            var credentials = UsernamePasswordAuthenticationToken.unauthenticated(identifiant.trim(), password);
            try {
                var result = provider.authenticate(credentials);
                return ((AdUserMapper.AdPrincipal) result.getPrincipal()).identite();
            } catch (AuthenticationServiceException ex) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "L'Active Directory est indisponible.");
            } catch (AuthenticationException ex) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants AD invalides ou acces non autorise.");
            } finally { credentials.eraseCredentials(); }
        };
    }
}
