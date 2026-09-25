package com.ga.gestionstock.service;

import com.ga.gestionstock.dto.*;
import com.ga.gestionstock.entity.*;
import com.ga.gestionstock.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Objects;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Service
@Validated
@Transactional(readOnly = true)
public class DemandeService {
    private final DemandeRepository demandes;
    private final ArticleRepository articles;
    private final UtilisateurRepository utilisateurs;
    private final MouvementRepository mouvements;
    private final StockService stock;
    private final NotificationService notifications;

    public DemandeService(DemandeRepository demandes, ArticleRepository articles, UtilisateurRepository utilisateurs,
                          MouvementRepository mouvements, StockService stock, NotificationService notifications) {
        this.demandes = demandes; this.articles = articles; this.utilisateurs = utilisateurs;
        this.mouvements = mouvements; this.stock = stock; this.notifications = notifications;
    }

    @Transactional
    public DemandeResponse creer(@Valid DemandeRequest input, Long utilisateurId) {
        Utilisateur user = utilisateur(utilisateurId);
        if (!NotificationService.adresseValide(user.getEmail())) throw conflit("Votre compte doit avoir une adresse mail valide.");
        Article article = articles.verrouiller(input.articleId()).filter(Article::isActif).orElseThrow(DemandeService::absente);
        Demande d = demandes.saveAndFlush(new Demande(user, article, input.quantite(), nettoyer(input.motif())));
        notifications.notifier(d, true);
        return DemandeResponse.of(d);
    }

    public DemandeResponse consulter(Long id, Long utilisateurId) {
        Demande d = demandes.findById(id).orElseThrow(DemandeService::absente);
        verifierAcces(d, utilisateur(utilisateurId));
        return DemandeResponse.of(d);
    }

    public PageResponse<DemandeResponse> personnelles(Long userId, StatutDemande statut, @Min(0) int page, @Min(1) @Max(100) int size) {
        return liste(userId, statut, page, size);
    }

    public PageResponse<DemandeResponse> toutes(Long adminId, StatutDemande statut, @Min(0) int page, @Min(1) @Max(100) int size) {
        administrateur(adminId);
        return liste(null, statut, page, size);
    }

    @Transactional
    public DemandeResponse decider(Long id, @Valid DecisionDemandeRequest input, Long adminId) {
        Utilisateur admin = administrateur(adminId);
        if (input.statut() != StatutDemande.APPROUVEE && input.statut() != StatutDemande.REFUSEE)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choisir APPROUVEE ou REFUSEE.");
        Demande d = demandes.verrouiller(id).orElseThrow(DemandeService::absente);
        String reponse = input.reponse().trim();
        if (d.getStatut() != StatutDemande.EN_ATTENTE) {
            if (d.getStatut() == input.statut() && Objects.equals(d.getReponse(), reponse)) return DemandeResponse.of(d);
            throw conflit("Cette demande a deja ete traitee.");
        }
        Mouvement mouvement = null;
        if (input.statut() == StatutDemande.APPROUVEE) {
            MouvementResponse sortie = stock.enregistrer(d.getArticle().getId(),
                    new MouvementRequest(TypeMouvement.SORTIE, d.getQuantite(), null,
                            "Demande #" + d.getId(), d.getDemandeur().getId()), adminId);
            mouvement = mouvements.getReferenceById(sortie.id());
        }
        d.traiter(input.statut(), admin, reponse, mouvement);
        notifications.notifier(d, false);
        return DemandeResponse.of(d);
    }

    @Transactional
    public DemandeResponse annuler(Long id, Long userId) {
        Demande d = demandes.verrouiller(id).orElseThrow(DemandeService::absente);
        Utilisateur user = utilisateur(userId);
        verifierAcces(d, user);
        if (d.getStatut() == StatutDemande.ANNULEE) return DemandeResponse.of(d);
        if (d.getStatut() != StatutDemande.EN_ATTENTE) throw conflit("Seule une demande en attente peut etre annulee.");
        d.traiter(StatutDemande.ANNULEE, user, "Demande annulee.", null);
        notifications.notifier(d, false);
        return DemandeResponse.of(d);
    }

    private PageResponse<DemandeResponse> liste(Long userId, StatutDemande statut, int page, int size) {
        Specification<Demande> filtre = (r, q, cb) -> cb.conjunction();
        if (userId != null) filtre = filtre.and((r, q, cb) -> cb.equal(r.get("demandeur").get("id"), userId));
        if (statut != null) filtre = filtre.and((r, q, cb) -> cb.equal(r.get("statut"), statut));
        return PageResponse.of(demandes.findAll(filtre, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "creeLe", "id")))
                .map(DemandeResponse::of));
    }
    private Utilisateur utilisateur(Long id) {
        return utilisateurs.findById(id).filter(Utilisateur::isActif)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Compte indisponible."));
    }
    private Utilisateur administrateur(Long id) {
        Utilisateur u = utilisateur(id);
        if (u.getRole() != Role.ADMIN) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administration requise.");
        return u;
    }
    private void verifierAcces(Demande d, Utilisateur u) {
        if (u.getRole() != Role.ADMIN && !d.getDemandeur().getId().equals(u.getId())) throw absente();
    }
    private static String nettoyer(String s) { return s == null ? null : s.trim(); }
    private static ResponseStatusException absente() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Ressource introuvable."); }
    private static ResponseStatusException conflit(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
