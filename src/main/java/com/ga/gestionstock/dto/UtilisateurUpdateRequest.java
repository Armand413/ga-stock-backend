package com.ga.gestionstock.dto;

import com.ga.gestionstock.entity.Role;
import jakarta.validation.constraints.*;
public record UtilisateurUpdateRequest(@NotBlank @Size(max = 150) String nom,
                                       @NotNull Role role, @NotNull Boolean actif) {}

