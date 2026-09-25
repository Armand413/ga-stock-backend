package com.ga.gestionstock.repository;

import com.ga.gestionstock.entity.Article;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ArticleRepository extends JpaRepository<Article, Long>, JpaSpecificationExecutor<Article> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Article a where a.id = :id")
    Optional<Article> verrouiller(@Param("id") Long id);

    long countByActif(boolean actif);
    @Query("select count(a) from Article a where a.actif = true and a.quantite <= a.seuilAlerte")
    long compterStockBas();
    long countByActifTrueAndQuantite(long quantite);
}
