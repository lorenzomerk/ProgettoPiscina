package it.unibo.piscina.controller;

import it.unibo.piscina.model.Utente;
import it.unibo.piscina.service.UtenteService;
import java.util.List;
import java.util.Objects;

/** Espone alla vista i casi d'uso relativi agli utenti. */
public final class UtentiController {

    private final UtenteService utenteService;

    public UtentiController(final UtenteService utenteService) {
        this.utenteService = Objects.requireNonNull(utenteService);
    }

    public List<Utente> caricaUtenti() {
        return utenteService.caricaUtenti();
    }

    public long creaUtente(final Utente utente) {
        return utenteService.creaUtente(utente);
    }

    public void modificaUtente(final Utente utente) {
        utenteService.modificaUtente(utente);
    }

    public List<Utente> cercaUtenti(final String keyword) {
        return utenteService.cercaUtenti(keyword);
    }
}
