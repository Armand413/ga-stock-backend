package com.ga.gestionstock.dto;

import com.ga.gestionstock.entity.Role;
import jakarta.validation.constraints.*;
public record UtilisateurRequest(@NotBlank @Pattern(regexp = "[a-zA-Z0-9._-]{3,80}") String identifiant,
                                 @NotBlank @Size(max = 150) String nom,
                                 @NotBlank @Size(min = 6, max = 64) String motDePasse,
                                 @NotNull Role role, @Email @Size(max = 254) String email) {}
