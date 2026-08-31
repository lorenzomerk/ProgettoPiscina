package it.unibo.piscina.service;

import it.unibo.piscina.dao.UtenteDAO;
import it.unibo.piscina.model.Utente;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Regole applicative per la gestione delle anagrafiche. */
public final class UtenteService {

    private static final Pattern CODICE_FISCALE =
        Pattern.compile("[A-Z0-9]{16}");
    private static final Pattern EMAIL = Pattern.compile(
        "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
    );

    private final UtenteDAO utenteDAO;

    public UtenteService(final UtenteDAO utenteDAO) {
        this.utenteDAO = Objects.requireNonNull(utenteDAO);
    }

    public List<Utente> caricaUtenti() {
        return List.copyOf(utenteDAO.findAll());
    }

    public long creaUtente(final Utente input) {
        final Utente utente = normalizeAndValidate(input, false);
        return utenteDAO.insert(utente);
    }

    public void modificaUtente(final Utente input) {
        final Utente utente = normalizeAndValidate(input, true);
        if (!utenteDAO.update(utente)) {
            throw new IllegalArgumentException(
                "L'utente da modificare non esiste più"
            );
        }
    }

    public void disattivaUtente(final long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Identificativo non valido");
        }
        if (!utenteDAO.deactivate(id)) {
            throw new IllegalArgumentException(
                "L'utente da disattivare non esiste più"
            );
        }
    }

    private Utente normalizeAndValidate(
            final Utente input,
            final boolean requireId) {

        Objects.requireNonNull(input, "Utente mancante");
        if (requireId && (input.id() == null || input.id() <= 0)) {
            throw new IllegalArgumentException(
                "Identificativo utente non valido"
            );
        }

        final String fiscalCode = required(
            input.codiceFiscale(),
            "Codice fiscale"
        ).replaceAll("\\s", "").toUpperCase(Locale.ROOT);
        if (!CODICE_FISCALE.matcher(fiscalCode).matches()) {
            throw new IllegalArgumentException(
                "Il codice fiscale deve contenere 16 caratteri alfanumerici"
            );
        }

        final String name = required(input.nome(), "Nome");
        final String surname = required(input.cognome(), "Cognome");
        if (input.dataNascita() == null) {
            throw new IllegalArgumentException(
                "La data di nascita è obbligatoria"
            );
        }
        if (input.dataNascita().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                "La data di nascita non può essere futura"
            );
        }
        final LocalDate registrationDate =
            input.dataRegistrazione() == null
                ? LocalDate.now()
                : input.dataRegistrazione();
        if (registrationDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                "La data di registrazione non può essere futura"
            );
        }
        if (registrationDate.isBefore(input.dataNascita())) {
            throw new IllegalArgumentException(
                "La data di registrazione non può precedere la nascita"
            );
        }

        final String email = optional(input.email()).toLowerCase(Locale.ROOT);
        if (!email.isEmpty() && !EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("Indirizzo email non valido");
        }

        final String instructorQualification =
            nullable(input.qualificaIstruttore());
        if (input.istruttore() && instructorQualification == null) {
            throw new IllegalArgumentException(
                "La qualifica è obbligatoria per un istruttore"
            );
        }

        return new Utente(
            input.id(),
            fiscalCode,
            name,
            surname,
            input.dataNascita(),
            email.isEmpty() ? null : email,
            nullable(input.telefono()),
            input.scadenzaCertificatoMedico(),
            input.attivo(),
            input.atleta(),
            input.istruttore(),
            input.istruttore() ? instructorQualification : null,
            registrationDate
        );
    }

    private String required(final String value, final String fieldName) {
        final String normalized = optional(value);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                fieldName + " è obbligatorio"
            );
        }
        return normalized;
    }

    private String optional(final String value) {
        return value == null ? "" : value.trim();
    }

    private String nullable(final String value) {
        final String normalized = optional(value);
        return normalized.isEmpty() ? null : normalized;
    }

    public List<Utente> cercaUtenti(final String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return caricaUtenti();
        }
        return List.copyOf(utenteDAO.search(keyword.trim()));
    }
}
