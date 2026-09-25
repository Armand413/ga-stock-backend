package com.ga.gestionstock.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sessions_acces")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class SessionAcces {
    @Id @Column(length = 64)
    private String empreinte;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;
    @Column(nullable = false)
    private Instant expiration;

    public SessionAcces(String empreinte, Utilisateur utilisateur, Instant expiration) {
        this.empreinte = empreinte;
        this.utilisateur = utilisateur;
        this.expiration = expiration;
    }
}
