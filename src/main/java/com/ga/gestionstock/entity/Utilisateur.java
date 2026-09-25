package com.ga.gestionstock.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Locale;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "utilisateurs")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Utilisateur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 254)
    private String identifiant;
    @Column(nullable = false, length = 150)
    private String nom;
    @Column(length = 100)
    private String motDePasse;
    @Column(length = 254)
    private String email;
    @Column(unique = true, length = 128)
    private String annuaireId;
    @Column(nullable = false, length = 20)
    private String origine = "LOCAL";
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;
    @Column(nullable = false)
    private boolean actif = true;
    @Column(nullable = false)
    private int echecsConnexion;
    private Instant bloqueJusqua;

    public Utilisateur(String identifiant, String nom, String motDePasse, Role role) {
        this.identifiant = identifiant.trim().toLowerCase(Locale.ROOT);
        this.nom = nom.trim();
        this.motDePasse = motDePasse;
        this.role = role;
    }

    public void modifier(String nom, Role role, boolean actif) {
        this.nom = nom.trim();
        this.role = role;
        this.actif = actif;
    }

    public void changerMotDePasse(String hash) {
        motDePasse = hash;
    }

    public void definirEmail(String email) { this.email = email; }

    public static Utilisateur depuisAnnuaire(String annuaireId, String upn, String nom, String email, Role role) {
        Utilisateur u = new Utilisateur(upn, nom, null, role);
        u.origine = "AD";
        u.annuaireId = annuaireId;
        u.email = email;
        return u;
    }

    public void synchroniserAnnuaire(String upn, String nom, String email, Role role) {
        this.identifiant = upn.toLowerCase(Locale.ROOT);
        this.nom = nom;
        this.email = email;
        this.role = role;
    }

    public void connexionReussie() {
        echecsConnexion = 0;
        bloqueJusqua = null;
    }

    public void connexionEchouee(Instant maintenant) {
        if (bloqueJusqua != null && !bloqueJusqua.isAfter(maintenant))
            echecsConnexion = 0;
        echecsConnexion++;
        if (echecsConnexion >= 5)
            bloqueJusqua = maintenant.plusSeconds(900);
    }
}
