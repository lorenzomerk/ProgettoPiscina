package it.unibo.piscina.model;

/** Qualifiche di dominio ricavate dall'anagrafica UTENTE collegata. */
public enum QualificaUtente {
    ATLETA("Atleta"),
    ISTRUTTORE("Istruttore");

    private final String descrizione;

    QualificaUtente(final String descrizione) {
        this.descrizione = descrizione;
    }

    public String descrizione() {
        return descrizione;
    }
}
