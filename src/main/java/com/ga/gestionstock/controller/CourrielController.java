package com.ga.gestionstock.controller;

import com.ga.gestionstock.dto.*;
import com.ga.gestionstock.service.CourrielService;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courriels")
public class CourrielController {
    private final CourrielService service;
    public CourrielController(CourrielService service) { this.service = service; }
    @GetMapping
    public PageResponse<CourrielResponse> enAttente(@RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) { return service.enAttente(page, size); }
    @PostMapping("/{id}/reessayer") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reessayer(@PathVariable Long id) { service.reessayer(id); }
}
