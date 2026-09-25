package com.ga.gestionstock.controller;

import com.ga.gestionstock.dto.*;
import com.ga.gestionstock.entity.StatutDemande;
import com.ga.gestionstock.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class DemandeController {
    private final DemandeService demandes;
    private final StockService stock;
    public DemandeController(DemandeService demandes, StockService stock) { this.demandes = demandes; this.stock = stock; }

    @GetMapping("/catalogue")
    public PageResponse<CatalogueArticleResponse> catalogue(@RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @Size(max = 150) String recherche) {
        var result = stock.articles(page, size, recherche, true, false);
        return new PageResponse<>(result.content().stream().map(a -> new CatalogueArticleResponse(
                a.id(), a.reference(), a.nom(), a.unite(), a.quantite() > 0)).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages());
    }
    @PostMapping("/demandes")
    public ResponseEntity<DemandeResponse> creer(@Valid @RequestBody DemandeRequest input,
            @AuthenticationPrincipal UtilisateurConnecte user) {
        var d = demandes.creer(input, user.id());
        return ResponseEntity.created(URI.create("/api/demandes/" + d.id())).body(d);
    }
    @GetMapping("/demandes/{id}")
    public DemandeResponse consulter(@PathVariable Long id, @AuthenticationPrincipal UtilisateurConnecte user) {
        return demandes.consulter(id, user.id());
    }
    @GetMapping("/mes-demandes")
    public PageResponse<DemandeResponse> personnelles(@AuthenticationPrincipal UtilisateurConnecte user,
            @RequestParam(required = false) StatutDemande statut, @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return demandes.personnelles(user.id(), statut, page, size);
    }
    @GetMapping("/demandes")
    public PageResponse<DemandeResponse> toutes(@AuthenticationPrincipal UtilisateurConnecte user,
            @RequestParam(required = false) StatutDemande statut, @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return demandes.toutes(user.id(), statut, page, size);
    }
    @PatchMapping("/demandes/{id}/decision")
    public DemandeResponse decider(@PathVariable Long id, @Valid @RequestBody DecisionDemandeRequest input,
            @AuthenticationPrincipal UtilisateurConnecte user) { return demandes.decider(id, input, user.id()); }
    @PatchMapping("/demandes/{id}/annulation")
    public DemandeResponse annuler(@PathVariable Long id, @AuthenticationPrincipal UtilisateurConnecte user) {
        return demandes.annuler(id, user.id());
    }
    @GetMapping("/mes-consommations")
    public PageResponse<MouvementResponse> consommations(@AuthenticationPrincipal UtilisateurConnecte user,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return stock.consommations(user.id(), page, size);
    }
}
