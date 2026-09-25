package com.ga.gestionstock.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "articles")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Article {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 60)
    private String reference;
    @Column(nullable = false, length = 150)
    private String nom;
    @Column(nullable = false, length = 40)
    private String unite;
    @Column(nullable = false)
    private long quantite;
    @Column(nullable = false)
    private long seuilAlerte;
    @Column(nullable = false)
    private boolean actif = true;
    @Version
    private Long version;

    public Article(String reference, String nom, String unite, long seuilAlerte) {
        modifier(reference, nom, unite, seuilAlerte);
    }

    public void modifier(String reference, String nom, String unite, long seuilAlerte) {
        this.reference = reference.trim();
        this.nom = nom.trim();
        this.unite = unite.trim();
        this.seuilAlerte = seuilAlerte;
    }

    public boolean stockBas() { return actif && quantite <= seuilAlerte; }
    public void definirQuantite(long quantite) { this.quantite = quantite; }
    public void definirActif(boolean actif) { this.actif = actif; }
}
