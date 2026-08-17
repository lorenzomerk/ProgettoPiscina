package it.unibo.piscina.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class RuoloApplicativoTest {

    @Test
    void amministratoreAccedeATutteLeAree() {
        final RuoloApplicativo role = RuoloApplicativo.AMMINISTRATORE;

        assertTrue(role.puoAccedere("Utenti"));
        assertTrue(role.puoAccedere("Accessi"));
        assertTrue(role.puoAccedere("Abbonamenti"));
        assertTrue(role.puoAccedere("Attività"));
        assertTrue(role.puoAccedere("Struttura"));
        assertTrue(role.puoAccedere("Club e squadre"));
        assertTrue(role.puoAccedere("Riepiloghi"));
    }

    @Test
    void utenteNonAccedeAAnagraficheOStruttura() {
        final RuoloApplicativo role = RuoloApplicativo.UTENTE;

        assertFalse(role.puoAccedere("Utenti"));
        assertFalse(role.puoAccedere("Struttura"));
        assertTrue(role.puoAccedere("Abbonamenti"));
        assertTrue(role.puoAccedere("Attività"));
    }

    @Test
    void clubAccedeSoloAlleAreeDellaPropriaVista() {
        final RuoloApplicativo role = RuoloApplicativo.CLUB;

        assertTrue(role.puoAccedere("Attività"));
        assertTrue(role.puoAccedere("Club e squadre"));
        assertFalse(role.puoAccedere("Utenti"));
        assertFalse(role.puoAccedere("Abbonamenti"));
        assertFalse(role.puoAccedere("Struttura"));
        assertFalse(role.puoAccedere("Riepiloghi"));
    }

    @Test
    void qualificheEstendonoLaVistaUtenteSenzaCambiareProfilo() {
        final SessioneUtente session = new SessioneUtente(
            1L,
            20L,
            "Andrea",
            "Completo",
            "andrea@example.com",
            RuoloApplicativo.UTENTE,
            Set.of(QualificaUtente.ATLETA, QualificaUtente.ISTRUTTORE)
        );

        assertTrue(session.puoAccedere("Abbonamenti"));
        assertTrue(session.puoAccedere("La mia squadra"));
        assertTrue(session.puoAccedere("Attività assegnate"));
        assertTrue(session.puoAccedere("Squadre seguite"));
        assertFalse(session.puoAccedere("Club e squadre"));
    }
}
