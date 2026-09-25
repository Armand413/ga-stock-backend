package com.ga.gestionstock.repository;

import com.ga.gestionstock.entity.Alerte;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface AlerteRepository extends JpaRepository<Alerte, Long>, JpaSpecificationExecutor<Alerte> {
    Optional<Alerte> findByArticleIdAndResolueLeIsNull(Long articleId);
    long countByResolueLeIsNullAndAcquitteeLeIsNull();
}
