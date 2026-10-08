package com.ga.gestionstock.dto;
import com.ga.gestionstock.entity.*;
import java.util.Set;
public record UtilisateurConnecte(Long id, String identifiant, Role role, Set<Permission> permissions) {
 public UtilisateurConnecte(Long id, String identifiant, Role role) { this(id, identifiant, role, Set.of()); }
}
