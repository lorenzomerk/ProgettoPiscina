package it.unibo.piscina.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** Definisce le consultazioni aggregate mostrate nei riepiloghi. */
public final class CatalogoRiepiloghi {

    private static final List<String> NOMI = List.of(
        "Attività complete",
        "Club per atleti",
        "Squadre per atleti",
        "Istruttori",
        "Tipi più frequenti",
        "Abbonamenti utilizzabili"
    );

    private CatalogoRiepiloghi() {
    }

    public static List<String> nomi() {
        return NOMI;
    }

    public static String query(
            final int indice,
            final LocalDate inizio,
            final LocalDate fine) {

        Objects.requireNonNull(inizio, "Data iniziale obbligatoria");
        Objects.requireNonNull(fine, "Data finale obbligatoria");
        if (inizio.isAfter(fine)) {
            throw new IllegalArgumentException(
                "La data iniziale non può superare quella finale"
            );
        }
        if (indice < 0 || indice >= NOMI.size()) {
            throw new IllegalArgumentException("Riepilogo non valido");
        }

        return switch (indice) {
            case 0 -> """
                SELECT ID_Attivita_Programmata AS ID, Titolo,
                       Numero_Iscrizioni AS Iscritti,
                       Capienza_Massima AS Capienza
                FROM VW_ATTIVITA_COMPLETE
                ORDER BY Titolo
                """;
            case 1 -> """
                SELECT ID_Club AS ID, Nome,
                       Numero_Atleti AS Atleti_Attivi
                FROM VW_CLUB_ATLETI_ATTIVI
                ORDER BY Numero_Atleti DESC, Nome
                """;
            case 2 -> """
                SELECT ID_Squadra AS ID, Nome, Club,
                       Numero_Atleti AS Atleti_Attivi
                FROM VW_SQUADRA_ATLETI_ATTIVI
                ORDER BY Numero_Atleti DESC, Nome
                """;
            case 3 -> """
                SELECT i.ID_Utente AS ID,
                       CONCAT(u.Cognome, ' ', u.Nome) AS Istruttore,
                       COUNT(ap.ID_Attivita_Programmata) AS Attivita
                FROM ISTRUTTORE i
                JOIN UTENTE u ON u.ID_Utente = i.ID_Utente
                LEFT JOIN ASSEGNATO_A aa
                  ON aa.ID_Utente_Istruttore = i.ID_Utente
                LEFT JOIN ATTIVITA_PROGRAMMATA ap
                  ON ap.ID_Attivita_Programmata =
                     aa.ID_Attivita_Programmata
                 AND ap.Periodo_Inizio <= DATE '%s'
                 AND ap.Periodo_Fine >= DATE '%s'
                GROUP BY i.ID_Utente, u.Cognome, u.Nome
                ORDER BY Attivita DESC, u.Cognome, u.Nome
                """.formatted(fine, inizio);
            case 4 -> """
                SELECT 'ATTIVITA' AS Ambito, ta.Nome,
                       COUNT(i.ID_Iscrizione_Attivita) AS Frequenza
                FROM TIPO_ATTIVITA ta
                LEFT JOIN ATTIVITA_PROGRAMMATA ap
                  ON ap.ID_Tipo_Attivita = ta.ID_Tipo_Attivita
                LEFT JOIN ISCRIZIONE_ATTIVITA i
                  ON i.ID_Attivita_Programmata =
                     ap.ID_Attivita_Programmata
                 AND i.Data_Iscrizione BETWEEN DATE '%s' AND DATE '%s'
                WHERE ta.Modalita_Partecipazione = 'ISCRIZIONE'
                GROUP BY ta.ID_Tipo_Attivita, ta.Nome
                UNION ALL
                SELECT 'ABBONAMENTO' AS Ambito, t.Nome,
                       COUNT(a.ID_Abbonamento) AS Frequenza
                FROM TIPO_ABBONAMENTO t
                LEFT JOIN ABBONAMENTO a
                  ON a.ID_Tipo_Abbonamento = t.ID_Tipo_Abbonamento
                 AND a.Data_Acquisto BETWEEN DATE '%s' AND DATE '%s'
                GROUP BY t.ID_Tipo_Abbonamento, t.Nome
                ORDER BY Ambito, Frequenza DESC, Nome
                """.formatted(inizio, fine, inizio, fine);
            case 5 -> """
                SELECT COUNT(*) AS Abbonamenti_Attualmente_Utilizzabili
                FROM VW_ABBONAMENTI_UTILIZZABILI
                """;
            default -> throw new IllegalStateException(
                "Indice di riepilogo non gestito"
            );
        };
    }
}
