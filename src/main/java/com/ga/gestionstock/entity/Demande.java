package com.ga.gestionstock.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "demandes")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Demande {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "demandeur_id", nullable = false)
    private Utilisateur demandeur;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "article_id", nullable = false)
    private Article article;
    @Column(nullable = false) private long quantite;
    @Column(length = 1000) private String motif;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private StatutDemande statut;
    @Column(nullable = false) private Instant creeLe;
    private Instant traiteeLe;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "traitee_par") private Utilisateur traiteePar;
    @Column(length = 1000) private String reponse;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "mouvement_id", unique = true) private Mouvement mouvement;
    @Version private Long version;

    public Demande(Utilisateur demandeur, Article article, long quantite, String motif) {
        this.demandeur = demandeur;
        this.article = article;
        this.quantite = quantite;
        this.motif = motif;
        statut = StatutDemande.EN_ATTENTE;
        creeLe = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
    public void traiter(StatutDemande statut, Utilisateur acteur, String reponse, Mouvement mouvement) {
        this.statut = statut;
        this.traiteePar = acteur;
        this.reponse = reponse;
        this.mouvement = mouvement;
        this.traiteeLe = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
