package com.ga.gestionstock.dto;

import jakarta.validation.constraints.*;
public record ConnexionRequest(@NotBlank @Size(max = 254) String identifiant,
                               @NotBlank @Size(max = 1024) String motDePasse) {}
