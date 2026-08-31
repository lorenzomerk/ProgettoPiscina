package it.unibo.piscina.view.gestione;

import static it.unibo.piscina.model.TipoCampo.BOOLEANO;
import static it.unibo.piscina.model.TipoCampo.DATA;
import static it.unibo.piscina.model.TipoCampo.DATA_ORA;
import static it.unibo.piscina.model.TipoCampo.DECIMALE;
import static it.unibo.piscina.model.TipoCampo.INTERO;
import static it.unibo.piscina.model.TipoCampo.ORA;
import static it.unibo.piscina.model.TipoCampo.TESTO;
import static it.unibo.piscina.model.TipoCampo.TESTO_LUNGO;

import it.unibo.piscina.model.CampoEntita;
import it.unibo.piscina.model.DefinizioneEntita;
import it.unibo.piscina.model.RuoloApplicativo;
import it.unibo.piscina.model.SessioneUtente;
import java.util.ArrayList;
import java.util.List;

/** Catalogo dei moduli ricavato dalle entità e associazioni della relazione. */
public final class DefinizioniDominio {

    private static final String LOOKUP_UTENTI = """
        SELECT ID_Utente,
               CONCAT(Cognome, ' ', Nome, ' (#', ID_Utente, ')')
        FROM UTENTE
        WHERE Attivo = TRUE
        ORDER BY Cognome, Nome
        """;
    private static final String LOOKUP_ATLETI = """
        SELECT a.ID_Utente,
               CONCAT(u.Cognome, ' ', u.Nome, ' (#', a.ID_Utente, ')')
        FROM ATLETA a
        JOIN UTENTE u ON u.ID_Utente = a.ID_Utente
        ORDER BY u.Cognome, u.Nome
        """;
    private static final String LOOKUP_ISTRUTTORI = """
        SELECT i.ID_Utente,
               CONCAT(u.Cognome, ' ', u.Nome, ' - ', i.Qualifica)
        FROM ISTRUTTORE i
        JOIN UTENTE u ON u.ID_Utente = i.ID_Utente
        ORDER BY u.Cognome, u.Nome
        """;
    private static final String LOOKUP_TIPI_ABBONAMENTO = """
        SELECT ID_Tipo_Abbonamento,
               CONCAT(Nome, ' - ', Modalita_Validita)
        FROM TIPO_ABBONAMENTO
        ORDER BY Nome
        """;
    private static final String LOOKUP_TIPI_ABBONAMENTO_ATTIVI = """
        SELECT ID_Tipo_Abbonamento,
               CONCAT(Nome, ' - ', Modalita_Validita)
        FROM TIPO_ABBONAMENTO
        WHERE Attivo = TRUE
        ORDER BY Nome
        """;
    private static final String LOOKUP_TIPI_ATTIVITA = """
        SELECT ID_Tipo_Attivita,
               CONCAT(Nome, ' - ', Modalita_Partecipazione)
        FROM TIPO_ATTIVITA
        ORDER BY Nome
        """;
    private static final String LOOKUP_ATTIVITA = """
        SELECT ID_Attivita_Programmata,
               CONCAT(Titolo, ' (#', ID_Attivita_Programmata, ')')
        FROM ATTIVITA_PROGRAMMATA
        ORDER BY Periodo_Inizio DESC, Titolo
        """;
    private static final String LOOKUP_ATTIVITA_ISCRIZIONE = """
        SELECT ap.ID_Attivita_Programmata,
               CONCAT(ap.Titolo, ' (#',
                      ap.ID_Attivita_Programmata, ')')
        FROM ATTIVITA_PROGRAMMATA ap
        JOIN TIPO_ATTIVITA ta
          ON ta.ID_Tipo_Attivita = ap.ID_Tipo_Attivita
        WHERE ta.Modalita_Partecipazione = 'ISCRIZIONE'
          AND ap.Stato NOT IN ('CONCLUSA', 'ANNULLATA')
        ORDER BY ap.Periodo_Inizio DESC, ap.Titolo
        """;
    private static final String LOOKUP_ATTIVITA_ACCESSO = """
        SELECT ap.ID_Attivita_Programmata,
               CONCAT(ap.Titolo, ' (#',
                      ap.ID_Attivita_Programmata, ')')
        FROM ATTIVITA_PROGRAMMATA ap
        JOIN TIPO_ATTIVITA ta
          ON ta.ID_Tipo_Attivita = ap.ID_Tipo_Attivita
        WHERE ta.Modalita_Partecipazione = 'ACCESSO_LIBERO'
          AND ap.Stato = 'ATTIVA'
        ORDER BY ap.Titolo
        """;
    private static final String LOOKUP_ATTIVITA_SQUADRA = """
        SELECT ap.ID_Attivita_Programmata,
               CONCAT(ap.Titolo, ' (#',
                      ap.ID_Attivita_Programmata, ')')
        FROM ATTIVITA_PROGRAMMATA ap
        JOIN TIPO_ATTIVITA ta
          ON ta.ID_Tipo_Attivita = ap.ID_Tipo_Attivita
        WHERE ta.Modalita_Partecipazione = 'SQUADRA'
        ORDER BY ap.Periodo_Inizio DESC, ap.Titolo
        """;
    private static final String LOOKUP_ABBONAMENTI = """
        SELECT a.ID_Abbonamento,
               CONCAT(u.Cognome, ' ', u.Nome, ' - ', t.Nome,
                      ' (#', a.ID_Abbonamento, ')')
        FROM ABBONAMENTO a
        JOIN UTENTE u ON u.ID_Utente = a.ID_Utente
        JOIN TIPO_ABBONAMENTO t
          ON t.ID_Tipo_Abbonamento = a.ID_Tipo_Abbonamento
        ORDER BY a.Data_Acquisto DESC
        """;
    private static final String LOOKUP_VASCHE = """
        SELECT ID_Vasca, CONCAT(Nome, ' (#', ID_Vasca, ')')
        FROM VASCA ORDER BY Nome
        """;
    private static final String LOOKUP_SQUADRE = """
        SELECT s.ID_Squadra,
               CONCAT(c.Nome, ' / ', s.Nome, ' (#', s.ID_Squadra, ')')
        FROM SQUADRA s
        JOIN CLUB_SPORTIVO c ON c.ID_Club = s.ID_Club
        ORDER BY c.Nome, s.Nome
        """;
    private static final String LOOKUP_CLUB = """
        SELECT ID_Club, CONCAT(Nome, ' (#', ID_Club, ')')
        FROM CLUB_SPORTIVO ORDER BY Nome
        """;

    private DefinizioniDominio() {
    }

    public static List<DefinizioneEntita> perSezione(
            final String section,
            final SessioneUtente session) {

        if ("La mia squadra".equals(section)) {
            return miaSquadra(session);
        }
        if ("Attività assegnate".equals(section)) {
            return attivitaAssegnate(session);
        }
        if ("Squadre seguite".equals(section)) {
            return squadreSeguite(session);
        }
        if (session.ruolo() == RuoloApplicativo.UTENTE) {
            return vistaUtente(section, session);
        }
        return switch (section) {
            case "Abbonamenti" -> abbonamenti();
            case "Attività" -> attivita();
            case "Accessi" -> accessi();
            case "Struttura" -> struttura();
            case "Club e squadre" -> clubESquadre();
            default -> List.of();
        };
    }

    private static List<DefinizioneEntita> abbonamenti() {
        return List.of(
            new DefinizioneEntita(
                "Tipi di abbonamento",
                "Offerta commerciale, validità e disponibilità",
                "TIPO_ABBONAMENTO",
                """
                SELECT ID_Tipo_Abbonamento AS ID, Nome, Costo, Attivo,
                       Modalita_Validita AS Modalita,
                       Condizioni_Utilizzo AS Condizioni,
                       Durata_Giorni AS Durata,
                       Numero_Ingressi AS Ingressi
                FROM TIPO_ABBONAMENTO ORDER BY Nome
                """,
                List.of(
                    CampoEntita.id("ID", "ID_Tipo_Abbonamento"),
                    CampoEntita.campo("Nome", "Nome", TESTO, true),
                    CampoEntita.campo("Costo", "Costo", DECIMALE, true),
                    CampoEntita.campo("Attivo", "Attivo", BOOLEANO, true),
                    CampoEntita.elenco(
                        "Modalità", "Modalita_Validita",
                        "TEMPO", "INGRESSI"
                    ),
                    CampoEntita.campo(
                        "Condizioni", "Condizioni_Utilizzo",
                        TESTO_LUNGO, true
                    ),
                    CampoEntita.campo(
                        "Durata (giorni)", "Durata_Giorni",
                        INTERO, false
                    ),
                    CampoEntita.campo(
                        "Numero ingressi", "Numero_Ingressi",
                        INTERO, false
                    )
                ),
                true, true, false
            ),
            new DefinizioneEntita(
                "Compatibilità",
                "Tipi di attività consentiti da ciascun abbonamento",
                "COMPATIBILITA",
                """
                SELECT c.ID_Tipo_Abbonamento AS Abbonamento_ID,
                       c.ID_Tipo_Attivita AS Attivita_ID,
                       ta.Nome AS Abbonamento,
                       tt.Nome AS Attivita
                FROM COMPATIBILITA c
                JOIN TIPO_ABBONAMENTO ta
                  ON ta.ID_Tipo_Abbonamento = c.ID_Tipo_Abbonamento
                JOIN TIPO_ATTIVITA tt
                  ON tt.ID_Tipo_Attivita = c.ID_Tipo_Attivita
                ORDER BY ta.Nome, tt.Nome
                """,
                List.of(
                    CampoEntita.chiave(
                        "Tipo abbonamento",
                        "ID_Tipo_Abbonamento",
                        LOOKUP_TIPI_ABBONAMENTO
                    ),
                    CampoEntita.chiave(
                        "Tipo attività",
                        "ID_Tipo_Attivita",
                        LOOKUP_TIPI_ATTIVITA
                    ),
                    CampoEntita.solaLettura(
                        "Abbonamento", "Abbonamento"
                    ),
                    CampoEntita.solaLettura("Attività", "Attivita")
                ),
                true, false, true
            ),
            definizioneAbbonamentiAcquistati(true)
        );
    }

    private static DefinizioneEntita definizioneAbbonamentiAcquistati(
            final boolean writable) {

        return new DefinizioneEntita(
            "Abbonamenti acquistati",
            "Titoli individuali, validità e ingressi residui",
            "ABBONAMENTO",
            """
            SELECT a.ID_Abbonamento AS ID,
                   a.ID_Utente AS Utente_ID,
                   a.ID_Tipo_Abbonamento AS Tipo_ID,
                   a.Data_Acquisto AS Acquisto,
                   a.Data_Inizio AS Inizio,
                   a.Data_Fine AS Fine,
                   a.Stato,
                   a.Ingressi_Rimanenti AS Ingressi,
                   CONCAT(u.Cognome, ' ', u.Nome) AS Utente,
                   t.Nome AS Tipo
            FROM ABBONAMENTO a
            JOIN UTENTE u ON u.ID_Utente = a.ID_Utente
            JOIN TIPO_ABBONAMENTO t
              ON t.ID_Tipo_Abbonamento = a.ID_Tipo_Abbonamento
            ORDER BY a.Data_Acquisto DESC, a.ID_Abbonamento DESC
            """,
            List.of(
                CampoEntita.id("ID", "ID_Abbonamento"),
                CampoEntita.riferimento(
                    "Utente", "ID_Utente", LOOKUP_UTENTI
                ),
                CampoEntita.riferimento(
                    "Tipo abbonamento",
                    "ID_Tipo_Abbonamento",
                    LOOKUP_TIPI_ABBONAMENTO_ATTIVI
                ),
                CampoEntita.campo(
                    "Data acquisto", "Data_Acquisto", DATA, true
                ),
                CampoEntita.campo(
                    "Data inizio", "Data_Inizio", DATA, true
                ),
                CampoEntita.solaLettura("Data fine", "Data_Fine"),
                CampoEntita.solaLettura("Stato", "Stato"),
                CampoEntita.solaLettura(
                    "Ingressi rimanenti", "Ingressi_Rimanenti"
                ),
                CampoEntita.solaLettura("Utente", "Utente"),
                CampoEntita.solaLettura("Tipo", "Tipo")
            ),
            writable, false, false
        );
    }

    private static List<DefinizioneEntita> attivita() {
        return List.of(
            new DefinizioneEntita(
                "Tipi di attività",
                "Categorie e modalità di partecipazione",
                "TIPO_ATTIVITA",
                """
                SELECT ID_Tipo_Attivita AS ID, Nome,
                       Modalita_Partecipazione AS Modalita,
                       Richiede_Istruttore AS Istruttore,
                       Descrizione
                FROM TIPO_ATTIVITA ORDER BY Nome
                """,
                List.of(
                    CampoEntita.id("ID", "ID_Tipo_Attivita"),
                    CampoEntita.campo("Nome", "Nome", TESTO, true),
                    CampoEntita.elenco(
                        "Modalità", "Modalita_Partecipazione",
                        "ISCRIZIONE", "ACCESSO_LIBERO", "SQUADRA"
                    ),
                    CampoEntita.campo(
                        "Richiede istruttore",
                        "Richiede_Istruttore", BOOLEANO, true
                    ),
                    CampoEntita.campo(
                        "Descrizione", "Descrizione",
                        TESTO_LUNGO, false
                    )
                ),
                true, true, true
            ),
            definizioneAttivitaProgrammate(true),
            new DefinizioneEntita(
                "Istruttori assegnati",
                "Assegnazioni senza sovrapposizioni di calendario",
                "ASSEGNATO_A",
                """
                SELECT aa.ID_Utente_Istruttore AS Istruttore_ID,
                       aa.ID_Attivita_Programmata AS Attivita_ID,
                       CONCAT(u.Cognome, ' ', u.Nome) AS Istruttore,
                       ap.Titolo AS Attivita
                FROM ASSEGNATO_A aa
                JOIN UTENTE u
                  ON u.ID_Utente = aa.ID_Utente_Istruttore
                JOIN ATTIVITA_PROGRAMMATA ap
                  ON ap.ID_Attivita_Programmata =
                     aa.ID_Attivita_Programmata
                ORDER BY ap.Periodo_Inizio DESC, ap.Titolo
                """,
                List.of(
                    CampoEntita.chiave(
                        "Istruttore", "ID_Utente_Istruttore",
                        LOOKUP_ISTRUTTORI
                    ),
                    CampoEntita.chiave(
                        "Attività", "ID_Attivita_Programmata",
                        LOOKUP_ATTIVITA
                    ),
                    CampoEntita.solaLettura(
                        "Istruttore", "Istruttore"
                    ),
                    CampoEntita.solaLettura("Attività", "Attivita")
                ),
                true, false, true
            ),
            definizioneIscrizioni(true)
        );
    }

    private static DefinizioneEntita definizioneAttivitaProgrammate(
            final boolean writable) {

        return new DefinizioneEntita(
            "Attività programmate",
            "Calendario, capienza e stato operativo",
            "ATTIVITA_PROGRAMMATA",
            """
            SELECT ap.ID_Attivita_Programmata AS ID,
                   ap.ID_Tipo_Attivita AS Tipo_ID,
                   ap.Titolo,
                   ap.Giorno_Settimanale AS Giorno,
                   ap.Ora_Inizio AS Ora_Inizio,
                   ap.Ora_Fine AS Ora_Fine,
                   ap.Capienza_Massima AS Capienza,
                   ap.Periodo_Inizio AS Periodo_Inizio,
                   ap.Periodo_Fine AS Periodo_Fine,
                   ap.Stato,
                   ta.Nome AS Tipo,
                   ta.Modalita_Partecipazione AS Modalita
            FROM ATTIVITA_PROGRAMMATA ap
            JOIN TIPO_ATTIVITA ta
              ON ta.ID_Tipo_Attivita = ap.ID_Tipo_Attivita
            ORDER BY ap.Periodo_Inizio DESC, ap.Giorno_Settimanale,
                     ap.Ora_Inizio
            """,
            List.of(
                CampoEntita.id("ID", "ID_Attivita_Programmata"),
                CampoEntita.riferimento(
                    "Tipo attività", "ID_Tipo_Attivita",
                    LOOKUP_TIPI_ATTIVITA
                ),
                CampoEntita.campo("Titolo", "Titolo", TESTO, true),
                CampoEntita.elenco(
                    "Giorno", "Giorno_Settimanale",
                    "LUNEDI", "MARTEDI", "MERCOLEDI", "GIOVEDI",
                    "VENERDI", "SABATO", "DOMENICA"
                ),
                CampoEntita.campo(
                    "Ora inizio", "Ora_Inizio", ORA, true
                ),
                CampoEntita.campo(
                    "Ora fine", "Ora_Fine", ORA, true
                ),
                CampoEntita.campo(
                    "Capienza massima",
                    "Capienza_Massima", INTERO, true
                ),
                CampoEntita.campo(
                    "Periodo inizio", "Periodo_Inizio", DATA, true
                ),
                CampoEntita.campo(
                    "Periodo fine", "Periodo_Fine", DATA, true
                ),
                CampoEntita.elenco(
                    "Stato", "Stato",
                    "PROGRAMMATA", "ATTIVA", "CONCLUSA", "ANNULLATA"
                ),
                CampoEntita.solaLettura("Tipo", "Tipo"),
                CampoEntita.solaLettura("Modalità", "Modalita")
            ),
            writable, writable, writable
        );
    }

    private static DefinizioneEntita definizioneIscrizioni(
            final boolean writable) {

        return new DefinizioneEntita(
            "Iscrizioni",
            "Partecipazioni individuali con controllo di capienza",
            "ISCRIZIONE_ATTIVITA",
            """
            SELECT i.ID_Iscrizione_Attivita AS ID,
                   i.ID_Abbonamento AS Abbonamento_ID,
                   i.ID_Attivita_Programmata AS Attivita_ID,
                   i.Data_Iscrizione AS Data_Iscrizione,
                   CONCAT(u.Cognome, ' ', u.Nome) AS Utente,
                   ap.Titolo AS Attivita
            FROM ISCRIZIONE_ATTIVITA i
            JOIN ABBONAMENTO a
              ON a.ID_Abbonamento = i.ID_Abbonamento
            JOIN UTENTE u ON u.ID_Utente = a.ID_Utente
            JOIN ATTIVITA_PROGRAMMATA ap
              ON ap.ID_Attivita_Programmata =
                 i.ID_Attivita_Programmata
            ORDER BY i.Data_Iscrizione DESC
            """,
            List.of(
                CampoEntita.id("ID", "ID_Iscrizione_Attivita"),
                CampoEntita.riferimento(
                    "Abbonamento", "ID_Abbonamento",
                    LOOKUP_ABBONAMENTI
                ),
                CampoEntita.riferimento(
                    "Attività", "ID_Attivita_Programmata",
                    LOOKUP_ATTIVITA_ISCRIZIONE
                ),
                CampoEntita.campo(
                    "Data iscrizione", "Data_Iscrizione", DATA, true
                ),
                CampoEntita.solaLettura("Utente", "Utente"),
                CampoEntita.solaLettura("Attività", "Attivita")
            ),
            writable, false, false
        );
    }

    private static List<DefinizioneEntita> accessi() {
        return List.of(new DefinizioneEntita(
            "Accessi al nuoto libero",
            "Ingressi effettivi; gli abbonamenti a ingressi sono decrementati",
            "ACCESSO_NUOTO_LIBERO",
            """
            SELECT x.ID_Accesso_Nuoto_Libero AS ID,
                   x.ID_Abbonamento AS Abbonamento_ID,
                   x.ID_Attivita_Programmata AS Attivita_ID,
                   x.Data_Ora_Accesso AS Data_Ora,
                   CONCAT(u.Cognome, ' ', u.Nome) AS Utente,
                   ap.Titolo AS Fascia
            FROM ACCESSO_NUOTO_LIBERO x
            JOIN ABBONAMENTO a
              ON a.ID_Abbonamento = x.ID_Abbonamento
            JOIN UTENTE u ON u.ID_Utente = a.ID_Utente
            JOIN ATTIVITA_PROGRAMMATA ap
              ON ap.ID_Attivita_Programmata =
                 x.ID_Attivita_Programmata
            ORDER BY x.Data_Ora_Accesso DESC
            """,
            List.of(
                CampoEntita.id("ID", "ID_Accesso_Nuoto_Libero"),
                CampoEntita.riferimento(
                    "Abbonamento", "ID_Abbonamento",
                    LOOKUP_ABBONAMENTI
                ),
                CampoEntita.riferimento(
                    "Fascia di nuoto libero",
                    "ID_Attivita_Programmata",
                    LOOKUP_ATTIVITA_ACCESSO
                ),
                CampoEntita.campo(
                    "Data e ora", "Data_Ora_Accesso", DATA_ORA, true
                ),
                CampoEntita.solaLettura("Utente", "Utente"),
                CampoEntita.solaLettura("Fascia", "Fascia")
            ),
            true, false, false
        ));
    }

    private static List<DefinizioneEntita> struttura() {
        return List.of(
            new DefinizioneEntita(
                "Vasche",
                "Spazi fisici principali della piscina",
                "VASCA",
                """
                SELECT ID_Vasca AS ID, Nome, Lunghezza, Larghezza,
                       Profondita, Temperatura, Tipologia
                FROM VASCA ORDER BY Nome
                """,
                List.of(
                    CampoEntita.id("ID", "ID_Vasca"),
                    CampoEntita.campo("Nome", "Nome", TESTO, true),
                    CampoEntita.campo(
                        "Lunghezza (m)", "Lunghezza", DECIMALE, true
                    ),
                    CampoEntita.campo(
                        "Larghezza (m)", "Larghezza", DECIMALE, true
                    ),
                    CampoEntita.campo(
                        "Profondità (m)", "Profondita", DECIMALE, true
                    ),
                    CampoEntita.campo(
                        "Temperatura (°C)", "Temperatura", DECIMALE, true
                    ),
                    CampoEntita.campo(
                        "Tipologia", "Tipologia", TESTO, true
                    )
                ),
                true, true, true
            ),
            new DefinizioneEntita(
                "Corsie",
                "Corsie fisiche o corsia convenzionale dell'intera vasca",
                "CORSIA",
                """
                SELECT c.ID_Vasca AS Vasca_ID, c.Numero,
                       v.Nome AS Vasca
                FROM CORSIA c
                JOIN VASCA v ON v.ID_Vasca = c.ID_Vasca
                ORDER BY v.Nome, c.Numero
                """,
                List.of(
                    CampoEntita.chiave(
                        "Vasca", "ID_Vasca", LOOKUP_VASCHE
                    ),
                    new CampoEntita(
                        "Numero", "Numero", INTERO,
                        true, true, false, List.of(), null
                    ),
                    CampoEntita.solaLettura("Vasca", "Vasca")
                ),
                true, false, true
            ),
            new DefinizioneEntita(
                "Utilizzo delle corsie",
                "Assegnazioni con controllo automatico delle sovrapposizioni",
                "UTILIZZA",
                """
                SELECT u.ID_Attivita_Programmata AS Attivita_ID,
                       u.ID_Vasca AS Vasca_ID,
                       u.Numero_Corsia AS Corsia,
                       ap.Titolo AS Attivita,
                       v.Nome AS Vasca
                FROM UTILIZZA u
                JOIN ATTIVITA_PROGRAMMATA ap
                  ON ap.ID_Attivita_Programmata =
                     u.ID_Attivita_Programmata
                JOIN VASCA v ON v.ID_Vasca = u.ID_Vasca
                ORDER BY ap.Periodo_Inizio DESC, ap.Titolo
                """,
                List.of(
                    CampoEntita.chiave(
                        "Attività", "ID_Attivita_Programmata",
                        LOOKUP_ATTIVITA
                    ),
                    CampoEntita.chiave(
                        "Vasca", "ID_Vasca", LOOKUP_VASCHE
                    ),
                    new CampoEntita(
                        "Numero corsia", "Numero_Corsia", INTERO,
                        true, true, false, List.of(), null
                    ),
                    CampoEntita.solaLettura("Attività", "Attivita"),
                    CampoEntita.solaLettura("Vasca", "Vasca")
                ),
                true, false, true
            )
        );
    }

    private static List<DefinizioneEntita> clubESquadre() {
        return List.of(
            new DefinizioneEntita(
                "Club sportivi",
                "Club affiliati alla struttura",
                "CLUB_SPORTIVO",
                """
                SELECT ID_Club AS ID, Nome, Email, Telefono, Indirizzo
                FROM CLUB_SPORTIVO ORDER BY Nome
                """,
                List.of(
                    CampoEntita.id("ID", "ID_Club"),
                    CampoEntita.campo("Nome", "Nome", TESTO, true),
                    CampoEntita.campo("Email", "Email", TESTO, false),
                    CampoEntita.campo(
                        "Telefono", "Telefono", TESTO, false
                    ),
                    CampoEntita.campo(
                        "Indirizzo", "Indirizzo", TESTO, false
                    )
                ),
                true, true, true
            ),
            new DefinizioneEntita(
                "Squadre",
                "Squadre appartenenti a un unico club",
                "SQUADRA",
                """
                SELECT s.ID_Squadra AS ID, s.ID_Club AS Club_ID,
                       s.Nome, s.Categoria, c.Nome AS Club
                FROM SQUADRA s
                JOIN CLUB_SPORTIVO c ON c.ID_Club = s.ID_Club
                ORDER BY c.Nome, s.Nome
                """,
                List.of(
                    CampoEntita.id("ID", "ID_Squadra"),
                    CampoEntita.riferimento(
                        "Club", "ID_Club", LOOKUP_CLUB
                    ),
                    CampoEntita.campo("Nome", "Nome", TESTO, true),
                    CampoEntita.campo(
                        "Categoria", "Categoria", TESTO, true
                    ),
                    CampoEntita.solaLettura("Club", "Club")
                ),
                true, true, true
            ),
            new DefinizioneEntita(
                "Appartenenze degli atleti",
                "Storico non sovrapposto delle squadre degli atleti",
                "APPARTENENZA_SQUADRA",
                """
                SELECT a.ID_Appartenenza AS ID,
                       a.ID_Utente_Atleta AS Atleta_ID,
                       a.ID_Squadra AS Squadra_ID,
                       a.Data_Inizio AS Inizio,
                       a.Data_Fine AS Fine,
                       CONCAT(u.Cognome, ' ', u.Nome) AS Atleta,
                       s.Nome AS Squadra
                FROM APPARTENENZA_SQUADRA a
                JOIN UTENTE u
                  ON u.ID_Utente = a.ID_Utente_Atleta
                JOIN SQUADRA s ON s.ID_Squadra = a.ID_Squadra
                ORDER BY a.Data_Inizio DESC
                """,
                List.of(
                    CampoEntita.id("ID", "ID_Appartenenza"),
                    CampoEntita.riferimento(
                        "Atleta", "ID_Utente_Atleta", LOOKUP_ATLETI
                    ),
                    CampoEntita.riferimento(
                        "Squadra", "ID_Squadra", LOOKUP_SQUADRE
                    ),
                    CampoEntita.campo(
                        "Data inizio", "Data_Inizio", DATA, true
                    ),
                    CampoEntita.campo(
                        "Data fine", "Data_Fine", DATA, false
                    ),
                    CampoEntita.solaLettura("Atleta", "Atleta"),
                    CampoEntita.solaLettura("Squadra", "Squadra")
                ),
                true, true, false
            ),
            new DefinizioneEntita(
                "Incarichi degli istruttori",
                "Storico degli incarichi presso le squadre",
                "INCARICO_SQUADRA",
                """
                SELECT i.ID_Incarico_Squadra AS ID,
                       i.ID_Utente_Istruttore AS Istruttore_ID,
                       i.ID_Squadra AS Squadra_ID,
                       i.Data_Inizio AS Inizio,
                       i.Data_Fine AS Fine,
                       CONCAT(u.Cognome, ' ', u.Nome) AS Istruttore,
                       s.Nome AS Squadra
                FROM INCARICO_SQUADRA i
                JOIN UTENTE u
                  ON u.ID_Utente = i.ID_Utente_Istruttore
                JOIN SQUADRA s ON s.ID_Squadra = i.ID_Squadra
                ORDER BY i.Data_Inizio DESC
                """,
                List.of(
                    CampoEntita.id("ID", "ID_Incarico_Squadra"),
                    CampoEntita.riferimento(
                        "Istruttore", "ID_Utente_Istruttore",
                        LOOKUP_ISTRUTTORI
                    ),
                    CampoEntita.riferimento(
                        "Squadra", "ID_Squadra", LOOKUP_SQUADRE
                    ),
                    CampoEntita.campo(
                        "Data inizio", "Data_Inizio", DATA, true
                    ),
                    CampoEntita.campo(
                        "Data fine", "Data_Fine", DATA, false
                    ),
                    CampoEntita.solaLettura(
                        "Istruttore", "Istruttore"
                    ),
                    CampoEntita.solaLettura("Squadra", "Squadra")
                ),
                true, true, false
            ),
            new DefinizioneEntita(
                "Attività svolte dalle squadre",
                "Partecipazione, anche congiunta, ad attività di squadra",
                "SVOLGE",
                """
                SELECT x.ID_Squadra AS Squadra_ID,
                       x.ID_Attivita_Programmata AS Attivita_ID,
                       s.Nome AS Squadra,
                       ap.Titolo AS Attivita
                FROM SVOLGE x
                JOIN SQUADRA s ON s.ID_Squadra = x.ID_Squadra
                JOIN ATTIVITA_PROGRAMMATA ap
                  ON ap.ID_Attivita_Programmata =
                     x.ID_Attivita_Programmata
                ORDER BY ap.Periodo_Inizio DESC
                """,
                List.of(
                    CampoEntita.chiave(
                        "Squadra", "ID_Squadra", LOOKUP_SQUADRE
                    ),
                    CampoEntita.chiave(
                        "Attività", "ID_Attivita_Programmata",
                        LOOKUP_ATTIVITA_SQUADRA
                    ),
                    CampoEntita.solaLettura("Squadra", "Squadra"),
                    CampoEntita.solaLettura("Attività", "Attivita")
                ),
                true, false, true
            )
        );
    }

    private static List<DefinizioneEntita> vistaUtente(
            final String section,
            final SessioneUtente session) {

        if (session.utenteId() == null) {
            return List.of();
        }
        final long id = session.utenteId();
        if ("Abbonamenti".equals(section)) {
            final DefinizioneEntita filtered = readOnlyFiltered(
                definizioneAbbonamentiAcquistati(false),
                " WHERE a.ID_Utente = " + id
            );
            final List<CampoEntita> fields =
                new ArrayList<>(filtered.campi());
            fields.set(1, CampoEntita.riferimento(
                "Utente",
                "ID_Utente",
                personalUserLookup(id)
            ));
            return List.of(withPermissions(
                filtered, fields, true, false, false
            ));
        }
        if ("Attività".equals(section)) {
            final DefinizioneEntita filteredRegistrations =
                readOnlyFiltered(
                    definizioneIscrizioni(false),
                    " WHERE a.ID_Utente = " + id
                );
            final List<CampoEntita> fields =
                new ArrayList<>(filteredRegistrations.campi());
            fields.set(1, CampoEntita.riferimento(
                "Abbonamento",
                "ID_Abbonamento",
                personalSubscriptionsLookup(id)
            ));
            return List.of(
                definizioneAttivitaProgrammate(false),
                withPermissions(
                    filteredRegistrations,
                    fields,
                    true,
                    false,
                    false
                )
            );
        }
        if ("Accessi".equals(section)) {
            final DefinizioneEntita access = accessi().getFirst();
            final DefinizioneEntita filtered = readOnlyFiltered(
                access,
                " WHERE a.ID_Utente = " + id
            );
            final List<CampoEntita> fields =
                new ArrayList<>(filtered.campi());
            fields.set(1, CampoEntita.riferimento(
                "Abbonamento",
                "ID_Abbonamento",
                personalSubscriptionsLookup(id)
            ));
            return List.of(withPermissions(
                filtered, fields, true, false, false
            ));
        }
        return List.of();
    }

    private static List<DefinizioneEntita> miaSquadra(
            final SessioneUtente session) {

        if (session.utenteId() == null) {
            return List.of();
        }
        final long id = session.utenteId();
        final List<DefinizioneEntita> definitions = clubESquadre();
        return List.of(readOnlyFiltered(
            definitions.get(2),
            " WHERE a.ID_Utente_Atleta = " + id
        ));
    }

    private static List<DefinizioneEntita> attivitaAssegnate(
            final SessioneUtente session) {

        if (session.utenteId() == null) {
            return List.of();
        }
        final long id = session.utenteId();
        final DefinizioneEntita assignments = attivita().get(2);
        return List.of(readOnlyFiltered(
            assignments,
            " WHERE aa.ID_Utente_Istruttore = " + id
        ));
    }

    private static List<DefinizioneEntita> squadreSeguite(
            final SessioneUtente session) {

        if (session.utenteId() == null) {
            return List.of();
        }
        final long id = session.utenteId();
        return List.of(readOnlyFiltered(
            clubESquadre().get(3),
            " WHERE i.ID_Utente_Istruttore = " + id
        ));
    }

    private static DefinizioneEntita readOnlyFiltered(
            final DefinizioneEntita source,
            final String whereClause) {

        final String query = insertBeforeOrderBy(
            source.querySelezione(),
            whereClause
        );
        return new DefinizioneEntita(
            source.titolo(),
            source.descrizione(),
            source.tabella(),
            query,
            new ArrayList<>(source.campi()),
            false,
            false,
            false
        );
    }

    private static DefinizioneEntita withPermissions(
            final DefinizioneEntita source,
            final List<CampoEntita> fields,
            final boolean insert,
            final boolean update,
            final boolean delete) {

        return new DefinizioneEntita(
            source.titolo(),
            source.descrizione(),
            source.tabella(),
            source.querySelezione(),
            fields,
            insert,
            update,
            delete
        );
    }

    private static String personalUserLookup(final long userId) {
        return """
            SELECT ID_Utente,
                   CONCAT(Cognome, ' ', Nome, ' (#', ID_Utente, ')')
            FROM UTENTE
            WHERE ID_Utente = %d
            """.formatted(userId);
    }

    private static String personalSubscriptionsLookup(final long userId) {
        return """
            SELECT a.ID_Abbonamento,
                   CONCAT(t.Nome, ' (#', a.ID_Abbonamento, ')')
            FROM ABBONAMENTO a
            JOIN TIPO_ABBONAMENTO t
              ON t.ID_Tipo_Abbonamento = a.ID_Tipo_Abbonamento
            WHERE a.ID_Utente = %d
            ORDER BY a.Data_Acquisto DESC
            """.formatted(userId);
    }

    private static String insertBeforeOrderBy(
            final String query,
            final String clause) {

        final int order = query.toUpperCase().lastIndexOf("ORDER BY");
        if (order < 0) {
            return query + clause;
        }
        return query.substring(0, order)
            + clause + "\n" + query.substring(order);
    }
}
