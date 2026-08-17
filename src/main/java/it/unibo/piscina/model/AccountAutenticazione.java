package it.unibo.piscina.model;

import java.util.Set;

/** Account persistente, comprensivo dei dati necessari alla verifica. */
public record AccountAutenticazione(
    Long id,
    Long utenteId,
    String nome,
    String cognome,
    String email,
    String passwordHash,
    String passwordSalt,
    int passwordIterazioni,
    RuoloApplicativo ruolo,
    Set<QualificaUtente> qualifiche,
    boolean attivo
) {
    public AccountAutenticazione {
        qualifiche = qualifiche == null ? Set.of() : Set.copyOf(qualifiche);
    }
}
