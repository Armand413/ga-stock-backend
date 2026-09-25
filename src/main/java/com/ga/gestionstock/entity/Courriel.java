package com.ga.gestionstock.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "courriels")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Courriel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "demande_id", nullable = false)
    private Demande demande;
    @Column(nullable = false, length = 254) private String destinataire;
    @Column(nullable = false, length = 200) private String sujet;
    @Column(nullable = false, columnDefinition = "text") private String contenu;
    @Column(nullable = false) private Instant creeLe;
    private Instant envoyeLe;
    @Column(nullable = false) private Instant prochaineTentative;
    @Column(nullable = false) private int tentatives;
    @Column(length = 500) private String derniereErreur;

    public Courriel(Demande demande, String destinataire, String sujet, String contenu) {
        this.demande = demande;
        this.destinataire = destinataire;
        this.sujet = sujet;
        this.contenu = contenu;
        this.creeLe = Instant.now();
        this.prochaineTentative = creeLe;
    }
    public void envoye() { tentatives++; envoyeLe = Instant.now(); derniereErreur = null; }
    public void echec(String erreur) {
        tentatives++;
        derniereErreur = erreur;
        prochaineTentative = Instant.now().plusSeconds(Math.min(3600, 60L << Math.min(tentatives - 1, 6)));
    }
    public void reessayer() { prochaineTentative = Instant.now(); tentatives = 0; derniereErreur = null; }
}
