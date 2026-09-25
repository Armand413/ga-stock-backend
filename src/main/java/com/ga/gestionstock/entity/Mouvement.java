package com.ga.gestionstock.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mouvements")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Mouvement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private TypeMouvement type;
    @Column(nullable = false)
    private long quantite;
    @Column(nullable = false)
    private long stockApres;
    @Column(nullable = false, length = 150)
    private String responsable;
    // Nullable uniquement pour conserver les mouvements anterieurs a l'authentification.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;
    @Column(length = 150)
    private String beneficiaire;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "beneficiaire_id")
    private Utilisateur utilisateurBeneficiaire;
    @Column(length = 500)
    private String motif;
    @Column(nullable = false)
    private Instant date;

    public Mouvement(Article article, TypeMouvement type, long quantite, Utilisateur utilisateur,
                     String beneficiaire, String motif) {
        this.article = article;
        this.type = type;
        this.quantite = quantite;
        this.stockApres = article.getQuantite();
        this.utilisateur = utilisateur;
        this.responsable = utilisateur.getNom();
        this.beneficiaire = beneficiaire;
        this.motif = motif;
        this.date = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }
    public void attribuerBeneficiaire(Utilisateur utilisateur) {
        utilisateurBeneficiaire = utilisateur;
        beneficiaire = utilisateur.getNom();
    }
}
