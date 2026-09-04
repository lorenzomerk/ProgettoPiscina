package it.unibo.piscina.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unibo.piscina.model.RuoloApplicativo;
import it.unibo.piscina.model.SessioneUtente;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class CatalogoDominioTest {

    @Test
    void amministratoreTrovaTuttiIModuliConcettuali() {
        final SessioneUtente admin = session(
            RuoloApplicativo.AMMINISTRATORE,
            null
        );

        assertEquals(
            3,
            CatalogoDominio.perSezione("Abbonamenti", admin).size()
        );
        assertEquals(
            4,
            CatalogoDominio.perSezione("Attività", admin).size()
        );
        assertEquals(
            3,
            CatalogoDominio.perSezione("Struttura", admin).size()
        );
        assertEquals(
            5,
            CatalogoDominio.perSezione(
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
        final var subscriptions = CatalogoDominio.perSezione(
            "Abbonamenti",
            user
        ).getFirst();
        final var registrations = CatalogoDominio.perSezione(
            "Attività",
            user
        ).get(1);
        final var accesses = CatalogoDominio.perSezione(
            "Accessi",
            user
        ).getFirst();

        assertTrue(subscriptions.querySelezione().contains(
            "a.ID_Utente = 42"
        ));
        assertTrue(subscriptions.inseribile());
        assertTrue(registrations.inseribile());
        assertFalse(registrations.eliminabile());
        assertTrue(accesses.inseribile());
    }

    @Test
    void vistaClubUsaIdentificativoENonEsponeIscrizioniIndividuali() {
        final SessioneUtente club = new SessioneUtente(
            3L,
            null,
            17L,
            "Nuoto",
            "Emilia",
            "email-modificabile@example.com",
            RuoloApplicativo.CLUB,
            Set.of()
        );

        final var activities = CatalogoDominio.perSezione(
            "Attività",
            club
        );
        assertEquals(2, activities.size());
        assertTrue(activities.get(1).querySelezione().contains(
            "s.ID_Club = 17"
        ));
        assertTrue(activities.stream().noneMatch(definition ->
            definition.tabella().equals("ISCRIZIONE_ATTIVITA")
        ));
        assertTrue(activities.stream().allMatch(definition ->
            !definition.inseribile()
                && !definition.modificabile()
                && !definition.eliminabile()
        ));

        final var clubData = CatalogoDominio.perSezione(
            "Club e squadre",
            club
        );
        assertTrue(clubData.stream().allMatch(definition ->
            definition.querySelezione().contains("ID_Club = 17")
        ));
        assertTrue(clubData.stream().noneMatch(definition ->
            definition.querySelezione().contains(club.email())
        ));
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
            CatalogoDominio.perSezione(section, admin).stream()
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
            null,
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
