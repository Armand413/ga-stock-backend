package com.ga.gestionstock.dto;

import jakarta.validation.constraints.*;
public record MotDePasseRequest(@NotBlank @Size(max = 72) String ancienMotDePasse,
                               @NotBlank @Size(min = 6, max = 64) String nouveauMotDePasse) {}

