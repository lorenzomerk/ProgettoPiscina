package it.unibo.piscina.dao;

import it.unibo.piscina.data.ConnectionFactory;
import it.unibo.piscina.data.DAOException;
import it.unibo.piscina.model.AccountAutenticazione;
import it.unibo.piscina.model.QualificaUtente;
import it.unibo.piscina.model.RuoloApplicativo;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;

/** Implementazione JDBC della persistenza degli account. */
public final class JdbcAccountDAO implements AccountDAO {

    private static final String FIND_BY_EMAIL = """
        SELECT A.ID_Account, A.ID_Utente, A.Nome, A.Cognome, A.Email,
               A.Password_Hash, A.Password_Salt, A.Password_Iterazioni,
               A.Ruolo, A.Attivo,
               EXISTS (
                   SELECT 1
                   FROM ATLETA AT
                   WHERE AT.ID_Utente = A.ID_Utente
               ) AS Qualifica_Atleta,
               EXISTS (
                   SELECT 1
                   FROM ISTRUTTORE I
                   WHERE I.ID_Utente = A.ID_Utente
               ) AS Qualifica_Istruttore
        FROM ACCOUNT A
        WHERE A.Email = ?
        """;

    private static final String INSERT = """
        INSERT INTO ACCOUNT
            (ID_Utente, Nome, Cognome, Email, Password_Hash, Password_Salt,
             Password_Iterazioni, Ruolo, Attivo)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

    private static final String RECORD_ACCESS = """
        UPDATE ACCOUNT
        SET Ultimo_Accesso = CURRENT_TIMESTAMP
        WHERE ID_Account = ?
        """;

    private final ConnectionFactory connectionFactory;

    public JdbcAccountDAO(final ConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory);
    }

    @Override
    public Optional<AccountAutenticazione> findByEmail(
            final String email) {

        try (Connection connection = connectionFactory.openConnection();
                PreparedStatement statement =
                    connection.prepareStatement(FIND_BY_EMAIL)) {

            statement.setString(1, email);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                    ? Optional.of(mapAccount(resultSet))
                    : Optional.empty();
            }
        } catch (SQLException exception) {
            throw databaseError("cercare l'account", exception);
        }
    }

    @Override
    public long insert(final AccountAutenticazione account) {
        try (Connection connection = connectionFactory.openConnection();
                PreparedStatement statement = connection.prepareStatement(
                    INSERT,
                    Statement.RETURN_GENERATED_KEYS
                )) {

            if (account.utenteId() == null) {
                statement.setNull(1, Types.BIGINT);
            } else {
                statement.setLong(1, account.utenteId());
            }
            statement.setString(2, account.nome());
            statement.setString(3, account.cognome());
            statement.setString(4, account.email());
            statement.setString(5, account.passwordHash());
            statement.setString(6, account.passwordSalt());
            statement.setInt(7, account.passwordIterazioni());
            statement.setString(8, account.ruolo().name());
            statement.setBoolean(9, account.attivo());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            throw new DAOException(
                "Account creato senza identificativo generato"
            );
        } catch (SQLException exception) {
            if (exception.getSQLState() != null
                    && exception.getSQLState().startsWith("23")) {
                throw new DAOException("Email già registrata", exception);
            }
            throw databaseError("creare l'account", exception);
        }
    }

    @Override
    public void recordSuccessfulAccess(final long accountId) {
        try (Connection connection = connectionFactory.openConnection();
                PreparedStatement statement =
                    connection.prepareStatement(RECORD_ACCESS)) {

            statement.setLong(1, accountId);
            if (statement.executeUpdate() != 1) {
                throw new DAOException(
                    "Account non trovato durante la registrazione dell'accesso"
                );
            }
        } catch (SQLException exception) {
            throw databaseError("registrare l'ultimo accesso", exception);
        }
    }

    private AccountAutenticazione mapAccount(final ResultSet resultSet)
            throws SQLException {

        final EnumSet<QualificaUtente> qualifiche =
            EnumSet.noneOf(QualificaUtente.class);
        if (resultSet.getBoolean("Qualifica_Atleta")) {
            qualifiche.add(QualificaUtente.ATLETA);
        }
        if (resultSet.getBoolean("Qualifica_Istruttore")) {
            qualifiche.add(QualificaUtente.ISTRUTTORE);
        }
        final Number utenteId = (Number) resultSet.getObject("ID_Utente");
        return new AccountAutenticazione(
            resultSet.getLong("ID_Account"),
            utenteId == null ? null : utenteId.longValue(),
            resultSet.getString("Nome"),
            resultSet.getString("Cognome"),
            resultSet.getString("Email"),
            resultSet.getString("Password_Hash"),
            resultSet.getString("Password_Salt"),
            resultSet.getInt("Password_Iterazioni"),
            RuoloApplicativo.valueOf(resultSet.getString("Ruolo")),
            qualifiche,
            resultSet.getBoolean("Attivo")
        );
    }

    private DAOException databaseError(
            final String operation,
            final SQLException exception) {

        return new DAOException(
            "Impossibile " + operation
                + ". Verifica lo schema ACCOUNT nel database.",
            exception
        );
    }
}
