package com.ga.gestionstock.dto;

import jakarta.validation.constraints.*;

public record DemandeRequest(@NotNull @Positive Long articleId, @NotNull @Positive Long quantite,
                             @Size(max = 1000) String motif) {}
