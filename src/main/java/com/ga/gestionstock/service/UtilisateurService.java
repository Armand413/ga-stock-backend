package com.ga.gestionstock.service;

import com.ga.gestionstock.dto.*;
import com.ga.gestionstock.entity.*;
import com.ga.gestionstock.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Service
@Validated
@Transactional(readOnly = true)
public class UtilisateurService {
    private final UtilisateurRepository utilisateurs;
    private final SessionAccesRepository sessions;
    private final PasswordEncoder encoder;
    @org.springframework.beans.factory.annotation.Value("${app.auth.mode:ad}")
    private String mode;

    public UtilisateurService(UtilisateurRepository utilisateurs, SessionAccesRepository sessions, PasswordEncoder encoder) {
        this.utilisateurs = utilisateurs;
        this.sessions = sessions;
        this.encoder = encoder;
    }

    public PageResponse<UtilisateurResponse> lister(@Min(0) int page, @Min(1) @Max(100) int size) {
        return PageResponse.of(utilisateurs.findAll(PageRequest.of(page, size, Sort.by("identifiant")))
                .map(UtilisateurResponse::of));
    }

    @Transactional
    public UtilisateurResponse creer(@Valid UtilisateurRequest input) {
        if (!"local".equals(mode)) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Les comptes sont provisionnes depuis l'Active Directory a la connexion.");
        if (input.role() == Role.GESTIONNAIRE) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Roles disponibles : ADMIN ou LECTEUR (utilisateur simple).");
        AuthService.verifierMotDePasse(input.motDePasse());
        Utilisateur u = new Utilisateur(input.identifiant(), input.nom(), encoder.encode(input.motDePasse()), input.role());
        u.definirEmail(input.email());
        return UtilisateurResponse.of(utilisateurs.saveAndFlush(u));
    }

    @Transactional
    public UtilisateurResponse modifier(Long id, @Valid UtilisateurUpdateRequest input) {
        if (input.role() == Role.GESTIONNAIRE) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role obsolete.");
        // Serialiser les changements de droits pour ne jamais supprimer le dernier administrateur.
        var tous = utilisateurs.verrouillerTous();
        Utilisateur u = tous.stream().filter(item -> item.getId().equals(id)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));
        if ("AD".equals(u.getOrigine()) && (input.role() != u.getRole() || !input.nom().equals(u.getNom())))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le nom et le role de ce compte sont geres par l'AD.");
        if (u.isActif() && u.getRole() == Role.ADMIN && (!input.actif() || input.role() != Role.ADMIN)
                && tous.stream().filter(item -> item.isActif() && item.getRole() == Role.ADMIN
                        && item.getOrigine().equals(u.getOrigine())).count() == 1)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Conservez au moins un administrateur actif.");
        if (!input.actif() || input.role() != u.getRole()) sessions.deleteByUtilisateurId(id);
        u.modifier(input.nom(), input.role(), input.actif());
        return UtilisateurResponse.of(u);
    }

    @Transactional
    public void reinitialiserMotDePasse(Long id, @Valid ReinitialisationMotDePasseRequest input) {
        AuthService.verifierMotDePasse(input.nouveauMotDePasse());
        Utilisateur u = utilisateurs.verrouiller(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));
        if ("AD".equals(u.getOrigine()) || !"local".equals(mode))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le mot de passe doit etre modifie dans l'Active Directory.");
        u.changerMotDePasse(encoder.encode(input.nouveauMotDePasse()));
        u.connexionReussie();
        sessions.deleteByUtilisateurId(id);
    }
    @Transactional
    public UtilisateurResponse permissions(Long id, @Valid PermissionsRequest input) {
        Utilisateur u = utilisateurs.verrouiller(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));
        if (u.getRole() == Role.ADMIN) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Un administrateur dispose deja de toutes les permissions.");
        if (!u.getPermissions().equals(input.permissions())) {
            u.definirPermissions(input.permissions());
            sessions.deleteByUtilisateurId(id);
        }
        return UtilisateurResponse.of(u);
    }}

