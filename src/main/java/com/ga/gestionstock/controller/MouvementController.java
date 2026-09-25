package com.ga.gestionstock.controller;

import com.ga.gestionstock.dto.*;
import com.ga.gestionstock.entity.TypeMouvement;
import com.ga.gestionstock.service.StockService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class MouvementController {
    private final StockService stock;
    public MouvementController(StockService stock) { this.stock = stock; }

    @PostMapping("/articles/{id}/mouvements")
    @ResponseStatus(HttpStatus.CREATED)
    public MouvementResponse enregistrer(@PathVariable Long id, @Valid @RequestBody MouvementRequest input,
                                         @AuthenticationPrincipal UtilisateurConnecte utilisateur) {
        return stock.enregistrer(id, input, utilisateur.id());
    }
    @GetMapping("/mouvements")
    public PageResponse<MouvementResponse> historique(@RequestParam(required = false) Long articleId,
            @RequestParam(required = false) TypeMouvement type, @RequestParam(required = false) Instant debut,
            @RequestParam(required = false) Instant fin, @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return stock.historique(articleId, type, debut, fin, page, size);
    }
}
