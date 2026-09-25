package com.ga.gestionstock.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpMailTransport implements MailTransport {
    private final ObjectProvider<JavaMailSender> sender;
    private final String expediteur;
    private final boolean enabled;

    public SmtpMailTransport(ObjectProvider<JavaMailSender> sender,
            @Value("${app.mail.expediteur:}") String expediteur,
            @Value("${app.mail.enabled:false}") boolean enabled) {
        this.sender = sender;
        this.expediteur = expediteur;
        this.enabled = enabled;
    }
    @Override
    public void envoyer(String destinataire, String sujet, String contenu) {
        if (!enabled || !NotificationService.adresseValide(expediteur) || sender.getIfAvailable() == null)
            throw new MailSendException("Configuration SMTP incomplete ou envoi desactive.");
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(expediteur);
        message.setTo(destinataire);
        message.setSubject(sujet);
        message.setText(contenu);
        sender.getObject().send(message);
    }
}
