package it.unibo.piscina.model;

import java.util.Set;

/** Profili applicativi di base, distinti dalle qualifiche del dominio. */
public enum RuoloApplicativo {
    AMMINISTRATORE(
        "Amministratore",
        Set.of(
            "Utenti", "Accessi", "Abbonamenti", "Attività",
            "Struttura", "Club e squadre", "Riepiloghi"
        )
    ),
    CLUB(
        "Club",
        Set.of("Attività", "Club e squadre")
    ),
    UTENTE(
        "Utente / cliente",
        Set.of("Accessi", "Abbonamenti", "Attività")
    );

    private final String descrizione;
    private final Set<String> sezioni;

    RuoloApplicativo(
            final String descrizione,
            final Set<String> sezioni) {

        this.descrizione = descrizione;
        this.sezioni = Set.copyOf(sezioni);
    }

    public String descrizione() {
        return descrizione;
    }

    public boolean puoAccedere(final String sezione) {
        return sezioni.contains(sezione);
    }

    public Set<String> sezioni() {
        return sezioni;
    }

    public int numeroSezioni() {
        return sezioni.size();
    }
}
