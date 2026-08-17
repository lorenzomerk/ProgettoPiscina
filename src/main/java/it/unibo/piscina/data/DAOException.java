package it.unibo.piscina.data;

/** Eccezione usata dal livello di accesso ai dati. */
public class DAOException extends RuntimeException {
    public DAOException(final String message) {
        super(message);
    }

    public DAOException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
