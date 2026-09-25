package com.ga.gestionstock.controller;

import com.ga.gestionstock.dto.TableauBordResponse;
import com.ga.gestionstock.service.StockService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tableau-bord")
public class TableauBordController {
    private final StockService stock;
    public TableauBordController(StockService stock) { this.stock = stock; }
    @GetMapping
    public TableauBordResponse consulter() { return stock.tableauBord(); }
}
