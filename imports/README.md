# Import des consommables du 2 juillet 2026

59 articles et 59 entrées de stock issus des trois photos fournies, importés le 30 septembre 2026 via l’API de l’application.

- Facture ECTT 002/2026 : 51 articles, références internes BUR-001 à BUR-051.
- Facture EZIF 045/2026 : 8 références de cartouches HP.
- Quantités facturées utilisées à la demande de l’utilisateur. Il ne s’agit pas d’un inventaire physique du stock actuel.
- Post-it : 1/2 carton devient 1 demi-carton ; la ligne de facture présente aussi une incohérence de montant (prix unitaire et total tous deux indiqués à 115 000). Aucun montant n’a été corrigé ou intégré au stock.
- Les conditionnements mixtes restent regroupés comme sur les factures. Aucun nombre de pièces par paquet n’a été inventé. Papier : stock géré en cartons de 5 rames.
- Unité des cartouches : unité, à défaut de colonne unité sur cette facture.
- Seuils d’alerte initialisés à 0, faute de seuils fournis. À ajuster depuis Articles et stock.
- Les prix unitaires HT figurent dans le CSV source pour traçabilité ; l’application actuelle n’a pas de gestion des prix.
- Les désignations ont été normalisées pour leur lisibilité ; la marque de la calculatrice n’a pas été reprise car sa lecture est incertaine. La référence HP 2630 est reprise telle qu’elle apparaît sur la photo.
- Les mouvements portent la date réelle d’import ; le motif conserve le numéro et la date de facture. Pas d’antidatage.
- Le script reconnaît les références et les motifs d’import existants pour éviter les doublons lors d’une relance séquentielle. Ne pas lancer deux imports simultanément.

Le fichier resultat-import-2026-07-02.csv contient les identifiants des articles et mouvements ainsi que les stocks contrôlés après import.
