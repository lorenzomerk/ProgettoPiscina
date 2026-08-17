package it.unibo.piscina.model;

import java.util.List;

/** Descrive una tabella o vista esposta in uno dei moduli applicativi. */
public record DefinizioneEntita(
    String titolo,
    String descrizione,
    String tabella,
    String querySelezione,
    List<CampoEntita> campi,
    boolean inseribile,
    boolean modificabile,
    boolean eliminabile
) {
    public DefinizioneEntita {
        campi = List.copyOf(campi);
    }
}
