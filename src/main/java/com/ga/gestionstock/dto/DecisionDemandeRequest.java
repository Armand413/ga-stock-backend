package com.ga.gestionstock.dto;

import com.ga.gestionstock.entity.StatutDemande;
import jakarta.validation.constraints.*;

public record DecisionDemandeRequest(@NotNull StatutDemande statut, @NotBlank @Size(max = 1000) String reponse) {}
