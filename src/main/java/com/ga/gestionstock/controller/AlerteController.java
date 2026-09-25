package com.ga.gestionstock.controller;

import com.ga.gestionstock.dto.*;
import com.ga.gestionstock.service.StockService;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alertes")
public class AlerteController {
    private final StockService stock;
    public AlerteController(StockService stock) { this.stock = stock; }
    @GetMapping
    public PageResponse<AlerteResponse> lister(@RequestParam(defaultValue = "false") boolean inclureResolues,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return stock.alertes(inclureResolues, page, size);
    }
    @PatchMapping("/{id}/acquittement")
    public AlerteResponse acquitter(@PathVariable Long id, @AuthenticationPrincipal UtilisateurConnecte utilisateur) {
        return stock.acquitter(id, utilisateur.id());
    }
}
