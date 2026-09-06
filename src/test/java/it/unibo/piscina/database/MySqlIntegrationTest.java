package it.unibo.piscina.database;

import static org.junit.jupiter.api.Assertions.*;

import it.unibo.piscina.dao.*;
import it.unibo.piscina.data.*;
import it.unibo.piscina.service.*;
import it.unibo.piscina.service.security.PasswordHasher;
import java.sql.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Prove reali abilitate con piscina.integration=true su un server MySQL isolato.
 * Predisporre gli script 01 e 02 e l'accesso root senza password; specificare
 * piscina.test.url e piscina.test.datadir. La directory viene verificata prima
 * delle operazioni. La suite non avvia né arresta il server di prova.
 */
@EnabledIfSystemProperty(named = "piscina.integration", matches = "true")
class MySqlIntegrationTest {
    private Connection connection;
    private long user, otherUser, timeType, courseType, freeType, entriesType;

    private static Connection open() throws SQLException {
        return open("root", "");
    }

    private static Connection open(String user, String password) throws SQLException {
        final Connection result = DriverManager.getConnection(
            System.getProperty("piscina.test.url"), user, password);
        try {
            try (var statement = result.createStatement();
                    var rows = statement.executeQuery("SELECT @@datadir")) {
                rows.next();
                final var actual = java.nio.file.Path.of(rows.getString(1)).toRealPath();
                final var expected = java.nio.file.Path.of(
                    System.getProperty("piscina.test.datadir")).toRealPath();
                if (!actual.equals(expected)) {
                    throw new SQLException("Il server non usa la directory di test prevista");
                }
            }
            result.setAutoCommit(false);
            return result;
        } catch (Exception exception) {
            result.close();
            throw new SQLException("Verifica del database isolato fallita", exception);
        }
    }

    @BeforeEach
    void connect() throws SQLException {
        connection = open();
        user = scalar(connection, "SELECT ID_Utente FROM UTENTE WHERE Email='cliente@piscina.local'");
        otherUser = scalar(connection, "SELECT ID_Utente FROM UTENTE WHERE Email='atleta@piscina.local'");
        timeType = scalar(connection, "SELECT ID_Tipo_Abbonamento FROM TIPO_ABBONAMENTO WHERE Nome='Annuale corsi'");
        entriesType = scalar(connection, "SELECT ID_Tipo_Abbonamento FROM TIPO_ABBONAMENTO WHERE Nome='Dieci ingressi'");
        courseType = scalar(connection, "SELECT ID_Tipo_Attivita FROM TIPO_ATTIVITA WHERE Nome='Corso di nuoto'");
        freeType = scalar(connection, "SELECT ID_Tipo_Attivita FROM TIPO_ATTIVITA WHERE Nome='Nuoto libero'");
    }

    @AfterEach
    void close() throws SQLException {
        if (connection != null) {
            try { connection.rollback(); } finally { connection.close(); }
        }
    }

    @Test
    void tuttiGliAccountLeVisteILookupEIReportFunzionano() {
        final ConnectionFactory factory = () -> {
            try {
                final var result = open();
                result.setAutoCommit(true);
                return result;
            } catch (SQLException exception) {
                throw new DAOException("Connessione di test fallita", exception);
            }
        };
        DAOUtils.checkConnection(factory);
        final var dao = new JdbcGestioneDAO(factory);
        final var auth = new AuthService(new JdbcAccountDAO(factory),
            new JdbcRegistrazioneDAO(factory), new PasswordHasher());
        dao.sincronizzaStati();
        final var queries = new HashSet<String>();
        final var lookups = new HashSet<String>();
        for (String[] account : new String[][] {
                {"admin", "Admin123!"}, {"club", "Club123!"},
                {"cliente", "Cliente123!"}, {"atleta", "Atleta123!"},
                {"istruttore", "Istruttore123!"}, {"atleta.istruttore", "Completo123!"}}) {
            final var session = auth.accedi(account[0] + "@piscina.local", account[1].toCharArray());
            for (var section : session.sezioniAccessibili()) {
                for (var definition : CatalogoDominio.perSezione(section, session)) {
                    if (queries.add(definition.querySelezione())) {
                        assertNotNull(dao.query(definition.querySelezione()));
                    }
                    for (var field : definition.campi()) {
                        if (field.queryRiferimento() != null && lookups.add(field.queryRiferimento())) {
                            assertNotNull(dao.caricaOpzioni(field.queryRiferimento()));
                        }
                    }
                }
            }
        }
        for (int index = 0; index < CatalogoRiepiloghi.nomi().size(); index++) {
            assertNotNull(dao.query(CatalogoRiepiloghi.query(index,
                LocalDate.now().minusYears(1), LocalDate.now())));
        }
        assertTrue(queries.size() >= 40);
        assertTrue(lookups.size() >= 20);
    }

    @Test
    void registrazioneTecnicaCreaEntrambeLeRigheERollbackNonLasciaOrfani() throws SQLException {
        final ConnectionFactory factory = () -> {
            try { return open(); }
            catch (SQLException exception) { throw new DAOException("Test", exception); }
        };
        final var accounts = new JdbcAccountDAO(factory);
        final var registrations = new JdbcRegistrazioneDAO(factory);
        final var auth = new AuthService(accounts, registrations, new PasswordHasher());
        final String fiscalCode = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        final String email = fiscalCode + "@test.local";
        final var session = auth.registra("Test", "Registrazione", fiscalCode,
            "2000-01-01", email, "Nuova123!".toCharArray(), "Nuova123!".toCharArray());
        assertNotNull(session.utenteId());
        connection.commit(); // Legge il commit della registrazione da uno snapshot nuovo.
        assertEquals(1, scalar(connection, "SELECT COUNT(*) FROM ACCOUNT WHERE ID_Utente=" + session.utenteId()));
        connection.commit();

        final String rejectedCode = (fiscalCode.startsWith("F") ? "E" : "F")
            + fiscalCode.substring(1);
        final var existing = accounts.findByEmail("admin@piscina.local").orElseThrow();
        assertThrows(DAOException.class, () -> registrations.insert(
            rejectedCode, LocalDate.of(2000, 1, 1), existing));
        assertEquals(0, scalar(connection, "SELECT COUNT(*) FROM UTENTE WHERE Codice_Fiscale='" + rejectedCode + "'"));
    }

    @Test
    void capienzaNonScendeSottoDueIscritti() throws SQLException {
        final long activity = activity(courseType, 2);
        execute(connection, enrollment(subscription(user, timeType), activity));
        execute(connection, enrollment(subscription(otherUser, timeType), activity));
        reject(connection, "UPDATE ATTIVITA_PROGRAMMATA SET Capienza_Massima=1 WHERE ID_Attivita_Programmata=" + activity, "capienza");
        assertEquals(2, countEnrollments(activity));
    }

    @Test
    void iscrizioneConcorrenteNonSuperaLaCapienzaConSnapshotPrecedente() throws Exception {
        final long activity = activity(courseType, 1);
        final long first = subscription(user, timeType);
        final long second = subscription(otherUser, timeType);
        connection.commit();
        try (var oldSnapshot = open()) {
            scalar(oldSnapshot, "SELECT COUNT(*) FROM ISCRIZIONE_ATTIVITA");
            execute(connection, enrollment(first, activity));
            connection.commit();
            reject(oldSnapshot, enrollment(second, activity), "Capienza");
            oldSnapshot.rollback();
        }
        assertEquals(1, countEnrollments(activity));
    }

    @Test
    void iscrizioniSimultaneeSerializzanoIlPostoDisponibile() throws Exception {
        final long activity = activity(courseType, 1);
        final long first = subscription(user, timeType);
        final long second = subscription(otherUser, timeType);
        connection.commit();
        execute(connection, enrollment(first, activity));
        final var started = new CountDownLatch(1);
        try (var executor = Executors.newSingleThreadExecutor()) {
            final var result = executor.submit(() -> {
                try (var contender = open()) {
                    started.countDown();
                    reject(contender, enrollment(second, activity), "Capienza");
                    contender.rollback();
                    return true;
                }
            });
            assertTrue(started.await(5, TimeUnit.SECONDS));
            connection.commit();
            assertTrue(result.get(15, TimeUnit.SECONDS));
        }
        assertEquals(1, countEnrollments(activity));
    }

    @Test
    void dueAbbonamentiNonPermettonoDoppiaIscrizioneDelloStessoUtente() throws SQLException {
        final long activity = activity(courseType, 3);
        final long first = subscription(user, timeType);
        final long second = subscription(user, timeType);
        connection.commit();
        try (var oldSnapshot = open()) {
            scalar(oldSnapshot, "SELECT COUNT(*) FROM ISCRIZIONE_ATTIVITA");
            execute(connection, enrollment(first, activity));
            connection.commit();
            reject(oldSnapshot, enrollment(second, activity), "gia iscritto");
            oldSnapshot.rollback();
        }
    }

    @Test
    void riduzioneConcorrenteDellaCapienzaVedeLeNuoveIscrizioni() throws SQLException {
        final long activity = activity(courseType, 2);
        final long first = subscription(user, timeType);
        final long second = subscription(otherUser, timeType);
        connection.commit();
        try (var oldSnapshot = open()) {
            scalar(oldSnapshot, "SELECT COUNT(*) FROM ISCRIZIONE_ATTIVITA");
            execute(connection, enrollment(first, activity));
            execute(connection, enrollment(second, activity));
            connection.commit();
            reject(oldSnapshot, "UPDATE ATTIVITA_PROGRAMMATA SET Capienza_Massima=1 WHERE ID_Attivita_Programmata=" + activity, "capienza");
            oldSnapshot.rollback();
        }
    }

    @Test
    void consumoEDatiStoriciSonoAtomici() throws SQLException {
        final long activity = freeActivity();
        final long subscription = subscription(user, entriesType);
        execute(connection, access(subscription, activity));
        assertEquals(9, remaining(subscription));
        reject(connection, "UPDATE ABBONAMENTO SET Ingressi_Rimanenti=10 WHERE ID_Abbonamento=" + subscription, "residui");
        reject(connection, "UPDATE ABBONAMENTO SET Ingressi_Rimanenti=8 WHERE ID_Abbonamento=" + subscription, "residui");
        reject(connection, "UPDATE ACCESSO_NUOTO_LIBERO SET Data_Ora_Accesso='2000-01-01' WHERE ID_Abbonamento=" + subscription, "non ammessa");
        reject(connection, "DELETE FROM ACCESSO_NUOTO_LIBERO WHERE ID_Abbonamento=" + subscription, "non ammessa");
        assertEquals(9, remaining(subscription));
    }

    @Test
    void dueConsumiConSnapshotPrecedenteMantengonoIlContatore() throws SQLException {
        final long activity = freeActivity();
        final long subscription = subscription(user, entriesType);
        connection.commit();
        try (var oldSnapshot = open()) {
            scalar(oldSnapshot, "SELECT COUNT(*) FROM ACCESSO_NUOTO_LIBERO");
            execute(connection, access(subscription, activity));
            connection.commit();
            execute(oldSnapshot, access(subscription, activity));
            oldSnapshot.commit();
        }
        assertEquals(8, remaining(subscription));
    }

    @Test
    void ultimoIngressoNonPuoEssereConsumatoDueVolte() throws SQLException {
        final long activity = freeActivity();
        final long single = scalar(connection, "SELECT ID_Tipo_Abbonamento FROM TIPO_ABBONAMENTO WHERE Nome='Ingresso Singolo'");
        final long subscription = subscription(user, single);
        connection.commit();
        try (var oldSnapshot = open()) {
            scalar(oldSnapshot, "SELECT COUNT(*) FROM ACCESSO_NUOTO_LIBERO");
            execute(connection, access(subscription, activity));
            connection.commit();
            reject(oldSnapshot, access(subscription, activity), "non utilizzabile");
            oldSnapshot.rollback();
        }
        assertEquals(0, remaining(subscription));
    }

    @Test
    void accessoFuoriFasciaNonConsumaIngressi() throws SQLException {
        final long activity = freeActivity();
        final long subscription = subscription(user, entriesType);
        reject(connection, "INSERT INTO ACCESSO_NUOTO_LIBERO(ID_Abbonamento,ID_Attivita_Programmata,Data_Ora_Accesso) VALUES(" + subscription + "," + activity + ",'2000-01-01')", "fuori");
        assertEquals(10, remaining(subscription));
    }

    @Test
    void acquistiEIscrizioniNonSiRiscrivonoONonSiCancellano() throws SQLException {
        final long activity = activity(courseType, 2);
        final long subscription = subscription(user, timeType);
        execute(connection, enrollment(subscription, activity));
        reject(connection, "UPDATE ABBONAMENTO SET ID_Utente=" + otherUser + " WHERE ID_Abbonamento=" + subscription, "storici");
        reject(connection, "DELETE FROM ABBONAMENTO WHERE ID_Abbonamento=" + subscription, "non ammessa");
        reject(connection, "UPDATE ISCRIZIONE_ATTIVITA SET Data_Iscrizione='2000-01-01' WHERE ID_Abbonamento=" + subscription, "non ammessa");
        reject(connection, "DELETE FROM ISCRIZIONE_ATTIVITA WHERE ID_Abbonamento=" + subscription, "non ammessa");
        reject(connection, "UPDATE ATTIVITA_PROGRAMMATA SET Ora_Inizio='00:01:00' WHERE ID_Attivita_Programmata=" + activity, "storici");
    }

    @Test
    void aggiornareCompatibilitaNonAggiraIlControlloDiModalita() throws SQLException {
        reject(connection, "UPDATE COMPATIBILITA SET ID_Tipo_Attivita=" + courseType + " WHERE ID_Tipo_Abbonamento=" + entriesType + " AND ID_Tipo_Attivita=" + freeType, "non ammessa");
    }

    @Test
    void corsieConcorrentiNonSiSovrappongono() throws SQLException {
        final long lane = lane();
        final long first = activity(courseType, 3);
        final long second = activity(courseType, 3);
        connection.commit();
        try (var oldSnapshot = open()) {
            scalar(oldSnapshot, "SELECT COUNT(*) FROM UTILIZZA");
            execute(connection, useLane(first, lane));
            connection.commit();
            reject(oldSnapshot, useLane(second, lane), "occupata");
            oldSnapshot.rollback();
        }
    }

    @Test
    void attivitaAttivaConservaUltimaCorsia() throws SQLException {
        final long activity = freeActivity();
        reject(connection, "DELETE FROM UTILIZZA WHERE ID_Attivita_Programmata=" + activity, "almeno una corsia");
        reject(connection, "UPDATE UTILIZZA SET Numero_Corsia=2 WHERE ID_Attivita_Programmata=" + activity, "non ammessa");
    }

    @Test
    void rapportoSportivoSiChiudeSenzaRiscrivereLoStorico() throws SQLException {
        final long id = scalar(connection, "SELECT ID_Appartenenza FROM APPARTENENZA_SQUADRA WHERE ID_Utente_Atleta=" + otherUser + " LIMIT 1");
        execute(connection, "UPDATE APPARTENENZA_SQUADRA SET Data_Fine=CURRENT_DATE WHERE ID_Appartenenza=" + id);
        reject(connection, "UPDATE APPARTENENZA_SQUADRA SET Data_Inizio=Data_Inizio - INTERVAL 1 DAY WHERE ID_Appartenenza=" + id, "storico");
        reject(connection, "DELETE FROM APPARTENENZA_SQUADRA WHERE ID_Appartenenza=" + id, "non ammessa");
    }

    @Test
    void rapportiSportiviConcorrentiNonSiSovrappongono() throws SQLException {
        final String code = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        final long person = insert("INSERT INTO UTENTE(Codice_Fiscale,Nome,Cognome,Data_Nascita,Email) VALUES('"
            + code + "','Test','Sport','2000-01-01','" + code + "@sport.local')");
        execute(connection, "INSERT INTO ATLETA(ID_Utente) VALUES(" + person + ")");
        execute(connection, "INSERT INTO ISTRUTTORE(ID_Utente,Qualifica) VALUES(" + person + ",'Test')");
        final long team = scalar(connection, "SELECT MIN(ID_Squadra) FROM SQUADRA");
        connection.commit();
        try (var oldSnapshot = open()) {
            scalar(oldSnapshot, "SELECT COUNT(*) FROM APPARTENENZA_SQUADRA");
            scalar(oldSnapshot, "SELECT COUNT(*) FROM INCARICO_SQUADRA");
            final String membership = "INSERT INTO APPARTENENZA_SQUADRA(ID_Utente_Atleta,ID_Squadra,Data_Inizio) VALUES("
                + person + "," + team + ",CURRENT_DATE)";
            final String appointment = "INSERT INTO INCARICO_SQUADRA(ID_Utente_Istruttore,ID_Squadra,Data_Inizio) VALUES("
                + person + "," + team + ",CURRENT_DATE)";
            execute(connection, membership);
            execute(connection, appointment);
            connection.commit();
            reject(oldSnapshot, membership, "appartiene gia");
            reject(oldSnapshot, appointment, "sovrapposto");
            oldSnapshot.rollback();
        }
    }

    private long subscription(long owner, long type) throws SQLException {
        return insert("INSERT INTO ABBONAMENTO(ID_Utente,ID_Tipo_Abbonamento,Data_Acquisto,Data_Inizio) VALUES(" + owner + "," + type + ",CURRENT_DATE,CURRENT_DATE)");
    }

    private long activity(long type, int capacity) throws SQLException {
        return insert("INSERT INTO ATTIVITA_PROGRAMMATA(ID_Tipo_Attivita,Titolo,Giorno_Settimanale,Ora_Inizio,Ora_Fine,Capienza_Massima,Periodo_Inizio,Periodo_Fine,Stato) VALUES(" + type + ",'Test " + UUID.randomUUID() + "',ELT(WEEKDAY(CURRENT_DATE)+1,'LUNEDI','MARTEDI','MERCOLEDI','GIOVEDI','VENERDI','SABATO','DOMENICA'),'00:00','23:59:59'," + capacity + ",CURRENT_DATE,CURRENT_DATE + INTERVAL 30 DAY,'PROGRAMMATA')");
    }

    private long lane() throws SQLException {
        final long pool = insert("INSERT INTO VASCA(Nome,Lunghezza,Larghezza,Profondita,Temperatura,Tipologia) VALUES('Test " + UUID.randomUUID() + "',25,12,1.5,28,'Coperta')");
        execute(connection, "INSERT INTO CORSIA(ID_Vasca,Numero) VALUES(" + pool + ",1)");
        return pool;
    }

    private long freeActivity() throws SQLException {
        final long activity = activity(freeType, 20);
        execute(connection, useLane(activity, lane()));
        execute(connection, "UPDATE ATTIVITA_PROGRAMMATA SET Stato='ATTIVA' WHERE ID_Attivita_Programmata=" + activity);
        return activity;
    }

    private static String useLane(long activity, long pool) {
        return "INSERT INTO UTILIZZA(ID_Attivita_Programmata,ID_Vasca,Numero_Corsia) VALUES(" + activity + "," + pool + ",1)";
    }

    private static String enrollment(long subscription, long activity) {
        return "INSERT INTO ISCRIZIONE_ATTIVITA(ID_Abbonamento,ID_Attivita_Programmata) VALUES(" + subscription + "," + activity + ")";
    }

    private static String access(long subscription, long activity) {
        return "INSERT INTO ACCESSO_NUOTO_LIBERO(ID_Abbonamento,ID_Attivita_Programmata) VALUES(" + subscription + "," + activity + ")";
    }

    private long countEnrollments(long activity) throws SQLException {
        return scalar(connection, "SELECT COUNT(*) FROM ISCRIZIONE_ATTIVITA WHERE ID_Attivita_Programmata=" + activity);
    }

    private long remaining(long subscription) throws SQLException {
        return scalar(connection, "SELECT Ingressi_Rimanenti FROM ABBONAMENTO WHERE ID_Abbonamento=" + subscription);
    }

    private long insert(String sql) throws SQLException {
        execute(connection, sql);
        return scalar(connection, "SELECT LAST_INSERT_ID()");
    }

    private static long scalar(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
            assertTrue(rows.next(), sql);
            return rows.getLong(1);
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static void reject(Connection connection, String sql, String message) {
        final var exception = assertThrows(SQLException.class, () -> execute(connection, sql));
        assertEquals("45000", exception.getSQLState(), exception.getMessage());
        assertTrue(exception.getMessage().contains(message), exception.getMessage());
    }
}
