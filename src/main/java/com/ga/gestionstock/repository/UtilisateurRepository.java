package com.ga.gestionstock.repository;

import com.ga.gestionstock.entity.Utilisateur;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByIdentifiant(String identifiant);
    Optional<Utilisateur> findByAnnuaireId(String annuaireId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Utilisateur u where u.annuaireId = :annuaireId")
    Optional<Utilisateur> verrouillerParAnnuaireId(@Param("annuaireId") String annuaireId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Utilisateur u where u.identifiant = :identifiant")
    Optional<Utilisateur> verrouillerParIdentifiant(@Param("identifiant") String identifiant);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Utilisateur u where u.id = :id")
    Optional<Utilisateur> verrouiller(@Param("id") Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Utilisateur u order by u.id")
    List<Utilisateur> verrouillerTous();
}
