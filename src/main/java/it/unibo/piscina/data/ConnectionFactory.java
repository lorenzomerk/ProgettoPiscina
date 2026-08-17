package it.unibo.piscina.data;

import java.sql.Connection;

/** Crea connessioni JDBC per i DAO dell'applicazione. */
@FunctionalInterface
public interface ConnectionFactory {

    Connection openConnection();
}
