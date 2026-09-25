package com.ga.gestionstock.dto;

import com.ga.gestionstock.entity.Role;

public record IdentiteAnnuaire(String id, String identifiant, String nom, String email, Role role) {}
