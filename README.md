# Backend de gestion des consommables GA

Spring Boot 4.1.1, Java 21 et PostgreSQL. Les utilisateurs de l'entreprise se connectent avec leurs identifiants Active Directory Windows Server, demandent des articles et consultent uniquement leurs demandes et leur historique de consommation. L'administrateur gère le stock, traite les demandes et reçoit les notifications par mail.

## Architecture

```text
src/main/java/com/ga/gestionstock
├── config
├── controller
├── dto
├── entity
├── repository
├── service
└── GestionStockApplication.java
```

Les migrations versionnées se trouvent dans `src/main/resources/db/migration`. Hibernate valide le schéma ; Flyway le fait évoluer.

## Parcours utilisateur

1. Connexion à l'application avec le nom de connexion AD ou `utilisateur@domaine` et le mot de passe AD.
2. Consultation du catalogue et création d'une demande pour un article et une quantité.
3. Enregistrement de la demande au statut `EN_ATTENTE`. Le stock n'est pas réservé à cette étape.
4. Mise en file d'un mail pour les adresses de l'administration et d'un accusé de réception pour le demandeur.
5. L'administrateur approuve ou refuse dans l'application, avec une réponse écrite.
6. L'approbation débite la quantité demandée et crée un mouvement rattaché au demandeur. Si le stock est insuffisant, l'opération est refusée et la demande reste en attente. Le refus ne modifie pas le stock.
7. Le demandeur reçoit un mail avec la décision et retrouve les consommables accordés dans son historique.

Une demande en attente peut être annulée par son auteur ou l'administrateur ; un mail informe le demandeur et l'administration. Les réponses directes aux mails ne sont pas traitées automatiquement. L'approbation représente la distribution du consommable ; il n'existe pas de seconde étape de remise physique.

## Rôles et confidentialité

| Fonction | Utilisateur simple | Administrateur |
| --- | --- | --- |
| Catalogue des articles actifs | Oui | Oui |
| Créer une demande et suivre ses demandes | Oui | Oui |
| Consulter son historique de consommations | Oui | Oui |
| Consulter toutes les demandes et les traiter | Non | Oui |
| Gérer le stock, les mouvements, les alertes et les exports | Non | Oui |
| Consulter les utilisateurs et désactiver leur accès applicatif | Non | Oui |
| Suivre et relancer les mails non envoyés | Non | Oui |

Le rôle technique `LECTEUR` désigne désormais l'utilisateur simple, qui peut créer des demandes. `ADMIN` donne la gestion complète. L'ancien rôle `GESTIONNAIRE` n'est plus attribuable ; V3 convertit les comptes existants correspondants en `LECTEUR` et invalide les anciennes sessions.

Les routes personnelles utilisent exclusivement le compte connecté. Aucun identifiant utilisateur envoyé en paramètre ne permet de consulter l'historique d'un autre utilisateur. Une demande appartenant à un autre compte renvoie 404, sauf pour l'administrateur.

## Configuration locale de l'Active Directory

Le mode par défaut est `AUTH_MODE=ad`. Renseigner localement les variables suivantes, dans l'environnement du service ou la configuration de lancement de l'IDE. `.env.example` sert d'inventaire ; il n'est pas chargé automatiquement. Les fichiers `.env` contenant des secrets sont ignorés par Git.

| Variable | Valeur attendue |
| --- | --- |
| `AD_DOMAIN` | Domaine DNS de l'AD, par exemple `entreprise.local` |
| `AD_URL` | URL LDAPS du contrôleur de domaine, par exemple `ldaps://dc01.entreprise.local:636` |
| `AD_BASE_DN` | Racine de recherche, par exemple `DC=entreprise,DC=local` |
| `AD_USER_GROUP_DN` | DN complet du groupe autorisé à utiliser l'application |
| `AD_ADMIN_GROUP_DN` | DN complet d'un groupe distinct réservé aux administrateurs de l'application |
| `AUTH_SESSION_DURATION` | Durée du jeton applicatif, `PT15M` par défaut |

L'authentification utilise un bind LDAPS avec les identifiants saisis. Aucun compte de service LDAP ni mot de passe AD partagé n'est nécessaire. L'AD reste responsable de sa politique de mot de passe et de ses blocages. La règle locale de six caractères ne s'applique pas à l'AD.

Le certificat LDAPS doit être approuvé par la JVM et correspondre au serveur. Installer la chaîne de certification de l'entreprise dans un magasin de confiance Java et le référencer au lancement, sans désactiver les contrôles TLS. Les paramètres AD sont obligatoires en mode AD ; une configuration absente ou non sécurisée bloque le démarrage.

Attributs lus : `objectGUID` pour le lien stable avec le compte applicatif, `userPrincipalName` pour l'identifiant, `displayName` pour le nom, `mail` pour l'adresse de notification et `memberOf` pour les groupes. Une adresse mail valide et l'appartenance à un groupe autorisé sont requises. Le nom est limité à 150 caractères côté application.

Les appartenances **directes** aux groupes configurés sont prises en charge. Les groupes imbriqués et les appartenances via le groupe primaire ne sont pas développés : ajouter directement les utilisateurs aux groupes applicatifs. Un compte appartenant au groupe administrateur reçoit `ADMIN` ; un compte appartenant uniquement au groupe utilisateur reçoit `LECTEUR`. Les autres sont refusés. Les noms génériques de groupes, comme « Domain Admins », ne donnent aucun droit implicitement.

Le compte local de l'application est créé à la première authentification AD réussie, puis son nom, son adresse et son rôle sont synchronisés à chaque connexion. Aucun mot de passe AD ni hachage du mot de passe AD n'est conservé. Un renommage AD conserve le même historique grâce à `objectGUID`. Un compte AD supprimé puis recréé avec un nouvel `objectGUID` n'hérite pas automatiquement de l'ancien compte : les collisions nécessitent une intervention administrative.

Un changement de groupe invalide les anciennes sessions au prochain login. Une désactivation AD est prise en compte à la prochaine authentification ; un jeton déjà délivré reste valable jusqu'à son expiration (15 minutes par défaut). Une désactivation depuis l'application révoque immédiatement ses sessions. Il n'y a pas de connexion locale de secours lorsque l'AD est indisponible.

En mode AD, la création manuelle de comptes, la modification de leur nom/rôle et la modification/réinitialisation de leur mot de passe sont gérées dans Windows Server, pas via l'API. L'administrateur peut activer/désactiver l'accès applicatif. Le premier administrateur doit appartenir au groupe `AD_ADMIN_GROUP_DN` ; aucun administrateur local n'est créé dans ce mode.

## Configuration locale des mails

| Variable | Description |
| --- | --- |
| `NOTIFICATION_ADMIN_EMAILS` | Une ou plusieurs adresses recevant les demandes, séparées par des virgules |
| `MAIL_ENABLED` | `true` pour activer le traitement de la file, `false` par défaut |
| `MAIL_FROM` | Adresse d'expédition autorisée par le serveur SMTP |
| `SMTP_HOST`, `SMTP_PORT` | Serveur SMTP et port, 587 par défaut |
| `SMTP_USERNAME`, `SMTP_PASSWORD` | Identifiants configurés localement si le serveur les exige |
| `SMTP_AUTH` | `true` par défaut ; adapter pour un relais interne sans authentification |
| `SMTP_STARTTLS` | STARTTLS obligatoire si `true`, valeur par défaut |
| `SMTP_SSL` | TLS implicite, `false` par défaut ; pour le port 465, mettre `true` et `SMTP_STARTTLS=false` |

Les paramètres réels de l'entreprise ne sont pas fournis dans le dépôt. Une adresse administrative et un mail utilisateur valides sont nécessaires pour créer une demande. Aucun mot de passe réel ne doit être ajouté au code ni transmis dans un ticket ou dans la conversation.

La demande, la décision, le mouvement et les notifications correspondantes sont enregistrés dans la même transaction. Les mails sont expédiés **après validation** de la transaction par une tâche exécutée toutes les 30 secondes. Chaque destinataire reçoit son propre message.

Si `MAIL_ENABLED=false`, les messages restent en file. Lorsqu'il est activé, un échec SMTP est réessayé avec un délai progressif, jusqu'à huit tentatives. L'administrateur peut consulter `/api/courriels` et remettre un message en attente avec `/api/courriels/{id}/reessayer`. Une panne de messagerie ne supprime pas une demande et ne répète pas une sortie de stock. Le contenu et l'adresse de destination sont ceux enregistrés lors de l'événement.

La livraison est de type « au moins une fois » : si le serveur SMTP accepte un mail mais que l'enregistrement du succès échoue, un doublon de mail reste possible. Cela ne répète pas la décision métier. Un succès signifie l'acceptation par le serveur SMTP, pas la lecture du mail ni l'absence d'un rejet ultérieur du destinataire.

## Démarrage

Prérequis : Java 21, PostgreSQL et une base `ga`. Après configuration des variables AD et SMTP ci-dessus :

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/ga'
$env:DB_USERNAME = 'postgres'
$env:DB_PASSWORD = 'valeur-configuree-localement'
.\mvnw.cmd spring-boot:run
```

`SERVER_PORT` change le port (8080 par défaut). `CORS_ORIGINS` contient les origines autorisées du frontend, séparées par des virgules. Pour l'entreprise, servir l'API derrière HTTPS pour protéger aussi le mot de passe entre le navigateur et l'application.

- Swagger : `http://localhost:8080/swagger-ui/index.html`
- OpenAPI : `http://localhost:8080/v3/api-docs`
- État du service et de la base : `http://localhost:8080/actuator/health`

### Développement sans accès à l'AD

Utiliser explicitement `AUTH_MODE=local`, `ADMIN_USERNAME`, `ADMIN_NAME` et `ADMIN_PASSWORD` pour travailler hors du réseau de l'entreprise. Le premier administrateur local est créé uniquement si la table utilisateurs est vide. Les mots de passe locaux restent limités à 6–64 caractères et 72 octets UTF-8. Créer les utilisateurs de test avec un champ `email` pour pouvoir envoyer des demandes. Ce mode ne doit pas servir de contournement à l'authentification AD en entreprise.

### Reprise d'une base existante

Ne pas modifier les migrations déjà appliquées. V3 ajoute le lien AD, les adresses mail, les demandes, les bénéficiaires identifiés et la file de courriels. Les articles et les anciens mouvements sont conservés.

Pour une base créée par la première version Hibernate et ne possédant pas d'historique Flyway, faire une sauvegarde puis activer `DB_BASELINE=true` pour le premier démarrage migré (baseline 0). Retirer cette option après la reprise. Une base déjà gérée par Flyway n'a pas besoin de cette option.

Les anciens comptes locaux ne sont pas associés automatiquement à une identité AD sur la seule base du nom. Les anciens mouvements dont le bénéficiaire n'était qu'un texte ne sont pas attribués arbitrairement à une personne : ils restent dans l'historique administrateur. Les nouveaux mouvements approuvés ou saisis avec `beneficiaireId` apparaissent dans l'historique personnel.

## API

Connexion : `POST /api/auth/login` avec `{"identifiant":"agent@entreprise.local","motDePasse":"mot-de-passe-AD"}`. La réponse contient `accessToken`, `tokenType`, `expiration` et le profil. Utiliser ensuite `Authorization: Bearer <accessToken>`. Dans Swagger, coller le jeton dans **Authorize**.

| Méthode | Route | Accès / fonction |
| --- | --- | --- |
| GET | `/api/auth/me` | Profil connecté |
| POST | `/api/auth/logout` | Révoquer sa session |
| GET | `/api/catalogue` | Articles actifs pour les demandes |
| POST | `/api/demandes` | Créer sa demande |
| GET | `/api/mes-demandes` | Ses demandes, filtre `statut` facultatif |
| GET | `/api/mes-consommations` | Ses sorties de consommables |
| GET | `/api/demandes/{id}` | Sa demande, ou toute demande pour l'admin |
| PATCH | `/api/demandes/{id}/annulation` | Annuler une demande en attente |
| GET | `/api/demandes` | Admin : toutes les demandes, filtre `statut` |
| PATCH | `/api/demandes/{id}/decision` | Admin : approuver/refuser |
| GET / POST | `/api/articles` | Admin : lister/créer les articles |
| GET / PUT | `/api/articles/{id}` | Admin : consulter/modifier une fiche |
| PATCH | `/api/articles/{id}/etat` | Admin : archiver/réactiver |
| POST | `/api/articles/{id}/mouvements` | Admin : enregistrer une entrée/sortie |
| GET | `/api/mouvements` | Admin : historique global |
| GET | `/api/articles/export` | Admin : export CSV du stock |
| GET | `/api/alertes` | Admin : alertes de stock bas |
| PATCH | `/api/alertes/{id}/acquittement` | Admin : prise en compte d'une alerte |
| GET | `/api/tableau-bord` | Admin : indicateurs |
| GET / PUT | `/api/utilisateurs`, `/api/utilisateurs/{id}` | Admin : consultation et gestion de l'accès |
| GET | `/api/courriels` | Admin : mails non envoyés, y compris échecs épuisés |
| POST | `/api/courriels/{id}/reessayer` | Admin : remettre un mail en attente |

Toutes les listes acceptent `page` à partir de 0 et `size` de 1 à 100 (20 par défaut). Réponse stable : `content`, `page`, `size`, `totalElements`, `totalPages`. Les historiques sont triés du plus récent au plus ancien.

Créer une demande :

```json
{"articleId":1,"quantite":2,"motif":"Besoin du service comptabilité"}
```

Approuver (ou utiliser `REFUSEE`) :

```json
{"statut":"APPROUVEE","reponse":"Deux rames accordées."}
```

Les statuts possibles sont `EN_ATTENTE`, `APPROUVEE`, `REFUSEE`, `ANNULEE`. La décision porte sur toute la quantité demandée. Une décision identique répétée ne recrée ni mouvement ni mail ; une décision contradictoire sur une demande déjà traitée est refusée. La création de demande n'est pas idempotente : après une coupure réseau, vérifier `/api/mes-demandes` avant de la renvoyer.

Créer/modifier un article :

```json
{"reference":"PAP-A4","nom":"Papier A4","unite":"rame","seuilAlerte":5}
```

Entrée : `{"type":"ENTREE","quantite":20,"motif":"Achat"}`.
Sortie manuelle nominative : `{"type":"SORTIE","quantite":2,"beneficiaireId":3,"motif":"Distribution directe"}`.
Le responsable du mouvement est toujours le compte administrateur connecté. `beneficiaire` reste disponible pour une description libre mais ne suffit pas à rattacher un mouvement à un historique personnel.

Le stock initial est zéro et s'alimente par des entrées. Quantités entières strictement positives, seuil positif ou nul, stock jamais négatif. Les écritures sur un article sont verrouillées en base et transactionnelles. Les mouvements sont immuables ; une correction se fait par un mouvement inverse explicite. L'archivage exige un stock nul : `{"actif":false}` ; réactivation : `{"actif":true}`.

Articles/export : filtres `recherche`, `actif`, `stockBas`. Historique global : `articleId`, `type`, `debut`, `fin` en UTC ISO 8601, bornes incluses. Export limité à 10 000 articles avec neutralisation des formules CSV.

Les alertes de stock restent internes à l'application : ouverture au seuil ou en dessous, clôture au réapprovisionnement au-dessus du seuil. Les mails ajoutés dans cette version concernent les demandes et leurs réponses.

Erreurs : `application/problem+json`, avec 400 pour la validation, 401 pour l'authentification, 403 pour les droits, 404 pour une ressource absente ou non visible, 409 pour un conflit métier et 503 pour une dépendance indisponible ou une configuration de notification manquante.

## Vérification et livraison

```powershell
.\mvnw.cmd clean verify
java -jar target/gestion-stock-0.0.1-SNAPSHOT.jar
```

Les tests utilisent H2 avec les migrations réelles. Pour PostgreSQL, créer une base de test dédiée et définir `TEST_DB_URL`, `TEST_DB_USERNAME`, `TEST_DB_PASSWORD` avant de lancer la même commande. Ne jamais lancer les tests sur la base de travail.

Les tests vérifient les droits, l'isolation des demandes et consommations, le stock, les décisions concurrentes, les notifications et leurs reprises, le mapping des attributs/groupes AD et l'absence de mot de passe AD en base. L'annuaire et le transport de mail sont simulés dans les tests ; aucun mail réel n'est envoyé. Le raccordement au contrôleur de domaine et au relais SMTP de l'entreprise doit être validé sur son réseau après configuration locale.

Vérification du 24 septembre 2026 : 31 tests réussis sur H2, puis sur PostgreSQL 18.3, sans échec ni test ignoré. La base temporaire de vérification est distincte de la base de travail `ga`.

Références : [authentification Active Directory Spring Security](https://docs.spring.io/spring-security/reference/7.1/api/java/org/springframework/security/ldap/authentication/ad/ActiveDirectoryLdapAuthenticationProvider.html), [envoi de mail Spring Boot](https://docs.spring.io/spring-boot/reference/io/email.html).

### Comptes de démonstration pour le frontend

Pour saisir le mot de passe PostgreSQL localement, sans l'enregistrer dans le code ou l'historique de commandes, lancer `.\demarrer-test.ps1`. Le script utilise `DB_PASSWORD` si cette variable est déjà définie, sinon il demande le mot de passe avec une saisie masquée. Il s'agit du mot de passe du compte PostgreSQL, pas de celui des comptes de l'application.

Démarrer avec le profil `local-test` :

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local-test'
```

Les paramètres PostgreSQL habituels (`DB_PASSWORD`, etc.) restent nécessaires.
Ce profil crée les comptes manquants : `armand.ga` (utilisateur) et `admin.ga` (administrateur), avec le mot de passe `123456789` pour les deux. Les mots de passe sont hachés en base et ne sont pas réinitialisés à chaque démarrage.
Les adresses `armand@example.test` et `admin@example.test` sont fictives pour tester les demandes. Les notifications sont conservées en attente, sans envoi réel.
Ce profil utilise la connexion locale. Sans ce profil, le fonctionnement AD habituel reste inchangé. Ne pas activer `local-test` en production.

### Connexion locale — phase actuelle

Le mode par défaut est désormais local, avec deux rôles : Utilisateur (LECTEUR dans l’API) et Administrateur (ADMIN). Il ne nécessite aucun serveur AD. Au premier démarrage d’une base vide, définir ADMIN_USERNAME et ADMIN_PASSWORD pour créer le premier administrateur, puis créer les utilisateurs depuis la page Utilisateurs. Les comptes existants sont conservés. Les comptes prédéfinis armand.ga et admin.ga restent disponibles uniquement via le profil local-test explicite. Aucun compte prédéfini n’est ajouté au démarrage normal.

L’intégration AD est conservée inactive pour la prochaine phase. Ne pas définir AUTH_MODE=ad pendant les essais locaux. Le frontend n’a plus de mode démonstration.

### Permissions supplémentaires par utilisateur

Dans Utilisateurs, le bouton Droits ouvre huit cases à cocher pour les comptes non administrateurs : tableau de bord, articles, entrées/sorties de stock, traitement des demandes, historique global, alertes, export CSV et notifications. Le rôle Utilisateur (LECTEUR) reste inchangé. Les administrateurs disposent de toutes les fonctions ; la gestion des comptes et des droits reste réservée au rôle ADMIN.

La modification est enregistrée par PUT /api/utilisateurs/{id}/permissions avec un tableau permissions. Un tableau vide retire toutes les permissions supplémentaires. Les sessions du compte sont révoquées quand les permissions changent : le titulaire doit se reconnecter. Les routes API et les contrôles métier des demandes appliquent les mêmes droits que le frontend.

Les droits Articles, Stock et Export donnent accès à la consultation de la liste des articles et des quantités, mais chaque action reste protégée séparément. Le droit Traiter les demandes inclut consultation, décision et annulation des demandes de tous les utilisateurs ; une approbation entraîne la sortie correspondante sans autoriser les sorties manuelles. La migration V4 ajoute une table de permissions vide, sans modifier le stock ni accorder automatiquement de nouveaux droits.
