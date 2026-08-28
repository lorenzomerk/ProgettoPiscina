package it.unibo.piscina.model;

import java.util.Collections;
import java.util.List;

/** Risultato tabellare indipendente da Swing e JDBC. */
public record DatiTabella(
    List<String> colonne,
    List<List<Object>> righe
) {
    public DatiTabella {
        colonne = List.copyOf(colonne);
        righe = righe.stream().map(Collections::unmodifiableList).toList();
    }
}
