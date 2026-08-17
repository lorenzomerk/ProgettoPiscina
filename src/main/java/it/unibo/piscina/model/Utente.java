package it.unibo.piscina.model;

import java.time.LocalDate;

/** Anagrafica di un iscritto al centro natatorio. */
public record Utente(
    Long id,
    String codiceFiscale,
    String nome,
    String cognome,
    LocalDate dataNascita,
    String email,
    String telefono,
    LocalDate scadenzaCertificatoMedico,
    boolean attivo,
    boolean atleta,
    boolean istruttore,
    String qualificaIstruttore,
    LocalDate dataRegistrazione
) {
    /** Costruttore compatibile per un utente senza qualifiche di dominio. */
    public Utente(
            final Long id,
            final String codiceFiscale,
            final String nome,
            final String cognome,
            final LocalDate dataNascita,
            final String email,
            final String telefono,
            final LocalDate scadenzaCertificatoMedico,
            final boolean attivo) {

        this(
            id,
            codiceFiscale,
            nome,
            cognome,
            dataNascita,
            email,
            telefono,
            scadenzaCertificatoMedico,
            attivo,
            false,
            false,
            null,
            LocalDate.now()
        );
    }

    /** Costruttore con qualifiche e data di registrazione odierna. */
    public Utente(
            final Long id,
            final String codiceFiscale,
            final String nome,
            final String cognome,
            final LocalDate dataNascita,
            final String email,
            final String telefono,
            final LocalDate scadenzaCertificatoMedico,
            final boolean attivo,
            final boolean atleta,
            final boolean istruttore,
            final String qualificaIstruttore) {

        this(
            id,
            codiceFiscale,
            nome,
            cognome,
            dataNascita,
            email,
            telefono,
            scadenzaCertificatoMedico,
            attivo,
            atleta,
            istruttore,
            qualificaIstruttore,
            LocalDate.now()
        );
    }
}
