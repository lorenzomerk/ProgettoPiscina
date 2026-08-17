package it.unibo.piscina.model;

import java.util.List;

/** Metadati di una colonna modificabile o visualizzata in un modulo CRUD. */
public record CampoEntita(
    String etichetta,
    String colonna,
    TipoCampo tipo,
    boolean obbligatorio,
    boolean chiave,
    boolean generato,
    List<String> valoriAmmessi,
    String queryRiferimento
) {
    public CampoEntita {
        valoriAmmessi = valoriAmmessi == null
            ? List.of()
            : List.copyOf(valoriAmmessi);
    }

    public static CampoEntita id(final String label, final String column) {
        return new CampoEntita(
            label, column, TipoCampo.INTERO,
            true, true, true, List.of(), null
        );
    }

    public static CampoEntita chiave(
            final String label,
            final String column,
            final String lookupQuery) {

        return new CampoEntita(
            label, column, TipoCampo.RIFERIMENTO,
            true, true, false, List.of(), lookupQuery
        );
    }

    public static CampoEntita riferimento(
            final String label,
            final String column,
            final String lookupQuery) {

        return new CampoEntita(
            label, column, TipoCampo.RIFERIMENTO,
            true, false, false, List.of(), lookupQuery
        );
    }

    public static CampoEntita campo(
            final String label,
            final String column,
            final TipoCampo type,
            final boolean required) {

        return new CampoEntita(
            label, column, type, required,
            false, false, List.of(), null
        );
    }

    public static CampoEntita elenco(
            final String label,
            final String column,
            final String... choices) {

        return new CampoEntita(
            label, column, TipoCampo.ELENCO,
            true, false, false, List.of(choices), null
        );
    }

    public static CampoEntita solaLettura(
            final String label,
            final String alias) {

        return new CampoEntita(
            label, alias, TipoCampo.TESTO,
            false, false, true, List.of(), null
        );
    }
}
