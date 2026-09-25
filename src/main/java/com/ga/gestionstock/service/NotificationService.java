package com.ga.gestionstock.service;

import com.ga.gestionstock.entity.*;
import com.ga.gestionstock.repository.CourrielRepository;
import jakarta.mail.internet.InternetAddress;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {
    private final CourrielRepository courriels;
    private final String administrateurs;

    public NotificationService(CourrielRepository courriels,
            @Value("${app.notifications.administrateurs:}") String administrateurs) {
        this.courriels = courriels;
        this.administrateurs = administrateurs;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void notifier(Demande demande, boolean creation) {
        Set<String> destinataires = new LinkedHashSet<>();
        if (creation || demande.getStatut() == StatutDemande.ANNULEE) {
            for (String email : administrateurs.split(",")) if (!email.isBlank()) destinataires.add(email.trim());
            if (destinataires.isEmpty()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Configurer NOTIFICATION_ADMIN_EMAILS avant de soumettre des demandes.");
        }
        destinataires.add(demande.getDemandeur().getEmail());
        for (String email : destinataires) if (!adresseValide(email))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une adresse mail de notification est absente ou invalide.");
        String sujet = "[Consommables GA] Demande #" + demande.getId() + " - " + demande.getStatut();
        String contenu = "Demande #" + demande.getId() + "\nDemandeur : " + demande.getDemandeur().getNom()
                + "\nArticle : " + demande.getArticle().getNom() + "\nQuantite : " + demande.getQuantite()
                + "\nStatut : " + demande.getStatut()
                + "\nMotif : " + Objects.toString(demande.getMotif(), "")
                + "\nReponse : " + Objects.toString(demande.getReponse(), "")
                + "\n\nConsultez l'application pour suivre ou traiter cette demande. Les reponses a ce mail ne sont pas traitees automatiquement.";
        for (String email : destinataires) courriels.save(new Courriel(demande, email, sujet, contenu));
    }

    public static boolean adresseValide(String email) {
        try {
            if (email == null || email.isBlank() || email.length() > 254 || email.contains("\r") || email.contains("\n")) return false;
            InternetAddress address = new InternetAddress(email, true);
            address.validate();
            return address.getAddress().equals(email);
        } catch (Exception ex) { return false; }
    }
}
