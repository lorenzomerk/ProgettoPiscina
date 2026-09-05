package it.unibo.piscina.dao;

import it.unibo.piscina.data.ConnectionFactory;
import it.unibo.piscina.data.DAOException;
import it.unibo.piscina.model.AccountAutenticazione;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Objects;

/** Registrazione JDBC eseguita in un'unica transazione. */
public final class JdbcRegistrazioneDAO implements RegistrazioneDAO {

    private static final String INSERT_UTENTE = """
        INSERT INTO UTENTE
            (Codice_Fiscale, Nome, Cognome, Data_Nascita, Email)
        VALUES (?, ?, ?, ?, ?)
        """;

    private static final String INSERT_ACCOUNT = """
        INSERT INTO ACCOUNT
            (ID_Utente, ID_Club, Nome, Cognome, Email, Password_Hash,
             Password_Salt, Password_Iterazioni, Ruolo, Attivo)
        VALUES (?, NULL, ?, ?, ?, ?, ?, ?, 'UTENTE', TRUE)
        """;

    private final ConnectionFactory connectionFactory;

    public JdbcRegistrazioneDAO(final ConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory);
    }

    @Override
    public RegistrazioneCreata insert(
            final String codiceFiscale,
            final LocalDate dataNascita,
            final AccountAutenticazione account) {

        try (Connection connection = connectionFactory.openConnection()) {
            connection.setAutoCommit(false);
            try {
                final long utenteId = insertUtente(
                    connection,
                    codiceFiscale,
                    dataNascita,
                    account
                );
                final long accountId = insertAccount(
                    connection,
                    utenteId,
                    account
                );
                connection.commit();
                return new RegistrazioneCreata(accountId, utenteId);
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            if (exception.getSQLState() != null
                    && exception.getSQLState().startsWith("23")) {
                throw new DAOException(
                    "Codice fiscale o email già registrati",
                    exception
                );
            }
            throw new DAOException(
                "Impossibile completare la registrazione",
                exception
            );
        }
    }

    private long insertUtente(
            final Connection connection,
            final String codiceFiscale,
            final LocalDate dataNascita,
            final AccountAutenticazione account) throws SQLException {

        try (PreparedStatement statement = connection.prepareStatement(
                INSERT_UTENTE,
                Statement.RETURN_GENERATED_KEYS
            )) {

            statement.setString(1, codiceFiscale);
            statement.setString(2, account.nome());
            statement.setString(3, account.cognome());
            statement.setDate(4, Date.valueOf(dataNascita));
            statement.setString(5, account.email());
            statement.executeUpdate();
            return generatedKey(statement, "utente");
        }
    }

    private long insertAccount(
            final Connection connection,
            final long utenteId,
            final AccountAutenticazione account) throws SQLException {

        try (PreparedStatement statement = connection.prepareStatement(
                INSERT_ACCOUNT,
                Statement.RETURN_GENERATED_KEYS
            )) {

            statement.setLong(1, utenteId);
            statement.setString(2, account.nome());
            statement.setString(3, account.cognome());
            statement.setString(4, account.email());
            statement.setString(5, account.passwordHash());
            statement.setString(6, account.passwordSalt());
            statement.setInt(7, account.passwordIterazioni());
            statement.executeUpdate();
            return generatedKey(statement, "account");
        }
    }

    private long generatedKey(
            final PreparedStatement statement,
            final String entity) throws SQLException {

        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getLong(1);
            }
        }
        throw new DAOException(
            "Creazione " + entity + " senza identificativo generato"
        );
    }

    private void rollback(
            final Connection connection,
            final Exception original) {

        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            original.addSuppressed(rollbackError);
        }
    }
}
