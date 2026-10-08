CREATE TABLE utilisateur_permissions (
 utilisateur_id BIGINT NOT NULL REFERENCES utilisateurs(id),
 permission VARCHAR(40) NOT NULL,
 PRIMARY KEY (utilisateur_id, permission),
 CONSTRAINT permission_valide CHECK (permission IN ('VOIR_TABLEAU_BORD','GERER_ARTICLES','GERER_STOCK','TRAITER_DEMANDES','VOIR_MOUVEMENTS','GERER_ALERTES','EXPORTER_STOCK','GERER_NOTIFICATIONS'))
);
