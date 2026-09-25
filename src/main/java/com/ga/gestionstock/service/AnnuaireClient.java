package com.ga.gestionstock.service;

import com.ga.gestionstock.dto.IdentiteAnnuaire;

public interface AnnuaireClient {
    IdentiteAnnuaire authentifier(String identifiant, String motDePasse);
}
