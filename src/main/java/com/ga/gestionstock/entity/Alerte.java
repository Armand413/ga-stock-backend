package com.ga.gestionstock.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "alertes")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Alerte {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;
    @Column(nullable = false)
    private Instant creeLe;
    private Instant resolueLe;
    private Instant acquitteeLe;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acquittee_par")
    private Utilisateur acquitteePar;

    public Alerte(Article article) { this.article = article; this.creeLe = maintenant(); }
    public void resoudre() { resolueLe = maintenant(); }
    public void acquitter(Utilisateur utilisateur) {
        if (acquitteeLe == null) { acquitteeLe = maintenant(); acquitteePar = utilisateur; }
    }
    private static Instant maintenant() { return Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS); }
}
