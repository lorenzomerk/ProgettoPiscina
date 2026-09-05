package it.unibo.piscina.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unibo.piscina.dao.UtenteDAO;
import it.unibo.piscina.model.Utente;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UtenteServiceTest {

    private InMemoryUtenteDAO dao;
    private UtenteService service;

    @BeforeEach
    void setUp() {
        dao = new InMemoryUtenteDAO();
        service = new UtenteService(dao);
    }

    @Test
    void creaUtenteNormalizzaIValori() {
        final long id = service.creaUtente(new Utente(
            null,
            " rssmra90a01h501u ",
            " Mario ",
            " Rossi ",
            LocalDate.of(1990, 1, 1),
            " MARIO.ROSSI@EXAMPLE.COM ",
            " 3331234567 ",
            null
        ));

        assertEquals(1L, id);
        final Utente saved = dao.utenti.getFirst();
        assertEquals("RSSMRA90A01H501U", saved.codiceFiscale());
        assertEquals("Mario", saved.nome());
        assertEquals("mario.rossi@example.com", saved.email());
        assertEquals("3331234567", saved.telefono());
    }

    @Test
    void rifiutaCodiceFiscaleNonValido() {
        final IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> service.creaUtente(validUser(null, "ABC"))
        );

        assertTrue(exception.getMessage().contains("16 caratteri"));
        assertTrue(dao.utenti.isEmpty());
    }

    @Test
    void rifiutaDataDiNascitaFutura() {
        final Utente input = new Utente(
            null,
            "RSSMRA90A01H501U",
            "Mario",
            "Rossi",
            LocalDate.now().plusDays(1),
            null,
            null,
            null
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> service.creaUtente(input)
        );
    }

    @Test
    void modificaUtenteEsistente() {
        dao.utenti.add(validUser(7L, "RSSMRA90A01H501U"));
        final Utente modified = new Utente(
            7L,
            "RSSMRA90A01H501U",
            "Maria",
            "Rossi",
            LocalDate.of(1990, 1, 1),
            "maria@example.com",
            null,
            LocalDate.of(2027, 6, 30)
        );

        service.modificaUtente(modified);

        assertEquals("Maria", dao.utenti.getFirst().nome());
        assertEquals(
            LocalDate.of(2027, 6, 30),
            dao.utenti.getFirst().scadenzaCertificatoMedico()
        );
    }

    @Test
    void gestisceQualificheParzialiESovrapposte() {
        final Utente input = new Utente(
            null,
            "BNCDRA90D01H501W",
            "Andrea",
            "Completo",
            LocalDate.of(1990, 4, 1),
            "andrea@example.com",
            null,
            null,
            true,
            true,
            " Allenatore e istruttore "
        );

        service.creaUtente(input);

        final Utente saved = dao.utenti.getFirst();
        assertTrue(saved.atleta());
        assertTrue(saved.istruttore());
        assertEquals(
            "Allenatore e istruttore",
            saved.qualificaIstruttore()
        );
    }

    @Test
    void richiedeQualificaQuandoUtenteEIstruttore() {
        final Utente input = new Utente(
            null,
            "VSTRVI80A01H501X",
            "Ivo",
            "Istruttore",
            LocalDate.of(1980, 1, 1),
            null,
            null,
            null,
            false,
            true,
            " "
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> service.creaUtente(input)
        );
    }

    private Utente validUser(final Long id, final String fiscalCode) {
        return new Utente(
            id,
            fiscalCode,
            "Mario",
            "Rossi",
            LocalDate.of(1990, 1, 1),
            "mario@example.com",
            null,
            null
        );
    }

    private static final class InMemoryUtenteDAO implements UtenteDAO {

        private final List<Utente> utenti = new ArrayList<>();
        private long nextId = 1;

        @Override
        public List<Utente> findAll() {
            return List.copyOf(utenti);
        }

        @Override
        public List<Utente> search(final String name) {
            final String normalized = name == null
                ? ""
                : name.trim().toLowerCase();
            return utenti.stream()
                .filter(utente -> (
                    utente.nome() + " " + utente.cognome()
                ).toLowerCase().contains(normalized))
                .toList();
        }

        @Override
        public long insert(final Utente utente) {
            final long id = nextId++;
            utenti.add(copyWithId(utente, id));
            return id;
        }

        @Override
        public boolean update(final Utente utente) {
            for (int index = 0; index < utenti.size(); index++) {
                if (utenti.get(index).id().equals(utente.id())) {
                    utenti.set(index, utente);
                    return true;
                }
            }
            return false;
        }

        private Utente copyWithId(final Utente source, final long id) {
            return new Utente(
                id,
                source.codiceFiscale(),
                source.nome(),
                source.cognome(),
                source.dataNascita(),
                source.email(),
                source.telefono(),
                source.scadenzaCertificatoMedico(),
                source.atleta(),
                source.istruttore(),
                source.qualificaIstruttore(),
                source.dataRegistrazione()
            );
        }
    }
}
