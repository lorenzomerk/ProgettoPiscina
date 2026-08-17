package it.unibo.piscina.model;

/** Valore e descrizione leggibile di una chiave esterna. */
public record OpzioneRiferimento(Object valore, String descrizione) {
    @Override
    public String toString() {
        return descrizione;
    }
}
