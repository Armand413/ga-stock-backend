package com.ga.gestionstock.dto;

import jakarta.validation.constraints.*;

public record ReinitialisationMotDePasseRequest(@NotBlank @Size(min = 6, max = 64) String nouveauMotDePasse) {}
