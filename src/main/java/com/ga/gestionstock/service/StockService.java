package com.ga.gestionstock.service;

import com.ga.gestionstock.dto.*;
import com.ga.gestionstock.entity.*;
import com.ga.gestionstock.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.Locale;
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
public class StockService {
    private final ArticleRepository articles;
    private final MouvementRepository mouvements;
    private final AlerteRepository alertes;
    private final UtilisateurRepository utilisateurs;

    public StockService(ArticleRepository articles, MouvementRepository mouvements,
                        AlerteRepository alertes, UtilisateurRepository utilisateurs) {
        this.articles = articles;
        this.mouvements = mouvements;
        this.alertes = alertes;
        this.utilisateurs = utilisateurs;
    }

    public ArticleResponse article(Long id) { return ArticleResponse.of(trouver(id)); }

    public PageResponse<ArticleResponse> articles(@Min(0) int page, @Min(1) @Max(100) int size,
                                                 String recherche, Boolean actif, boolean stockBas) {
        return PageResponse.of(articles.findAll(filtreArticles(recherche, actif, stockBas),
                PageRequest.of(page, size, Sort.by("nom", "id"))).map(ArticleResponse::of));
    }

    @Transactional
    public ArticleResponse creer(@Valid ArticleRequest input) {
        Article article = articles.saveAndFlush(new Article(input.reference(), input.nom(), input.unite(), input.seuilAlerte()));
        synchroniserAlerte(article);
        return ArticleResponse.of(article);
    }

    @Transactional
    public ArticleResponse modifier(Long id, @Valid ArticleRequest input) {
        Article article = verrouiller(id);
        article.modifier(input.reference(), input.nom(), input.unite(), input.seuilAlerte());
        articles.flush();
        synchroniserAlerte(article);
        return ArticleResponse.of(article);
    }

    @Transactional
    public ArticleResponse etat(Long id, boolean actif) {
        Article article = verrouiller(id);
        if (!actif && article.getQuantite() != 0) throw conflit("Un article doit avoir un stock nul avant archivage.");
        article.definirActif(actif);
        synchroniserAlerte(article);
        return ArticleResponse.of(article);
    }

    @Transactional
    public MouvementResponse enregistrer(Long id, @Valid MouvementRequest input, Long utilisateurId) {
        Article article = verrouiller(id);
        if (!article.isActif()) throw conflit("Cet article est archive. Reactivez-le avant tout mouvement.");
        if (input.type() == TypeMouvement.SORTIE && input.quantite() > article.getQuantite())
            throw conflit("Stock insuffisant pour cette sortie.");
        long quantite;
        try {
            quantite = input.type() == TypeMouvement.ENTREE
                    ? Math.addExact(article.getQuantite(), input.quantite()) : article.getQuantite() - input.quantite();
        } catch (ArithmeticException ex) { throw conflit("La quantite maximale est depassee."); }
        article.definirQuantite(quantite);
        Mouvement mouvement = new Mouvement(article, input.type(), input.quantite(),
                utilisateur(utilisateurId), nettoyer(input.beneficiaire()), nettoyer(input.motif()));
        if (input.beneficiaireId() != null) {
            if (input.type() != TypeMouvement.SORTIE) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Un beneficiaire est associe uniquement aux sorties.");
            mouvement.attribuerBeneficiaire(utilisateur(input.beneficiaireId()));
        }
        mouvements.saveAndFlush(mouvement);
        synchroniserAlerte(article);
        return MouvementResponse.of(mouvement);
    }

    public PageResponse<MouvementResponse> historique(Long articleId, TypeMouvement type, Instant debut, Instant fin,
                                                      @Min(0) int page, @Min(1) @Max(100) int size) {
        if (debut != null && fin != null && debut.isAfter(fin))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date de debut doit preceder la date de fin.");
        if (articleId != null) trouver(articleId);
        Specification<Mouvement> filtre = (root, query, cb) -> cb.conjunction();
        if (articleId != null) filtre = filtre.and((r, q, cb) -> cb.equal(r.get("article").get("id"), articleId));
        if (type != null) filtre = filtre.and((r, q, cb) -> cb.equal(r.get("type"), type));
        if (debut != null) filtre = filtre.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("date"), debut));
        if (fin != null) filtre = filtre.and((r, q, cb) -> cb.lessThanOrEqualTo(r.get("date"), fin));
        return PageResponse.of(mouvements.findAll(filtre,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date", "id"))).map(MouvementResponse::of));
    }

    public PageResponse<AlerteResponse> alertes(boolean inclureResolues, @Min(0) int page, @Min(1) @Max(100) int size) {
        Specification<Alerte> filtre = (r, q, cb) -> inclureResolues ? cb.conjunction() : cb.isNull(r.get("resolueLe"));
        return PageResponse.of(alertes.findAll(filtre,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "creeLe", "id"))).map(AlerteResponse::of));
    }

    public PageResponse<MouvementResponse> consommations(Long utilisateurId, @Min(0) int page, @Min(1) @Max(100) int size) {
        Specification<Mouvement> filtre = (r, q, cb) -> cb.and(
                cb.equal(r.get("utilisateurBeneficiaire").get("id"), utilisateurId),
                cb.equal(r.get("type"), TypeMouvement.SORTIE));
        return PageResponse.of(mouvements.findAll(filtre,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date", "id"))).map(MouvementResponse::of));
    }

    @Transactional
    public AlerteResponse acquitter(Long id, Long utilisateurId) {
        Alerte alerte = alertes.findById(id).orElseThrow(() -> absent("Alerte"));
        // Meme ordre de verrouillage que les mouvements : article puis alerte.
        verrouiller(alerte.getArticle().getId());
        // Rafraichir apres attente du verrou, pour conserver le premier acquittement.
        entityManager.refresh(alerte);
        alerte.acquitter(utilisateur(utilisateurId));
        return AlerteResponse.of(alerte);
    }

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public TableauBordResponse tableauBord() {
        return new TableauBordResponse(articles.countByActif(true), articles.countByActif(false),
                articles.compterStockBas(), articles.countByActifTrueAndQuantite(0),
                alertes.countByResolueLeIsNullAndAcquitteeLeIsNull(), mouvements.count());
    }

    public String exporter(String recherche, Boolean actif, boolean stockBas) {
        Specification<Article> filtre = filtreArticles(recherche, actif, stockBas);
        if (articles.count(filtre) > 10000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Export limite a 10000 articles. Affinez la recherche.");
        StringBuilder csv = new StringBuilder("\uFEFFReference;Nom;Unite;Quantite;Seuil;Stock bas;Actif\r\n");
        for (Article a : articles.findAll(filtre, PageRequest.of(0, 10000, Sort.by("nom", "id")))) {
            csv.append(cellule(a.getReference())).append(';').append(cellule(a.getNom())).append(';')
                    .append(cellule(a.getUnite())).append(';').append(a.getQuantite()).append(';')
                    .append(a.getSeuilAlerte()).append(';').append(a.stockBas()).append(';').append(a.isActif()).append("\r\n");
        }
        return csv.toString();
    }

    private Specification<Article> filtreArticles(String recherche, Boolean actif, boolean stockBas) {
        Specification<Article> filtre = (r, q, cb) -> cb.conjunction();
        if (actif != null) filtre = filtre.and((r, q, cb) -> cb.equal(r.get("actif"), actif));
        if (stockBas) filtre = filtre.and((r, q, cb) -> cb.and(cb.isTrue(r.get("actif")),
                cb.lessThanOrEqualTo(r.get("quantite"), r.get("seuilAlerte"))));
        if (recherche != null && !recherche.isBlank()) {
            String terme = "%" + recherche.trim().toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%")
                    .replace("_", "!_") + "%";
            filtre = filtre.and((r, q, cb) -> cb.or(cb.like(cb.lower(r.get("nom")), terme, '!'),
                    cb.like(cb.lower(r.get("reference")), terme, '!')));
        }
        return filtre;
    }

    private void synchroniserAlerte(Article article) {
        var ouverte = alertes.findByArticleIdAndResolueLeIsNull(article.getId());
        if (article.stockBas() && ouverte.isEmpty()) alertes.save(new Alerte(article));
        else if (!article.stockBas()) ouverte.ifPresent(Alerte::resoudre);
    }
    private Article trouver(Long id) { return articles.findById(id).orElseThrow(() -> absent("Article")); }
    private Article verrouiller(Long id) { return articles.verrouiller(id).orElseThrow(() -> absent("Article")); }
    private Utilisateur utilisateur(Long id) {
        return utilisateurs.findById(id).filter(Utilisateur::isActif)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Compte indisponible."));
    }
    private static String nettoyer(String value) { return value == null ? null : value.trim(); }
    private static String cellule(String value) {
        // Neutraliser les formules lors de l'ouverture dans un tableur.
        String stripped = value.stripLeading();
        if (!stripped.isEmpty() && "=+-@".indexOf(stripped.charAt(0)) >= 0) value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    private static ResponseStatusException absent(String objet) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, objet + " introuvable.");
    }
    private static ResponseStatusException conflit(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
