package it.unibo.piscina.controller;

import it.unibo.piscina.model.SessioneUtente;
import it.unibo.piscina.service.AuthService;
import java.util.Objects;

/** Controller delle operazioni di autenticazione. */
public final class AuthController {

    private final AuthService authService;

    public AuthController(final AuthService authService) {
        this.authService = Objects.requireNonNull(authService);
    }

    public SessioneUtente accedi(
            final String email,
            final char[] password) {

        return authService.accedi(email, password);
    }

    public SessioneUtente registra(
            final String nome,
            final String cognome,
            final String email,
            final char[] password,
            final char[] conferma) {

        return authService.registra(
            nome,
            cognome,
            email,
            password,
            conferma
        );
    }
}
