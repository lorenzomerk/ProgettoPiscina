package it.unibo.piscina.service;

/** Errore applicativo mostrabile nel pannello di autenticazione. */
public final class AutenticazioneException extends RuntimeException {

    public AutenticazioneException(final String message) {
        super(message);
    }
}
