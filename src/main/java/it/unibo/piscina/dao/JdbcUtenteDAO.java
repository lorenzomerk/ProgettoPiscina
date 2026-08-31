package it.unibo.piscina.dao;

import it.unibo.piscina.data.ConnectionFactory;
import it.unibo.piscina.data.DAOException;
import it.unibo.piscina.model.Utente;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Implementazione JDBC del DAO degli utenti. */
public final class JdbcUtenteDAO implements UtenteDAO {

    private static final String SELECT_ALL = """
        SELECT ID_Utente, Codice_Fiscale, Nome, Cognome,
               Data_Nascita, Data_Registrazione, Email, Telefono,
               Scadenza_Certificato_Medico, Attivo,
               EXISTS (
                   SELECT 1 FROM ATLETA a
                   WHERE a.ID_Utente = u.ID_Utente
               ) AS Atleta,
               EXISTS (
                   SELECT 1 FROM ISTRUTTORE i
                   WHERE i.ID_Utente = u.ID_Utente
               ) AS Istruttore,
               (
                   SELECT i.Qualifica FROM ISTRUTTORE i
                   WHERE i.ID_Utente = u.ID_Utente
               ) AS Qualifica_Istruttore
        FROM UTENTE u
        ORDER BY Cognome, Nome, ID_Utente
        """;

    private static final String INSERT = """
        INSERT INTO UTENTE
            (Codice_Fiscale, Nome, Cognome, Data_Nascita,
             Email, Telefono, Scadenza_Certificato_Medico, Attivo)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

    private static final String UPDATE = """
        UPDATE UTENTE
        SET Codice_Fiscale = ?, Nome = ?, Cognome = ?,
            Data_Nascita = ?, Email = ?, Telefono = ?,
            Scadenza_Certificato_Medico = ?, Attivo = ?
        WHERE ID_Utente = ?
        """;

    private static final String DEACTIVATE = """
        UPDATE UTENTE SET Attivo = FALSE WHERE ID_Utente = ?
        """;

        private static final String SEARCH_USER = """
        SELECT ID_Utente, Codice_Fiscale, Nome, Cognome,
               Data_Nascita, Data_Registrazione, Email, Telefono,
               Scadenza_Certificato_Medico, Attivo,
               EXISTS (
                   SELECT 1 FROM ATLETA a
                   WHERE a.ID_Utente = u.ID_Utente
               ) AS Atleta,
               EXISTS (
                   SELECT 1 FROM ISTRUTTORE i
                   WHERE i.ID_Utente = u.ID_Utente
               ) AS Istruttore,
               (
                   SELECT i.Qualifica FROM ISTRUTTORE i
                   WHERE i.ID_Utente = u.ID_Utente
               ) AS Qualifica_Istruttore
        FROM UTENTE u
        WHERE LOWER(Nome) LIKE ? 
           OR LOWER(Cognome) LIKE ? 
           OR LOWER(CONCAT(Nome, ' ', Cognome)) LIKE ? 
           OR LOWER(CONCAT(Cognome, ' ', Nome)) LIKE ?
        ORDER BY Cognome, Nome, ID_Utente
        """;

    private final ConnectionFactory connectionFactory;

    public JdbcUtenteDAO(final ConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory);
    }

    @Override
    public List<Utente> findAll() {
        final List<Utente> utenti = new ArrayList<>();
        try (Connection connection = connectionFactory.openConnection();
                PreparedStatement statement =
                    connection.prepareStatement(SELECT_ALL);
                ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                utenti.add(mapUtente(resultSet));
            }
            return utenti;
        } catch (SQLException exception) {
            throw databaseError("caricare gli utenti", exception);
        }
    }

    @Override
    public long insert(final Utente utente) {
        try (Connection connection = connectionFactory.openConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(
                    INSERT,
                    Statement.RETURN_GENERATED_KEYS
                )) {

                bindFields(statement, utente);
                statement.executeUpdate();

                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        final long id = keys.getLong(1);
                        syncQualifications(connection, id, utente);
                        connection.commit();
                        return id;
                    }
                }
                throw new DAOException(
                    "Inserimento completato senza identificativo generato"
                );
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            throw databaseError("inserire l'utente", exception);
        }
    }

    @Override
    public boolean update(final Utente utente) {
        try (Connection connection = connectionFactory.openConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement =
                    connection.prepareStatement(UPDATE)) {

                bindFields(statement, utente);
                statement.setLong(9, utente.id());
                final boolean updated = statement.executeUpdate() == 1;
                if (updated) {
                    syncQualifications(connection, utente.id(), utente);
                }
                connection.commit();
                return updated;
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            throw databaseError("modificare l'utente", exception);
        }
    }

    @Override
    public boolean deactivate(final long id) {
        try (Connection connection = connectionFactory.openConnection();
                PreparedStatement statement =
                    connection.prepareStatement(DEACTIVATE)) {

            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw databaseError("disattivare l'utente", exception);
        }
    }

    private void bindFields(
            final PreparedStatement statement,
            final Utente utente) throws SQLException {

        statement.setString(1, utente.codiceFiscale());
        statement.setString(2, utente.nome());
        statement.setString(3, utente.cognome());
        statement.setDate(4, Date.valueOf(utente.dataNascita()));
        statement.setString(5, utente.email());
        statement.setString(6, utente.telefono());
        statement.setDate(
            7,
            utente.scadenzaCertificatoMedico() == null
                ? null
                : Date.valueOf(utente.scadenzaCertificatoMedico())
        );
        statement.setBoolean(8, utente.attivo());
    }

    private Utente mapUtente(final ResultSet resultSet)
            throws SQLException {

        final Date certificateDate = resultSet.getDate(
            "Scadenza_Certificato_Medico"
        );
        return new Utente(
            resultSet.getLong("ID_Utente"),
            resultSet.getString("Codice_Fiscale"),
            resultSet.getString("Nome"),
            resultSet.getString("Cognome"),
            resultSet.getDate("Data_Nascita").toLocalDate(),
            resultSet.getString("Email"),
            resultSet.getString("Telefono"),
            certificateDate == null
                ? null
                : certificateDate.toLocalDate(),
            resultSet.getBoolean("Attivo")
            ,
            resultSet.getBoolean("Atleta"),
            resultSet.getBoolean("Istruttore"),
            resultSet.getString("Qualifica_Istruttore"),
            resultSet.getDate("Data_Registrazione").toLocalDate()
        );
    }

    private void syncQualifications(
            final Connection connection,
            final long userId,
            final Utente utente) throws SQLException {

        if (utente.atleta()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT IGNORE INTO ATLETA (ID_Utente) VALUES (?)")) {
                statement.setLong(1, userId);
                statement.executeUpdate();
            }
        } else {
            deleteQualification(connection, "ATLETA", userId);
        }

        if (utente.istruttore()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO ISTRUTTORE (ID_Utente, Qualifica)
                    VALUES (?, ?)
                    ON DUPLICATE KEY UPDATE Qualifica = VALUES(Qualifica)
                    """)) {
                statement.setLong(1, userId);
                statement.setString(2, utente.qualificaIstruttore());
                statement.executeUpdate();
            }
        } else {
            deleteQualification(connection, "ISTRUTTORE", userId);
        }
    }

    private void deleteQualification(
            final Connection connection,
            final String table,
            final long userId) throws SQLException {

        final String sql = "DELETE FROM " + table + " WHERE ID_Utente = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.executeUpdate();
        }
    }

    private void rollback(
            final Connection connection,
            final Exception original) {

        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            original.addSuppressed(rollbackException);
        }
    }

    private DAOException databaseError(
            final String operation,
            final SQLException exception) {

        if (exception.getSQLState() != null
                && exception.getSQLState().startsWith("23")) {
            return new DAOException(
                "Codice fiscale o email già presenti nel database",
                exception
            );
        }
        return new DAOException(
            "Impossibile " + operation
                + ". Verifica che doc/sql/01_schema_completo.sql sia stato "
                + "eseguito.",
            exception
        );
    }

    @Override
    public List<Utente> search(final String keyword) {
        final List<Utente> utenti = new ArrayList<>();
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(SEARCH_USER)) {
            
            final String pattern = "%" + keyword.toLowerCase(java.util.Locale.ROOT) + "%";
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            statement.setString(4, pattern);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    utenti.add(mapUtente(resultSet));
                }
            }
            return utenti;
        } catch (SQLException exception) {
            throw databaseError("cercare gli utenti", exception);
        }
    }
}
