package it.unibo.piscina.data;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.List;

/** Funzioni comuni per l'accesso al database. */
public final class DAOUtils {

    private DAOUtils() {
    }

    public static Properties loadDatabaseProperties() {
        final Properties properties = new Properties();

        try (InputStream input = DAOUtils.class
                .getResourceAsStream("/database.properties")) {

            if (input == null) {
                throw new DAOException(
                    "File database.properties non trovato"
                );
            }

            properties.load(input);

        } catch (IOException exception) {
            throw new DAOException(
                "Impossibile leggere database.properties",
                exception
            );
        }

        return properties;
    }

    public static Connection createConnection(
            final Properties properties) {

        final String url = configuredValue(
            "piscina.db.url",
            "PISCINA_DB_URL",
            properties.getProperty("database.url")
        );
        final String user = configuredValue(
            "piscina.db.user",
            "PISCINA_DB_USER",
            properties.getProperty("database.user")
        );
        final String password = configuredValue(
            "piscina.db.password",
            "PISCINA_DB_PASSWORD",
            properties.getProperty("database.password", "")
        );

        loadMySqlDriver();
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException exception) {
            throw new DAOException(
                "Impossibile connettersi al database: "
                    + exception.getMessage(),
                exception
            );
        }
    }

    public static void checkConnection(
            final ConnectionFactory connectionFactory) {

        try (Connection connection = connectionFactory.openConnection();
                var statement = connection.createStatement()) {
            for (String table : List.of(
                    "UTENTE", "ATLETA", "ISTRUTTORE", "ACCOUNT",
                    "TIPO_ABBONAMENTO", "ABBONAMENTO", "TIPO_ATTIVITA",
                    "ATTIVITA_PROGRAMMATA", "ISCRIZIONE_ATTIVITA",
                    "ACCESSO_NUOTO_LIBERO", "COMPATIBILITA", "VASCA", "CORSIA",
                    "UTILIZZA", "ASSEGNATO_A", "CLUB_SPORTIVO", "SQUADRA",
                    "APPARTENENZA_SQUADRA", "INCARICO_SQUADRA", "SVOLGE",
                    "VW_ATTIVITA_COMPLETE", "VW_ABBONAMENTI_UTILIZZABILI",
                    "VW_CLUB_ATLETI_ATTIVI", "VW_SQUADRA_ATLETI_ATTIVI")) {
                // Verifica esistenza, permessi e dipendenze senza scansionare i dati.
                try (var result = statement.executeQuery(
                        "SELECT * FROM " + table + " LIMIT 0")) {
                    result.getMetaData();
                }
            }
        } catch (SQLException exception) {
            throw new DAOException(
                "Schema incompleto o non accessibile: " + exception.getMessage(),
                exception
            );
        }
    }

    private static String configuredValue(
            final String systemPropertyName,
            final String environmentName,
            final String propertyValue) {

        final String systemPropertyValue =
            System.getProperty(systemPropertyName);
        if (systemPropertyValue != null
                && !systemPropertyValue.isBlank()) {
            return systemPropertyValue;
        }

        final String environmentValue = System.getenv(environmentName);
        return environmentValue == null || environmentValue.isBlank()
            ? propertyValue
            : environmentValue;
    }

    private static void loadMySqlDriver() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException exception) {
            throw new DAOException(
                "Driver MySQL JDBC non trovato: verifica che il connector "
                    + "presente in lib sia incluso nel classpath",
                exception
            );
        }
    }
}
