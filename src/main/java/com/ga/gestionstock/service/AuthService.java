package com.ga.gestionstock.service;

import com.ga.gestionstock.dto.*;
import com.ga.gestionstock.entity.*;
import com.ga.gestionstock.repository.*;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Service
@Validated
@Transactional(readOnly = true)
public class AuthService {
    private final UtilisateurRepository utilisateurs;
    private final SessionAccesRepository sessions;
    private final PasswordEncoder encoder;
    private final Duration duree;
    private final SecureRandom random = new SecureRandom();
    private final String dummyHash;
    private final org.springframework.beans.factory.ObjectProvider<AnnuaireClient> annuaire;
    private final String mode;

    public AuthService(UtilisateurRepository utilisateurs, SessionAccesRepository sessions,
                       PasswordEncoder encoder, @Value("${app.auth.duree:PT15M}") Duration duree,
                       org.springframework.beans.factory.ObjectProvider<AnnuaireClient> annuaire,
                       @Value("${app.auth.mode:ad}") String mode) {
        this.utilisateurs = utilisateurs;
        this.sessions = sessions;
        this.encoder = encoder;
        this.duree = duree;
        this.annuaire = annuaire;
        this.mode = mode;
        if (!Set.of("ad", "local").contains(mode)) throw new IllegalArgumentException("AUTH_MODE doit etre ad ou local.");
        if (duree.isNegative() || duree.isZero()) throw new IllegalArgumentException("Duree de session invalide");
        this.dummyHash = encoder.encode(UUID.randomUUID().toString());
    }

    // Les echecs doivent etre conserves meme si la connexion est refusee.
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public ConnexionResponse connexion(@Valid ConnexionRequest input) {
        if ("ad".equals(mode)) return connexionAnnuaire(input);
        Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var found = utilisateurs.verrouillerParIdentifiant(input.identifiant().trim().toLowerCase(Locale.ROOT));
        if (found.isEmpty()) { correspond(input.motDePasse(), dummyHash); throw refuse(); }
        Utilisateur u = found.get();
        if (!"LOCAL".equals(u.getOrigine())) throw refuse();
        if (!u.isActif() || (u.getBloqueJusqua() != null && u.getBloqueJusqua().isAfter(now))) throw refuse();
        if (!correspond(input.motDePasse(), u.getMotDePasse())) {
            u.connexionEchouee(now);
            throw refuse();
        }
        u.connexionReussie();
        return ouvrirSession(u, now);
    }

    private ConnexionResponse connexionAnnuaire(ConnexionRequest input) {
        IdentiteAnnuaire identite = annuaire.getObject().authentifier(input.identifiant(), input.motDePasse());
        Utilisateur u = utilisateurs.verrouillerParAnnuaireId(identite.id()).orElse(null);
        if (u == null) {
            if (utilisateurs.findByIdentifiant(identite.identifiant()).isPresent())
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Identifiant deja associe a un autre compte. Contacter l'administrateur.");
            u = utilisateurs.saveAndFlush(Utilisateur.depuisAnnuaire(identite.id(), identite.identifiant(),
                    identite.nom(), identite.email(), identite.role()));
        } else {
            if (!u.isActif()) throw refuse();
            if (u.getRole() != identite.role()) sessions.deleteByUtilisateurId(u.getId());
            u.synchroniserAnnuaire(identite.identifiant(), identite.nom(), identite.email(), identite.role());
        }
        return ouvrirSession(u, Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
    }

    private ConnexionResponse ouvrirSession(Utilisateur u, Instant now) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiration = now.plus(duree);
        sessions.save(new SessionAcces(empreinte(token), u, expiration));
        return new ConnexionResponse(token, "Bearer", expiration, UtilisateurResponse.of(u));
    }

    public Optional<UtilisateurConnecte> authentifier(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) return Optional.empty();
        return sessions.findByEmpreinte(empreinte(token))
                .filter(s -> ("ad".equals(mode) ? "AD" : "LOCAL").equals(s.getUtilisateur().getOrigine()))
                .filter(s -> s.getExpiration().isAfter(Instant.now()) && s.getUtilisateur().isActif())
                .map(s -> new UtilisateurConnecte(s.getUtilisateur().getId(),
                        s.getUtilisateur().getIdentifiant(), s.getUtilisateur().getRole()));
    }

    public UtilisateurResponse moi(Long id) { return UtilisateurResponse.of(utilisateur(id)); }

    @Transactional
    public void deconnexion(String token) { sessions.deleteById(empreinte(token)); }

    @Transactional
    public void changerMotDePasse(Long id, @Valid MotDePasseRequest input) {
        Utilisateur u = utilisateurs.verrouiller(id).orElseThrow(AuthService::refuse);
        if (!"local".equals(mode) || "AD".equals(u.getOrigine()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le mot de passe doit etre modifie dans l'Active Directory.");
        if (!correspond(input.ancienMotDePasse(), u.getMotDePasse())) throw refuse();
        verifierMotDePasse(input.nouveauMotDePasse());
        u.changerMotDePasse(encoder.encode(input.nouveauMotDePasse()));
        sessions.deleteByUtilisateurId(id);
    }

    @Scheduled(fixedDelay = 3600000)
    @Transactional
    public void nettoyerSessions() { sessions.deleteByExpirationBefore(Instant.now()); }

    public static void verifierMotDePasse(String password) {
        if (password == null || password.isBlank() || password.length() < 6 || password.length() > 64
                || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le mot de passe doit contenir 6 a 64 caracteres et au plus 72 octets UTF-8.");
    }

    private boolean correspond(String password, String hash) {
        return password.getBytes(StandardCharsets.UTF_8).length <= 72 && encoder.matches(password, hash);
    }
    private Utilisateur utilisateur(Long id) { return utilisateurs.findById(id).orElseThrow(AuthService::refuse); }
    private static String empreinte(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    private static ResponseStatusException refuse() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants invalides ou compte temporairement indisponible.");
    }
}
