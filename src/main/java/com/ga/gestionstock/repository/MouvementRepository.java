package com.ga.gestionstock.repository;

import com.ga.gestionstock.entity.Mouvement;
import org.springframework.data.jpa.repository.*;

public interface MouvementRepository extends JpaRepository<Mouvement, Long>, JpaSpecificationExecutor<Mouvement> {}
