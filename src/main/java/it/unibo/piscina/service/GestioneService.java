package it.unibo.piscina.service;

import it.unibo.piscina.dao.GestioneDAO;
import it.unibo.piscina.model.CampoEntita;
import it.unibo.piscina.model.DatiTabella;
import it.unibo.piscina.model.DefinizioneEntita;
import it.unibo.piscina.model.OpzioneRiferimento;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/** Compone in modo controllato le operazioni CRUD descritte dai metadati. */
public final class GestioneService {

    private final GestioneDAO dao;

    public GestioneService(final GestioneDAO dao) {
        this.dao = Objects.requireNonNull(dao);
    }

    public DatiTabella carica(final DefinizioneEntita definition) {
        dao.sincronizzaStati();
        return dao.query(definition.querySelezione());
    }

    public List<OpzioneRiferimento> caricaOpzioni(final String sql) {
        return List.copyOf(dao.caricaOpzioni(sql));
    }

    public void inserisci(
            final DefinizioneEntita definition,
            final List<Object> values) {

        ensureAllowed(definition.inseribile(), "Inserimento non consentito");
        final List<CampoEntita> fields = definition.campi().stream()
            .filter(field -> !field.generato())
            .toList();
        if (fields.size() != values.size()) {
            throw new IllegalArgumentException("Dati del form incompleti");
        }

        final StringJoiner columns = new StringJoiner(", ");
        final StringJoiner placeholders = new StringJoiner(", ");
        fields.forEach(field -> {
            columns.add(field.colonna());
            placeholders.add("?");
        });
        final String sql = "INSERT INTO " + definition.tabella()
            + " (" + columns + ") VALUES (" + placeholders + ")";
        dao.execute(sql, values);
    }

    public void modifica(
            final DefinizioneEntita definition,
            final List<Object> originalRow,
            final List<Object> values) {

        ensureAllowed(definition.modificabile(), "Modifica non consentita");
        final List<CampoEntita> editable = definition.campi().stream()
            .filter(field -> !field.generato() && !field.chiave())
            .toList();
        if (editable.size() != values.size()) {
            throw new IllegalArgumentException("Dati del form incompleti");
        }

        final StringJoiner assignments = new StringJoiner(", ");
        editable.forEach(field ->
            assignments.add(field.colonna() + " = ?"));
        final StringJoiner where = new StringJoiner(" AND ");
        final List<Object> parameters = new ArrayList<>(values);
        for (int index = 0; index < definition.campi().size(); index++) {
            final CampoEntita field = definition.campi().get(index);
            if (field.chiave()) {
                where.add(field.colonna() + " = ?");
                parameters.add(originalRow.get(index));
            }
        }
        ensureKeys(where);
        final String sql = "UPDATE " + definition.tabella()
            + " SET " + assignments + " WHERE " + where;
        if (dao.execute(sql, parameters) != 1) {
            throw new IllegalArgumentException(
                "Il record da modificare non esiste più"
            );
        }
    }

    public void elimina(
            final DefinizioneEntita definition,
            final List<Object> originalRow) {

        ensureAllowed(definition.eliminabile(), "Eliminazione non consentita");
        final StringJoiner where = new StringJoiner(" AND ");
        final List<Object> parameters = new ArrayList<>();
        for (int index = 0; index < definition.campi().size(); index++) {
            final CampoEntita field = definition.campi().get(index);
            if (field.chiave()) {
                where.add(field.colonna() + " = ?");
                parameters.add(originalRow.get(index));
            }
        }
        ensureKeys(where);
        final String sql = "DELETE FROM " + definition.tabella()
            + " WHERE " + where;
        if (dao.execute(sql, parameters) != 1) {
            throw new IllegalArgumentException(
                "Il record da eliminare non esiste più"
            );
        }
    }

    public List<String> nomiRiepiloghi() {
        return CatalogoRiepiloghi.nomi();
    }

    public DatiTabella eseguiRiepilogo(
            final int indice,
            final LocalDate inizio,
            final LocalDate fine) {

        dao.sincronizzaStati();
        return dao.query(CatalogoRiepiloghi.query(indice, inizio, fine));
    }

    private void ensureAllowed(
            final boolean allowed,
            final String message) {

        if (!allowed) {
            throw new IllegalArgumentException(message);
        }
    }

    private void ensureKeys(final StringJoiner where) {
        if (where.length() == 0) {
            throw new IllegalArgumentException(
                "La definizione non contiene una chiave"
            );
        }
    }
}
