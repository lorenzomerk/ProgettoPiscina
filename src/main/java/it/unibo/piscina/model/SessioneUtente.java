package it.unibo.piscina.model;

import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

/** Identità autenticata esposta all'interfaccia, senza credenziali. */
public record SessioneUtente(
    long accountId,
    Long utenteId,
    String nome,
    String cognome,
    String email,
    RuoloApplicativo ruolo,
    Set<QualificaUtente> qualifiche
) {
    private static final String MIA_SQUADRA = "La mia squadra";
    private static final String ATTIVITA_ASSEGNATE = "Attività assegnate";
    private static final String SQUADRE_SEGUITE = "Squadre seguite";

    public SessioneUtente {
        qualifiche = qualifiche == null ? Set.of() : Set.copyOf(qualifiche);
    }

    public String nomeCompleto() {
        return nome + " " + cognome;
    }

    public boolean haQualifica(final QualificaUtente qualifica) {
        return qualifiche.contains(qualifica);
    }

    public boolean puoAccedere(final String sezione) {
        return sezioniAccessibili().contains(sezione);
    }

    public int numeroSezioni() {
        return sezioniAccessibili().size();
    }

    public Set<String> sezioniAccessibili() {
        final Set<String> sezioni = new HashSet<>(ruolo.sezioni());
        if (haQualifica(QualificaUtente.ATLETA)) {
            sezioni.add(MIA_SQUADRA);
        }
        if (haQualifica(QualificaUtente.ISTRUTTORE)) {
            sezioni.add(ATTIVITA_ASSEGNATE);
            sezioni.add(SQUADRE_SEGUITE);
        }
        return Set.copyOf(sezioni);
    }

    public String descrizioneProfilo() {
        final StringJoiner descrizione = new StringJoiner(" • ");
        descrizione.add(ruolo.descrizione());
        qualifiche.stream()
            .sorted()
            .map(QualificaUtente::descrizione)
            .forEach(descrizione::add);
        return descrizione.toString();
    }
}
