package it.unibo.piscina.dao;

import it.unibo.piscina.data.ConnectionFactory;
import it.unibo.piscina.data.DAOException;
import it.unibo.piscina.model.DatiTabella;
import it.unibo.piscina.model.OpzioneRiferimento;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Implementazione JDBC dei moduli CRUD e delle consultazioni aggregate. */
public final class JdbcGestioneDAO implements GestioneDAO {

    private static final String UPDATE_ABBONAMENTI_TEMPO = """
        UPDATE ABBONAMENTO a
        JOIN TIPO_ABBONAMENTO t
          ON t.ID_Tipo_Abbonamento = a.ID_Tipo_Abbonamento
        SET a.Stato = 'SCADUTO'
        WHERE t.Modalita_Validita = 'TEMPO'
          AND a.Stato = 'ATTIVO'
          AND a.Data_Fine < CURRENT_DATE
        """;

    private static final String UPDATE_ABBONAMENTI_INGRESSI = """
        UPDATE ABBONAMENTO a
        JOIN TIPO_ABBONAMENTO t
          ON t.ID_Tipo_Abbonamento = a.ID_Tipo_Abbonamento
        SET a.Stato = 'ESAURITO'
        WHERE t.Modalita_Validita = 'INGRESSI'
          AND a.Stato = 'ATTIVO'
          AND a.Ingressi_Rimanenti = 0
        """;

    private static final String UPDATE_ATTIVITA_NON_ATTIVE = """
        UPDATE ATTIVITA_PROGRAMMATA ap
        JOIN TIPO_ATTIVITA ta
          ON ta.ID_Tipo_Attivita = ap.ID_Tipo_Attivita
        SET ap.Stato = CASE
            WHEN CURRENT_DATE < ap.Periodo_Inizio THEN 'PROGRAMMATA'
            WHEN CURRENT_DATE > ap.Periodo_Fine THEN 'CONCLUSA'
            WHEN EXISTS (
                SELECT 1 FROM UTILIZZA u
                 WHERE u.ID_Attivita_Programmata =
                       ap.ID_Attivita_Programmata
            ) AND (
                ta.Richiede_Istruttore = FALSE
                OR EXISTS (
                    SELECT 1 FROM ASSEGNATO_A aa
                     WHERE aa.ID_Attivita_Programmata =
                           ap.ID_Attivita_Programmata
                )
            ) AND (
                ta.Modalita_Partecipazione <> 'SQUADRA'
                OR EXISTS (
                    SELECT 1 FROM SVOLGE s
                     WHERE s.ID_Attivita_Programmata =
                           ap.ID_Attivita_Programmata
                )
            ) THEN 'ATTIVA'
            ELSE 'PROGRAMMATA'
        END
        WHERE ap.Stato <> 'ANNULLATA'
        """;

    private final ConnectionFactory connectionFactory;

    public JdbcGestioneDAO(final ConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory);
    }

    @Override
    public DatiTabella query(final String sql) {
        try (Connection connection = connectionFactory.openConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {

            final ResultSetMetaData metadata = resultSet.getMetaData();
            final int count = metadata.getColumnCount();
            final List<String> columns = new ArrayList<>(count);
            for (int index = 1; index <= count; index++) {
                columns.add(metadata.getColumnLabel(index));
            }

            final List<List<Object>> rows = new ArrayList<>();
            while (resultSet.next()) {
                final List<Object> row = new ArrayList<>(count);
                for (int index = 1; index <= count; index++) {
                    row.add(resultSet.getObject(index));
                }
                rows.add(row);
            }
            return new DatiTabella(columns, rows);
        } catch (SQLException exception) {
            throw databaseError("caricare i dati", exception);
        }
    }

    @Override
    public List<OpzioneRiferimento> caricaOpzioni(final String sql) {
        final List<OpzioneRiferimento> options = new ArrayList<>();
        try (Connection connection = connectionFactory.openConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                options.add(new OpzioneRiferimento(
                    resultSet.getObject(1),
                    resultSet.getString(2)
                ));
            }
            return options;
        } catch (SQLException exception) {
            throw databaseError("caricare i valori disponibili", exception);
        }
    }

    @Override
    public int execute(
            final String sql,
            final List<Object> parameters) {

        try (Connection connection = connectionFactory.openConnection();
                PreparedStatement statement =
                    connection.prepareStatement(sql)) {

            for (int index = 0; index < parameters.size(); index++) {
                bind(statement, index + 1, parameters.get(index));
            }
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw databaseError("completare l'operazione", exception);
        }
    }

    @Override
    public void sincronizzaStati() {
        try (Connection connection = connectionFactory.openConnection();
                Statement statement = connection.createStatement()) {

            connection.setAutoCommit(false);
            try {
                statement.executeUpdate(UPDATE_ABBONAMENTI_TEMPO);
                statement.executeUpdate(UPDATE_ABBONAMENTI_INGRESSI);
                statement.executeUpdate(UPDATE_ATTIVITA_NON_ATTIVE);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw databaseError("aggiornare gli stati operativi", exception);
        }
    }

    private void bind(
            final PreparedStatement statement,
            final int index,
            final Object value) throws SQLException {

        if (value instanceof LocalDate date) {
            statement.setObject(index, date);
        } else if (value instanceof LocalTime time) {
            statement.setObject(index, time);
        } else if (value instanceof LocalDateTime dateTime) {
            statement.setObject(index, dateTime);
        } else {
            statement.setObject(index, value);
        }
    }

    private DAOException databaseError(
            final String operation,
            final SQLException exception) {

        final String detail = exception.getMessage() == null
            ? "errore SQL"
            : exception.getMessage();
        return new DAOException(
            "Impossibile " + operation + ": " + detail,
            exception
        );
    }
}
