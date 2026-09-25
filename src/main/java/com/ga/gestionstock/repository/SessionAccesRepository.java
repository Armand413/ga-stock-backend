package com.ga.gestionstock.repository;

import com.ga.gestionstock.entity.SessionAcces;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface SessionAccesRepository extends JpaRepository<SessionAcces, String> {
    @EntityGraph(attributePaths = "utilisateur")
    Optional<SessionAcces> findByEmpreinte(String empreinte);
    void deleteByUtilisateurId(Long utilisateurId);
    void deleteByExpirationBefore(Instant date);
}
