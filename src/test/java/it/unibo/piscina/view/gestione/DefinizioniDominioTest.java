package it.unibo.piscina.view.gestione;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unibo.piscina.model.RuoloApplicativo;
import it.unibo.piscina.model.SessioneUtente;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class DefinizioniDominioTest {

    @Test
    void amministratoreTrovaTuttiIModuliConcettuali() {
        final SessioneUtente admin = session(
            RuoloApplicativo.AMMINISTRATORE,
            null
        );

        assertEquals(
            3,
            DefinizioniDominio.perSezione("Abbonamenti", admin).size()
        );
        assertEquals(
            4,
            DefinizioniDominio.perSezione("Attività", admin).size()
        );
        assertEquals(
            3,
            DefinizioniDominio.perSezione("Struttura", admin).size()
        );
        assertEquals(
            5,
            DefinizioniDominio.perSezione(
                "Club e squadre",
                admin
            ).size()
        );
    }

    @Test
    void vistaUtenteFiltraIPropriDatiMaConsenteLeOperazioniIndividuali() {
        final SessioneUtente user = session(
            RuoloApplicativo.UTENTE,
            42L
        );
        final var subscriptions = DefinizioniDominio.perSezione(
            "Abbonamenti",
            user
        ).getFirst();
        final var registrations = DefinizioniDominio.perSezione(
            "Attività",
            user
        ).get(1);
        final var accesses = DefinizioniDominio.perSezione(
            "Accessi",
            user
        ).getFirst();

        assertTrue(subscriptions.querySelezione().contains(
            "a.ID_Utente = 42"
        ));
        assertTrue(subscriptions.inseribile());
        assertTrue(registrations.inseribile());
        assertTrue(accesses.inseribile());
    }

    @Test
    void ogniQueryRestituisceUnaColonnaPerCampoDichiarato() {
        final SessioneUtente admin = session(
            RuoloApplicativo.AMMINISTRATORE,
            null
        );
        Stream.of(
            "Abbonamenti",
            "Attività",
            "Accessi",
            "Struttura",
            "Club e squadre"
        ).flatMap(section ->
            DefinizioniDominio.perSezione(section, admin).stream()
        ).forEach(definition -> assertEquals(
            definition.campi().size(),
            selectColumnCount(definition.querySelezione()),
            definition.titolo()
        ));
    }

    private SessioneUtente session(
            final RuoloApplicativo role,
            final Long userId) {

        return new SessioneUtente(
            1L,
            userId,
            "Nome",
            "Cognome",
            "utente@example.com",
            role,
            Set.of()
        );
    }

    private int selectColumnCount(final String sql) {
        final String upper = sql.toUpperCase();
        final int start = upper.indexOf("SELECT") + "SELECT".length();
        final int end = upper.indexOf("FROM", start);
        final String selection = sql.substring(start, end);
        int count = 1;
        int depth = 0;
        boolean quoted = false;
        for (int index = 0; index < selection.length(); index++) {
            final char current = selection.charAt(index);
            if (current == '\''
                    && (index == 0 || selection.charAt(index - 1) != '\\')) {
                quoted = !quoted;
            } else if (!quoted && current == '(') {
                depth++;
            } else if (!quoted && current == ')') {
                depth--;
            } else if (!quoted && depth == 0 && current == ',') {
                count++;
            }
        }
        return count;
    }
}
