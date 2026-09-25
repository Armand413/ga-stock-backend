package com.ga.gestionstock.repository;

import com.ga.gestionstock.entity.Courriel;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CourrielRepository extends JpaRepository<Courriel, Long> {
    @Query("select c.id from Courriel c where c.envoyeLe is null and c.tentatives < 8 and c.prochaineTentative <= :now order by c.id")
    List<Long> aEnvoyer(@Param("now") Instant now, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Courriel c where c.id = :id")
    Optional<Courriel> verrouiller(@Param("id") Long id);
    Page<Courriel> findByEnvoyeLeIsNull(Pageable pageable);
}
