package com.ga.gestionstock.controller;

import com.ga.gestionstock.dto.*;
import com.ga.gestionstock.service.StockService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {
    private final StockService stock;
    public ArticleController(StockService stock) { this.stock = stock; }

    @GetMapping
    public PageResponse<ArticleResponse> lister(@RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @Size(max = 150) String recherche,
            @RequestParam(required = false) Boolean actif, @RequestParam(defaultValue = "false") boolean stockBas) {
        return stock.articles(page, size, recherche, actif, stockBas);
    }
    @GetMapping("/{id}")
    public ArticleResponse consulter(@PathVariable Long id) { return stock.article(id); }
    @PostMapping
    public ResponseEntity<ArticleResponse> creer(@Valid @RequestBody ArticleRequest input) {
        ArticleResponse article = stock.creer(input);
        return ResponseEntity.created(URI.create("/api/articles/" + article.id())).body(article);
    }
    @PutMapping("/{id}")
    public ArticleResponse modifier(@PathVariable Long id, @Valid @RequestBody ArticleRequest input) {
        return stock.modifier(id, input);
    }
    @PatchMapping("/{id}/etat")
    public ArticleResponse etat(@PathVariable Long id, @Valid @RequestBody EtatArticleRequest input) {
        return stock.etat(id, input.actif());
    }
    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<String> exporter(@RequestParam(required = false) @Size(max = 150) String recherche,
            @RequestParam(required = false) Boolean actif, @RequestParam(defaultValue = "false") boolean stockBas) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=stock.csv")
                .body(stock.exporter(recherche, actif, stockBas));
    }
}
