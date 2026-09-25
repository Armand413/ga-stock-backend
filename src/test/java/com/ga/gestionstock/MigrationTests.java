package com.ga.gestionstock;

import java.sql.DriverManager;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import static org.assertj.core.api.Assertions.*;

class MigrationTests {
    @Test
    void migrationConserveLesDonneesDeLaPremiereVersion() throws Exception {
        String url = System.getenv().getOrDefault("TEST_DB_URL", "jdbc:h2:mem:migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        String username = System.getenv().getOrDefault("TEST_DB_USERNAME", "sa");
        String password = System.getenv().getOrDefault("TEST_DB_PASSWORD", "");
        String schema = "migration_test_" + UUID.randomUUID().toString().replace("-", "");
        try (var conn = DriverManager.getConnection(url, username, password); var stmt = conn.createStatement()) {
            String originalSchema = conn.getSchema();
            stmt.execute("CREATE SCHEMA \"" + schema + "\"");
            try {
                conn.setSchema(schema);
                ScriptUtils.executeSqlScript(conn, new ClassPathResource("db/migration/V1__stock_initial.sql"));
                stmt.execute("INSERT INTO articles(reference, nom, unite, quantite, seuil_alerte, version) VALUES ('ANCIEN', 'Papier', 'rame', 3, 5, 0)");
                stmt.execute("INSERT INTO mouvements(article_id, type, quantite, stock_apres, responsable, date) SELECT id, 'ENTREE', 3, 3, 'Ancien responsable', CURRENT_TIMESTAMP FROM articles");
                // Garder la connexion de migration ouverte, comme le fait le pool de l'application.
                var dataSource = new SingleConnectionDataSource(conn, true);
                var ancienneVersion = Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                        .baselineOnMigrate(true).baselineVersion("0").target("2").load();
                assertThat(ancienneVersion.migrate().migrationsExecuted).isEqualTo(2);
                stmt.execute("INSERT INTO utilisateurs(identifiant,nom,mot_de_passe,role) VALUES ('ancien','Ancien compte','hash-conserve','GESTIONNAIRE')");
                stmt.execute("INSERT INTO sessions_acces(empreinte,utilisateur_id,expiration) SELECT 'ancienne-session',id,CURRENT_TIMESTAMP FROM utilisateurs");
                var flyway = Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).load();
                assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
                assertThat(flyway.migrate().migrationsExecuted).isZero();
                try (var rs = stmt.executeQuery("SELECT role, origine, mot_de_passe FROM utilisateurs WHERE identifiant = 'ancien'")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString(1)).isEqualTo("LECTEUR");
                    assertThat(rs.getString(2)).isEqualTo("LOCAL");
                    assertThat(rs.getString(3)).isEqualTo("hash-conserve");
                }
                try (var rs = stmt.executeQuery("SELECT count(*) FROM sessions_acces")) {
                    rs.next(); assertThat(rs.getInt(1)).isZero();
                }
                try (var rs = stmt.executeQuery("SELECT quantite, actif FROM articles WHERE reference = 'ANCIEN'")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getLong(1)).isEqualTo(3);
                    assertThat(rs.getBoolean(2)).isTrue();
                }
                try (var rs = stmt.executeQuery("SELECT responsable, utilisateur_id FROM mouvements")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString(1)).isEqualTo("Ancien responsable");
                    assertThat(rs.getObject(2)).isNull();
                }
                try (var rs = stmt.executeQuery("SELECT count(*) FROM alertes WHERE resolue_le IS NULL")) {
                    rs.next();
                    assertThat(rs.getInt(1)).isEqualTo(1);
                }
            } finally {
                conn.setSchema(originalSchema);
                stmt.execute("DROP SCHEMA \"" + schema + "\" CASCADE");
            }
        }
    }
}
