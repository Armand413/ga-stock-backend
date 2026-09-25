package com.ga.gestionstock.dto;

import com.ga.gestionstock.entity.TypeMouvement;
import jakarta.validation.constraints.*;
public record MouvementRequest(@NotNull TypeMouvement type, @NotNull @Positive Long quantite,
                               @Size(max = 150) String beneficiaire, @Size(max = 500) String motif,
                               @Positive Long beneficiaireId) {
    public MouvementRequest(TypeMouvement type, Long quantite, String beneficiaire, String motif) {
        this(type, quantite, beneficiaire, motif, null);
    }
}
