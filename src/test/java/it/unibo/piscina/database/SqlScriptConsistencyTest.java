package it.unibo.piscina.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Impedisce la divergenza tra i quattro script SQL ufficiali. */
class SqlScriptConsistencyTest {

    private static final Path SQL_DIRECTORY = Path.of("doc", "sql");

    @Test
    void cartellaContieneQuattroScriptConsolidati() throws IOException {
        try (var files = Files.list(SQL_DIRECTORY)) {
            final List<String> sqlFiles = files
                .map(path -> path.getFileName().toString())
                .filter(name -> name.endsWith(".sql"))
                .sorted()
                .toList();

            assertEquals(List.of(
                "01_schema_completo.sql",
                "02_popolamento_demo.sql",
                "03_migrazione_database_esistente.sql",
                "04_query_operazioni.sql"
            ), sqlFiles);
        }
    }

    @Test
    void schemaContieneEntitaAssociazioniVisteETrigger()
            throws IOException {

        final String schema = read("01_schema_completo.sql").toUpperCase();
        final List<String> tables = List.of(
            "UTENTE",
            "ATLETA",
            "ISTRUTTORE",
            "TIPO_ABBONAMENTO",
            "ABBONAMENTO",
            "TIPO_ATTIVITA",
            "ATTIVITA_PROGRAMMATA",
            "ISCRIZIONE_ATTIVITA",
            "ACCESSO_NUOTO_LIBERO",
            "VASCA",
            "CORSIA",
            "CLUB_SPORTIVO",
            "SQUADRA",
            "APPARTENENZA_SQUADRA",
            "INCARICO_SQUADRA",
            "COMPATIBILITA",
            "UTILIZZA",
            "ASSEGNATO_A",
            "SVOLGE"
        );
        for (String table : tables) {
            assertTrue(
                schema.contains("CREATE TABLE IF NOT EXISTS " + table),
                "Tabella mancante: " + table
            );
        }
        assertTrue(schema.contains("VW_ATTIVITA_COMPLETE"));
        assertTrue(schema.contains("VW_ABBONAMENTI_UTILIZZABILI"));
        assertTrue(schema.contains("TR_ISCRIZIONE_BI"));
        assertTrue(schema.contains("TR_ACCESSO_BI"));
        assertTrue(schema.contains("TR_UTILIZZA_BI"));
        assertTrue(schema.contains("TR_APPARTENENZA_BI"));
        assertTrue(schema.contains(
            "'AMMINISTRATORE', 'CLUB', 'UTENTE'"
        ));
        assertFalse(schema.contains("'RECEPTIONIST'"));
        assertFalse(schema.contains("'REFERENTE_CLUB'"));
    }

    @Test
    void popolamentoCopreTuttiIModuli() throws IOException {
        final String demo = read("02_popolamento_demo.sql").toUpperCase();
        final List<String> inserts = List.of(
            "INSERT IGNORE INTO UTENTE",
            "INSERT INTO TIPO_ATTIVITA",
            "INSERT INTO TIPO_ABBONAMENTO",
            "INSERT INTO VASCA",
            "INSERT INTO CLUB_SPORTIVO",
            "INSERT INTO ATTIVITA_PROGRAMMATA",
            "INSERT INTO ABBONAMENTO",
            "INSERT INTO ISCRIZIONE_ATTIVITA",
            "INSERT INTO ACCESSO_NUOTO_LIBERO",
            "INSERT INTO ACCOUNT"
        );
        for (String insert : inserts) {
            assertTrue(
                demo.contains(insert),
                "Popolamento mancante: " + insert
            );
        }
    }

    @Test
    void popolamentoCreaSoloGliAccountDimostrativiDocumentati()
            throws IOException {

        final String demo = read("02_popolamento_demo.sql");
        final String projectReadme = Files.readString(
            Path.of("README.md"),
            StandardCharsets.UTF_8
        );
        final List<String> demoAccounts = List.of(
            "admin@piscina.local",
            "club@piscina.local",
            "cliente@piscina.local",
            "atleta@piscina.local",
            "istruttore@piscina.local",
            "atleta.istruttore@piscina.local"
        );

        for (String email : demoAccounts) {
            assertTrue(
                demo.contains(email),
                "Account dimostrativo mancante: " + email
            );
            assertTrue(
                projectReadme.contains(email),
                "Account non documentato nel README: " + email
            );
        }
        assertTrue(
            demo.contains("DELETE FROM ACCOUNT"),
            "Il popolamento deve rimuovere il vecchio account receptionist"
        );
        assertFalse(
            projectReadme.contains("reception@piscina.local"),
            "Il README non deve documentare account estranei alla relazione"
        );
        assertTrue(demo.contains("'CLUB'"));
        assertFalse(demo.contains("'REFERENTE_CLUB'"));
    }

    @Test
    void migrazionePreservaIlNucleoPreesistente() throws IOException {
        final String migration = read(
            "03_migrazione_database_esistente.sql"
        ).toUpperCase();

        assertTrue(migration.contains("DATA_REGISTRAZIONE"));
        assertTrue(migration.contains("UPDATE ACCOUNT"));
        assertTrue(migration.contains("REFERENTIAL_CONSTRAINTS"));
        assertTrue(migration.contains("ON DELETE RESTRICT"));
        assertTrue(migration.contains("01_SCHEMA_COMPLETO.SQL"));
    }

    @Test
    void queryDocumentanoTutteLeOperazioni() throws IOException {
        final String operations = read(
            "04_query_operazioni.sql"
        ).toUpperCase();

        for (int operation = 1; operation <= 11; operation++) {
            assertTrue(
                operations.contains("OP" + operation),
                "Operazione non documentata: OP" + operation
            );
        }
        assertTrue(operations.contains("START TRANSACTION"));
        assertTrue(operations.contains("ROLLBACK"));
    }

    @Test
    void readmeDistingueInstallazioneEMigrazione() throws IOException {
        final String readme = Files.readString(
            SQL_DIRECTORY.resolve("README.md"),
            StandardCharsets.UTF_8
        );

        assertTrue(readme.contains("quattro script SQL ufficiali"));
        assertTrue(readme.contains("Nuova installazione"));
        assertTrue(readme.contains("Aggiornamento della prima versione"));
    }

    private String read(final String fileName) throws IOException {
        return Files.readString(
            SQL_DIRECTORY.resolve(fileName),
            StandardCharsets.UTF_8
        );
    }

}
