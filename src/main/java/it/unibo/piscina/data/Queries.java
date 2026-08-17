package it.unibo.piscina.data;

/** Query SQL condivise dall'applicazione. */
public final class Queries {
    private Queries() {
    }

    public static final String HEALTH_CHECK =
        "SELECT (SELECT COUNT(*) FROM UTENTE) >= 0 "
            + "AND (SELECT COUNT(*) FROM ACCOUNT) >= 0";
}
