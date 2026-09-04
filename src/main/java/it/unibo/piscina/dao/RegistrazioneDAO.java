package it.unibo.piscina.dao;

import it.unibo.piscina.model.AccountAutenticazione;
import java.time.LocalDate;

/** Crea atomicamente l'anagrafica di dominio e il relativo account. */
public interface RegistrazioneDAO {

    RegistrazioneCreata insert(
        String codiceFiscale,
        LocalDate dataNascita,
        AccountAutenticazione account
    );

    /** Identificativi generati dalla registrazione completata. */
    record RegistrazioneCreata(long accountId, long utenteId) {
    }
}
