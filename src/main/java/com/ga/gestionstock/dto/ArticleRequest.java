package com.ga.gestionstock.dto;

import jakarta.validation.constraints.*;
public record ArticleRequest(@NotBlank @Size(max = 60) String reference,
                             @NotBlank @Size(max = 150) String nom,
                             @NotBlank @Size(max = 40) String unite,
                             @NotNull @PositiveOrZero Long seuilAlerte) {}

