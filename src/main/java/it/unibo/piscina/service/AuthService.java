package it.unibo.piscina.service;

import it.unibo.piscina.dao.AccountDAO;
import it.unibo.piscina.dao.RegistrazioneDAO;
import it.unibo.piscina.model.AccountAutenticazione;
import it.unibo.piscina.model.RuoloApplicativo;
import it.unibo.piscina.model.SessioneUtente;
import it.unibo.piscina.service.security.PasswordHasher;
import it.unibo.piscina.service.security.PasswordHasher.PasswordHash;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** Casi d'uso di accesso e registrazione. */
public final class AuthService {

    private static final Pattern EMAIL = Pattern.compile(
        "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
    );
    private static final Pattern CODICE_FISCALE = Pattern.compile(
        "^[A-Z0-9]{16}$"
    );

    private final AccountDAO accountDAO;
    private final RegistrazioneDAO registrazioneDAO;
    private final PasswordHasher passwordHasher;

    public AuthService(
            final AccountDAO accountDAO,
            final RegistrazioneDAO registrazioneDAO,
            final PasswordHasher passwordHasher) {

        this.accountDAO = Objects.requireNonNull(accountDAO);
        this.registrazioneDAO = Objects.requireNonNull(registrazioneDAO);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
    }

    public SessioneUtente accedi(
            final String rawEmail,
            final char[] password) {

        final String email = normalizeEmail(rawEmail);
        if (password == null || password.length == 0) {
            throw new AutenticazioneException(
                "Inserisci email e password"
            );
        }

        final AccountAutenticazione account = accountDAO
            .findByEmail(email)
            .orElseThrow(() -> new AutenticazioneException(
                "Credenziali non valide"
            ));
        if (!account.attivo()) {
            throw new AutenticazioneException("Account disattivato");
        }
        if (!passwordHasher.verify(
                password,
                account.passwordHash(),
                account.passwordSalt(),
                account.passwordIterazioni())) {

            throw new AutenticazioneException("Credenziali non valide");
        }
        accountDAO.recordSuccessfulAccess(account.id());
        return toSession(account);
    }

    public SessioneUtente registra(
            final String rawName,
            final String rawSurname,
            final String rawFiscalCode,
            final String rawBirthDate,
            final String rawEmail,
            final char[] password,
            final char[] confirmation) {

        final String name = required(rawName, "Nome", 80);
        final String surname = required(rawSurname, "Cognome", 80);
        final String fiscalCode = normalizeFiscalCode(rawFiscalCode);
        final LocalDate birthDate = parseBirthDate(rawBirthDate);
        final String email = normalizeEmail(rawEmail);
        validatePassword(password, confirmation);

        if (accountDAO.findByEmail(email).isPresent()) {
            throw new AutenticazioneException("Email già registrata");
        }

        final PasswordHash protectedPassword = passwordHasher.hash(password);
        final AccountAutenticazione account = new AccountAutenticazione(
            null,
            null,
            null,
            name,
            surname,
            email,
            protectedPassword.hash(),
            protectedPassword.salt(),
            protectedPassword.iterations(),
            RuoloApplicativo.UTENTE,
            Set.of(),
            true
        );
        final RegistrazioneDAO.RegistrazioneCreata registration =
            registrazioneDAO.insert(fiscalCode, birthDate, account);
        return new SessioneUtente(
            registration.accountId(),
            registration.utenteId(),
            null,
            name,
            surname,
            email,
            RuoloApplicativo.UTENTE,
            Set.of()
        );
    }

    private String normalizeFiscalCode(final String value) {
        final String fiscalCode = required(value, "Codice fiscale", 16)
            .toUpperCase(Locale.ROOT);
        if (!CODICE_FISCALE.matcher(fiscalCode).matches()) {
            throw new AutenticazioneException(
                "Il codice fiscale deve contenere 16 lettere o cifre"
            );
        }
        return fiscalCode;
    }

    private LocalDate parseBirthDate(final String value) {
        final String normalized = required(
            value,
            "Data di nascita",
            10
        );
        try {
            final LocalDate birthDate = LocalDate.parse(normalized);
            if (birthDate.isAfter(LocalDate.now())) {
                throw new AutenticazioneException(
                    "La data di nascita non può essere futura"
                );
            }
            return birthDate;
        } catch (DateTimeParseException exception) {
            throw new AutenticazioneException(
                "Data di nascita non valida: usa il formato AAAA-MM-GG"
            );
        }
    }

    private void validatePassword(
            final char[] password,
            final char[] confirmation) {

        if (password == null || confirmation == null
                || password.length == 0 || confirmation.length == 0) {
            throw new AutenticazioneException(
                "Password e conferma sono obbligatorie"
            );
        }
        if (!java.util.Arrays.equals(password, confirmation)) {
            throw new AutenticazioneException(
                "Le password non coincidono"
            );
        }
        if (password.length < 8) {
            throw new AutenticazioneException(
                "La password deve avere almeno 8 caratteri"
            );
        }

        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        for (char character : password) {
            upper |= Character.isUpperCase(character);
            lower |= Character.isLowerCase(character);
            digit |= Character.isDigit(character);
        }
        if (!upper || !lower || !digit) {
            throw new AutenticazioneException(
                "La password deve includere maiuscola, minuscola e numero"
            );
        }
    }

    private String normalizeEmail(final String value) {
        final String email = required(value, "Email", 255)
            .toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(email).matches()) {
            throw new AutenticazioneException("Indirizzo email non valido");
        }
        return email;
    }

    private String required(
            final String value,
            final String field,
            final int maximumLength) {

        final String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new AutenticazioneException(field + " è obbligatorio");
        }
        if (normalized.length() > maximumLength) {
            throw new AutenticazioneException(field + " troppo lungo");
        }
        return normalized;
    }

    private SessioneUtente toSession(
            final AccountAutenticazione account) {

        return new SessioneUtente(
            account.id(),
            account.utenteId(),
            account.clubId(),
            account.nome(),
            account.cognome(),
            account.email(),
            account.ruolo(),
            account.qualifiche()
        );
    }
}
