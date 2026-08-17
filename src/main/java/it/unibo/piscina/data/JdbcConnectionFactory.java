package it.unibo.piscina.data;

import java.sql.Connection;
import java.util.Objects;
import java.util.Properties;

/** Connection factory configurata tramite database.properties. */
public final class JdbcConnectionFactory implements ConnectionFactory {

    private final Properties properties;

    public JdbcConnectionFactory(final Properties properties) {
        this.properties = Objects.requireNonNull(properties);
    }

    @Override
    public Connection openConnection() {
        return DAOUtils.createConnection(properties);
    }
}
