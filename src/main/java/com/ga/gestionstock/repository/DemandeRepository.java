package com.ga.gestionstock.repository;

import com.ga.gestionstock.entity.Demande;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface DemandeRepository extends JpaRepository<Demande, Long>, JpaSpecificationExecutor<Demande> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Demande d where d.id = :id")
    Optional<Demande> verrouiller(@Param("id") Long id);
}
