package com.ga.gestionstock.service;

public interface MailTransport {
    void envoyer(String destinataire, String sujet, String contenu);
}
