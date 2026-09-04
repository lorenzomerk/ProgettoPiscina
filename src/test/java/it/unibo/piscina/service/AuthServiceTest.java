package it.unibo.piscina.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unibo.piscina.dao.AccountDAO;
import it.unibo.piscina.dao.RegistrazioneDAO;
import it.unibo.piscina.model.AccountAutenticazione;
import it.unibo.piscina.model.QualificaUtente;
import it.unibo.piscina.model.RuoloApplicativo;
import it.unibo.piscina.model.SessioneUtente;
import it.unibo.piscina.service.security.PasswordHasher;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthServiceTest {

    private InMemoryAccountDAO dao;
    private PasswordHasher hasher;
    private AuthService service;

    @BeforeEach
    void setUp() {
        dao = new InMemoryAccountDAO();
        hasher = new PasswordHasher();
        service = new AuthService(dao, dao, hasher);
    }

    @Test
    void registrazioneCreaSempreUnUtenteConPasswordProtetta() {
        final SessioneUtente session = service.registra(
            " Mario ",
            " Rossi ",
            " rssmra90a01h501u ",
            "1990-01-01",
            " MARIO@EXAMPLE.COM ",
            "Cliente123!".toCharArray(),
            "Cliente123!".toCharArray()
        );

        assertEquals(RuoloApplicativo.UTENTE, session.ruolo());
        assertNotNull(session.utenteId());
        assertEquals(1L, session.utenteId());
        assertTrue(session.qualifiche().isEmpty());
        assertEquals("mario@example.com", session.email());
        final AccountAutenticazione saved = dao.accounts.getFirst();
        assertEquals(session.utenteId(), saved.utenteId());
        assertNotEquals("Cliente123!", saved.passwordHash());
        assertTrue(saved.passwordIterazioni() >= 100_000);
    }

    @Test
    void accessoRifiutaUnAccountDisattivato() {
        final AccountAutenticazione active = account(
            4L,
            "inactive@example.com",
            "Cliente123!",
            RuoloApplicativo.UTENTE
        );
        dao.accounts.add(new AccountAutenticazione(
            active.id(),
            active.utenteId(),
            active.clubId(),
            active.nome(),
            active.cognome(),
            active.email(),
            active.passwordHash(),
            active.passwordSalt(),
            active.passwordIterazioni(),
            active.ruolo(),
            active.qualifiche(),
            false
        ));

        assertThrows(
            AutenticazioneException.class,
            () -> service.accedi(
                "inactive@example.com",
                "Cliente123!".toCharArray()
            )
        );
    }

    @Test
    void accessoClubPropagaIlCollegamentoStabile() {
        final var protectedPassword =
            hasher.hash("Club123!".toCharArray());
        dao.accounts.add(new AccountAutenticazione(
            8L,
            null,
            77L,
            "Nuoto",
            "Emilia",
            "club@example.com",
            protectedPassword.hash(),
            protectedPassword.salt(),
            protectedPassword.iterations(),
            RuoloApplicativo.CLUB,
            Set.of(),
            true
        ));

        final SessioneUtente session = service.accedi(
            "club@example.com",
            "Club123!".toCharArray()
        );

        assertEquals(77L, session.clubId());
    }

    @Test
    void accessoRestituisceIlRuoloPersistito() {
        dao.accounts.add(account(
            9L,
            "admin@piscina.local",
            "Admin123!",
            RuoloApplicativo.AMMINISTRATORE
        ));

        final SessioneUtente session = service.accedi(
            "ADMIN@PISCINA.LOCAL",
            "Admin123!".toCharArray()
        );

        assertEquals(RuoloApplicativo.AMMINISTRATORE, session.ruolo());
        assertEquals(9L, session.accountId());
        assertEquals(9L, dao.lastSuccessfulAccess);
    }

    @Test
    void accessoRifiutaPasswordErrata() {
        dao.accounts.add(account(
            2L,
            "cliente@piscina.local",
            "Cliente123!",
            RuoloApplicativo.UTENTE
        ));

        assertThrows(
            AutenticazioneException.class,
            () -> service.accedi(
                "cliente@piscina.local",
                "Errata123!".toCharArray()
            )
        );
    }

    @Test
    void accessoPropagaIdentitaEQualificheDelDominio() {
        final var protectedPassword =
            hasher.hash("Completo123!".toCharArray());
        dao.accounts.add(new AccountAutenticazione(
            12L,
            44L,
            null,
            "Andrea",
            "Completo",
            "andrea@example.com",
            protectedPassword.hash(),
            protectedPassword.salt(),
            protectedPassword.iterations(),
            RuoloApplicativo.UTENTE,
            Set.of(QualificaUtente.ATLETA, QualificaUtente.ISTRUTTORE),
            true
        ));

        final SessioneUtente session = service.accedi(
            "andrea@example.com",
            "Completo123!".toCharArray()
        );

        assertEquals(44L, session.utenteId());
        assertTrue(session.haQualifica(QualificaUtente.ATLETA));
        assertTrue(session.haQualifica(QualificaUtente.ISTRUTTORE));
    }

    @Test
    void registrazioneRichiedePasswordConfermataERobusta() {
        assertThrows(
            AutenticazioneException.class,
            () -> service.registra(
                "Mario", "Rossi", "RSSMRA90A01H501U",
                "1990-01-01", "mario@example.com",
                "debole".toCharArray(),
                "debole".toCharArray()
            )
        );
        assertThrows(
            AutenticazioneException.class,
            () -> service.registra(
                "Mario", "Rossi", "RSSMRA90A01H501U",
                "1990-01-01", "mario@example.com",
                "Cliente123!".toCharArray(),
                "Diversa123!".toCharArray()
            )
        );
    }

    private AccountAutenticazione account(
            final long id,
            final String email,
            final String password,
            final RuoloApplicativo role) {

        final var protectedPassword =
            hasher.hash(password.toCharArray());
        return new AccountAutenticazione(
            id,
            role == RuoloApplicativo.UTENTE ? id + 100 : null,
            null,
            "Nome",
            "Cognome",
            email,
            protectedPassword.hash(),
            protectedPassword.salt(),
            protectedPassword.iterations(),
            role,
            Set.of(),
            true
        );
    }

    private static final class InMemoryAccountDAO
            implements AccountDAO, RegistrazioneDAO {

        private final List<AccountAutenticazione> accounts =
            new ArrayList<>();
        private long nextId = 1;
        private long lastSuccessfulAccess;

        @Override
        public Optional<AccountAutenticazione> findByEmail(
                final String email) {

            return accounts.stream()
                .filter(account -> account.email().equals(email))
                .findFirst();
        }

        @Override
        public RegistrazioneCreata insert(
                final String codiceFiscale,
                final LocalDate dataNascita,
                final AccountAutenticazione account) {

            final long utenteId = nextId++;
            final long accountId = nextId++;
            accounts.add(new AccountAutenticazione(
                accountId,
                utenteId,
                null,
                account.nome(),
                account.cognome(),
                account.email(),
                account.passwordHash(),
                account.passwordSalt(),
                account.passwordIterazioni(),
                account.ruolo(),
                account.qualifiche(),
                account.attivo()
            ));
            return new RegistrazioneCreata(accountId, utenteId);
        }

        @Override
        public void recordSuccessfulAccess(final long accountId) {
            lastSuccessfulAccess = accountId;
        }
    }
}
