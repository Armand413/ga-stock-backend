package com.ga.gestionstock;

import com.ga.gestionstock.dto.*;
import com.ga.gestionstock.entity.*;
import com.ga.gestionstock.repository.*;
import com.ga.gestionstock.service.*;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Transactional
class AdMappingTests extends ApiTestSupport {
    @Autowired UtilisateurRepository utilisateurs;
    @Autowired SessionAccesRepository sessions;
    @Autowired PasswordEncoder encoder;
    @Autowired UtilisateurService utilisateurService;
    AnnuaireClient annuaire;
    AuthService authAd;

    @BeforeEach
    void preparerAnnuaireSimule() {
        annuaire = mock(AnnuaireClient.class);
        var factory = new StaticListableBeanFactory(Map.of("annuaire", annuaire));
        authAd = new AuthService(utilisateurs, sessions, encoder, Duration.ofMinutes(15),
                factory.getBeanProvider(AnnuaireClient.class), "ad");
    }
    IdentiteAnnuaire identite(String guid, String upn, Role role) {
        return new IdentiteAnnuaire(guid, upn, "Agent AD", "agent@example.test", role);
    }

    @Test
    void provisionnementSansMotDePasseEtRenommageParIdentiteStable() {
        String guid = unique();
        when(annuaire.authentifier("agent", "secretAD")).thenReturn(identite(guid, "agent@ad.test", Role.LECTEUR));
        var first = authAd.connexion(new ConnexionRequest("agent", "secretAD"));
        var u = utilisateurs.findById(first.utilisateur().id()).orElseThrow();
        assertThat(u.getMotDePasse()).isNull();
        assertThat(u.getOrigine()).isEqualTo("AD");
        assertThat(u.getAnnuaireId()).isEqualTo(guid);
        when(annuaire.authentifier("renomme", "secretAD")).thenReturn(identite(guid, "renomme@ad.test", Role.LECTEUR));
        var second = authAd.connexion(new ConnexionRequest("renomme", "secretAD"));
        assertThat(second.utilisateur().id()).isEqualTo(first.utilisateur().id());
        assertThat(second.utilisateur().identifiant()).isEqualTo("renomme@ad.test");
        assertThat(authAd.authentifier(second.accessToken())).isPresent();
        assertThat(authAd.authentifier(adminToken)).isEmpty();
    }

    @Test
    void refusAdNeRevientJamaisAuxIdentifiantsLocaux() {
        long count = sessions.count();
        when(annuaire.authentifier("admin-test", PASSWORD)).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        assertThatThrownBy(() -> authAd.connexion(new ConnexionRequest("admin-test", PASSWORD)))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(sessions.count()).isEqualTo(count);
    }

    @Test
    void changementDeGroupeRevoqueAnciennesSessions() {
        String guid = unique();
        when(annuaire.authentifier("agent", "secretAD")).thenReturn(identite(guid, "agent@ad.test", Role.ADMIN));
        var first = authAd.connexion(new ConnexionRequest("agent", "secretAD"));
        assertThat(first.utilisateur().role()).isEqualTo(Role.ADMIN);
        when(annuaire.authentifier("agent", "secretAD")).thenReturn(identite(guid, "agent@ad.test", Role.LECTEUR));
        var second = authAd.connexion(new ConnexionRequest("agent", "secretAD"));
        assertThat(second.utilisateur().role()).isEqualTo(Role.LECTEUR);
        assertThat(authAd.authentifier(first.accessToken())).isEmpty();
        assertThat(authAd.authentifier(second.accessToken())).isPresent();
    }

    @Test
    void desactivationLocaleRespecteeEtMotDePasseAdNonModifiable() {
        when(annuaire.authentifier("agent", "secretAD")).thenReturn(identite(unique(), "agent@ad.test", Role.LECTEUR));
        var result = authAd.connexion(new ConnexionRequest("agent", "secretAD"));
        assertThatThrownBy(() -> authAd.changerMotDePasse(result.utilisateur().id(), new MotDePasseRequest("secretAD", "Secret")))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> utilisateurService.reinitialiserMotDePasse(result.utilisateur().id(), new ReinitialisationMotDePasseRequest("Secret")))
                .isInstanceOf(ResponseStatusException.class);
        var u = utilisateurs.findById(result.utilisateur().id()).orElseThrow();
        u.modifier(u.getNom(), Role.LECTEUR, false);
        assertThat(authAd.authentifier(result.accessToken())).isEmpty();
        assertThatThrownBy(() -> authAd.connexion(new ConnexionRequest("agent", "secretAD")))
                .isInstanceOf(ResponseStatusException.class);
    }
}
