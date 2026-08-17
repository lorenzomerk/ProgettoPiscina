package it.unibo.piscina.view.utenti;

import it.unibo.piscina.model.Utente;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/** Adatta le anagrafiche alla tabella Swing. */
final class UtentiTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {
        "ID", "Codice fiscale", "Cognome", "Nome", "Data nascita",
        "Registrazione", "Email", "Telefono", "Certificato medico",
        "Ruoli", "Stato"
    };

    private List<Utente> utenti = new ArrayList<>();

    void setUtenti(final List<Utente> updatedUsers) {
        utenti = new ArrayList<>(updatedUsers);
        fireTableDataChanged();
    }

    Utente getUtente(final int row) {
        return utenti.get(row);
    }

    @Override
    public int getRowCount() {
        return utenti.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(final int column) {
        return COLUMNS[column];
    }

    @Override
    public Object getValueAt(final int row, final int column) {
        final Utente utente = utenti.get(row);
        return switch (column) {
            case 0 -> utente.id();
            case 1 -> utente.codiceFiscale();
            case 2 -> utente.cognome();
            case 3 -> utente.nome();
            case 4 -> utente.dataNascita();
            case 5 -> utente.dataRegistrazione();
            case 6 -> utente.email();
            case 7 -> utente.telefono();
            case 8 -> certificateStatus(utente);
            case 9 -> roles(utente);
            case 10 -> utente.attivo() ? "Attivo" : "Disattivato";
            default -> "";
        };
    }

    private String roles(final Utente utente) {
        if (utente.atleta() && utente.istruttore()) {
            return "Atleta • Istruttore";
        }
        if (utente.atleta()) {
            return "Atleta";
        }
        if (utente.istruttore()) {
            return "Istruttore";
        }
        return "Nessuno";
    }

    private String certificateStatus(final Utente utente) {
        final LocalDate expiry = utente.scadenzaCertificatoMedico();
        if (expiry == null) {
            return "Non registrato";
        }
        return expiry.isBefore(LocalDate.now())
            ? "Scaduto: " + expiry
            : expiry.toString();
    }
}
