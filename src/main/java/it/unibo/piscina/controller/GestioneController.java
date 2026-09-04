package it.unibo.piscina.controller;

import it.unibo.piscina.model.DatiTabella;
import it.unibo.piscina.model.DefinizioneEntita;
import it.unibo.piscina.model.OpzioneRiferimento;
import it.unibo.piscina.model.SessioneUtente;
import it.unibo.piscina.service.CatalogoDominio;
import it.unibo.piscina.service.GestioneService;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** Espone alla GUI i casi d'uso trasversali del dominio piscina. */
public final class GestioneController {

    private final GestioneService service;

    public GestioneController(final GestioneService service) {
        this.service = Objects.requireNonNull(service);
    }

    public DatiTabella carica(final DefinizioneEntita definition) {
        return service.carica(definition);
    }

    public List<DefinizioneEntita> definizioniPerSezione(
            final String section,
            final SessioneUtente session) {

        return CatalogoDominio.perSezione(section, session);
    }

    public List<OpzioneRiferimento> caricaOpzioni(final String sql) {
        return service.caricaOpzioni(sql);
    }

    public void inserisci(
            final DefinizioneEntita definition,
            final List<Object> values) {

        service.inserisci(definition, values);
    }

    public void modifica(
            final DefinizioneEntita definition,
            final List<Object> originalRow,
            final List<Object> values) {

        service.modifica(definition, originalRow, values);
    }

    public void elimina(
            final DefinizioneEntita definition,
            final List<Object> originalRow) {

        service.elimina(definition, originalRow);
    }

    public List<String> nomiRiepiloghi() {
        return service.nomiRiepiloghi();
    }

    public DatiTabella eseguiRiepilogo(
            final int indice,
            final LocalDate inizio,
            final LocalDate fine) {

        return service.eseguiRiepilogo(indice, inizio, fine);
    }
}
