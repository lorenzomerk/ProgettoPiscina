package it.unibo.piscina.view.gestione;

import static it.unibo.piscina.view.theme.PoolTheme.BODY_FONT;
import static it.unibo.piscina.view.theme.PoolTheme.MUTED_TEXT;
import static it.unibo.piscina.view.theme.PoolTheme.PAGE_BACKGROUND;
import static it.unibo.piscina.view.theme.PoolTheme.SURFACE;

import it.unibo.piscina.controller.GestioneController;
import it.unibo.piscina.model.DatiTabella;
import it.unibo.piscina.view.theme.PoolTheme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/** Consultazioni operative e aggregate richieste da OP7-OP11. */
public final class RiepiloghiPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final GestioneController controller;
    private final List<ReportTab> reports = new ArrayList<>();
    private final JTabbedPane tabs = new JTabbedPane();
    private final JTextField from = new JTextField(
        LocalDate.now().withDayOfYear(1).toString(),
        10
    );
    private final JTextField to = new JTextField(
        LocalDate.now().toString(),
        10
    );
    private final JLabel status = new JLabel(" ");

    public RiepiloghiPanel(
            final GestioneController controller,
            final Runnable backAction) {

        this.controller = controller;
        setLayout(new BorderLayout());
        setBackground(PAGE_BACKGROUND);
        add(
            PoolTheme.createPageHeader(
                "Riepiloghi e statistiche",
                "Indicatori operativi previsti dall'analisi dei requisiti"
            ),
            BorderLayout.NORTH
        );

        final JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        content.setBorder(
            BorderFactory.createEmptyBorder(16, 28, 18, 28)
        );
        content.add(createFilters(), BorderLayout.NORTH);
        createReports();
        content.add(tabs, BorderLayout.CENTER);

        final JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        status.setFont(BODY_FONT);
        status.setForeground(MUTED_TEXT);
        final JButton back = new JButton("Torna alla dashboard");
        PoolTheme.stylePrimaryButton(back);
        back.addActionListener(event -> backAction.run());
        footer.add(status, BorderLayout.WEST);
        footer.add(back, BorderLayout.EAST);
        content.add(footer, BorderLayout.SOUTH);
        add(content, BorderLayout.CENTER);

        tabs.addChangeListener(event -> reloadSelected());
    }

    public void reloadData() {
        reloadSelected();
    }

    private JPanel createFilters() {
        final JPanel filters = new JPanel(
            new FlowLayout(FlowLayout.LEFT, 9, 0)
        );
        filters.setOpaque(false);
        filters.add(new JLabel("Periodo dal"));
        filters.add(from);
        filters.add(new JLabel("al"));
        filters.add(to);
        final JButton reload = new JButton("Calcola");
        PoolTheme.stylePrimaryButton(reload);
        reload.addActionListener(event -> reloadSelected());
        filters.add(reload);
        final JLabel hint = new JLabel(
            "Il periodo si applica ai riepiloghi temporali."
        );
        hint.setFont(BODY_FONT);
        hint.setForeground(MUTED_TEXT);
        filters.add(hint);
        return filters;
    }

    private void createReports() {
        addReport(
            "Attività complete",
            (start, end) -> """
                SELECT ID_Attivita_Programmata AS ID, Titolo,
                       Numero_Iscrizioni AS Iscritti,
                       Capienza_Massima AS Capienza
                FROM VW_ATTIVITA_COMPLETE
                ORDER BY Titolo
                """
        );
        addReport(
            "Club per atleti",
            (start, end) -> """
                SELECT ID_Club AS ID, Nome,
                       Numero_Atleti AS Atleti_Attivi
                FROM VW_CLUB_ATLETI_ATTIVI
                ORDER BY Numero_Atleti DESC, Nome
                """
        );
        addReport(
            "Squadre per atleti",
            (start, end) -> """
                SELECT ID_Squadra AS ID, Nome, Club,
                       Numero_Atleti AS Atleti_Attivi
                FROM VW_SQUADRA_ATLETI_ATTIVI
                ORDER BY Numero_Atleti DESC, Nome
                """
        );
        addReport(
            "Istruttori",
            (start, end) -> """
                SELECT i.ID_Utente AS ID,
                       CONCAT(u.Cognome, ' ', u.Nome) AS Istruttore,
                       COUNT(ap.ID_Attivita_Programmata) AS Attivita
                FROM ISTRUTTORE i
                JOIN UTENTE u ON u.ID_Utente = i.ID_Utente
                LEFT JOIN ASSEGNATO_A aa
                  ON aa.ID_Utente_Istruttore = i.ID_Utente
                LEFT JOIN ATTIVITA_PROGRAMMATA ap
                  ON ap.ID_Attivita_Programmata =
                     aa.ID_Attivita_Programmata
                 AND ap.Periodo_Inizio <= DATE '%s'
                 AND ap.Periodo_Fine >= DATE '%s'
                GROUP BY i.ID_Utente, u.Cognome, u.Nome
                ORDER BY Attivita DESC, u.Cognome, u.Nome
                """.formatted(end, start)
        );
        addReport(
            "Tipi più frequenti",
            (start, end) -> """
                SELECT 'ATTIVITA' AS Ambito, ta.Nome,
                       COUNT(i.ID_Iscrizione_Attivita) AS Frequenza
                FROM TIPO_ATTIVITA ta
                LEFT JOIN ATTIVITA_PROGRAMMATA ap
                  ON ap.ID_Tipo_Attivita = ta.ID_Tipo_Attivita
                LEFT JOIN ISCRIZIONE_ATTIVITA i
                  ON i.ID_Attivita_Programmata =
                     ap.ID_Attivita_Programmata
                 AND i.Data_Iscrizione BETWEEN DATE '%s' AND DATE '%s'
                WHERE ta.Modalita_Partecipazione = 'ISCRIZIONE'
                GROUP BY ta.ID_Tipo_Attivita, ta.Nome
                UNION ALL
                SELECT 'ABBONAMENTO' AS Ambito, t.Nome,
                       COUNT(a.ID_Abbonamento) AS Frequenza
                FROM TIPO_ABBONAMENTO t
                LEFT JOIN ABBONAMENTO a
                  ON a.ID_Tipo_Abbonamento = t.ID_Tipo_Abbonamento
                 AND a.Data_Acquisto BETWEEN DATE '%s' AND DATE '%s'
                GROUP BY t.ID_Tipo_Abbonamento, t.Nome
                ORDER BY Ambito, Frequenza DESC, Nome
                """.formatted(start, end, start, end)
        );
        addReport(
            "Abbonamenti utilizzabili",
            (start, end) -> """
                SELECT COUNT(*) AS Abbonamenti_Attualmente_Utilizzabili
                FROM VW_ABBONAMENTI_UTILIZZABILI
                """
        );
    }

    private void addReport(
            final String name,
            final QueryFactory queryFactory) {

        final DefaultTableModel model = new DefaultTableModel() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(
                    final int row,
                    final int column) {

                return false;
            }
        };
        final JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(27);
        table.setFont(BODY_FONT);
        table.getTableHeader().setFont(
            new Font(Font.SANS_SERIF, Font.BOLD, 12)
        );
        table.getTableHeader().setBackground(new Color(220, 238, 244));
        table.setFillsViewportHeight(true);
        final JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(SURFACE);
        scroll.setBorder(
            BorderFactory.createLineBorder(new Color(205, 220, 226))
        );
        reports.add(new ReportTab(queryFactory, model));
        tabs.addTab(name, scroll);
    }

    private void reloadSelected() {
        final int index = tabs.getSelectedIndex();
        if (index < 0) {
            return;
        }
        final LocalDate start;
        final LocalDate end;
        try {
            start = LocalDate.parse(from.getText().trim());
            end = LocalDate.parse(to.getText().trim());
            if (start.isAfter(end)) {
                throw new IllegalArgumentException(
                    "La data iniziale non può superare quella finale"
                );
            }
        } catch (DateTimeParseException exception) {
            showError("Le date devono usare il formato AAAA-MM-GG");
            return;
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
            return;
        }

        final ReportTab report = reports.get(index);
        status.setText("Calcolo in corso...");
        new SwingWorker<DatiTabella, Void>() {
            @Override
            protected DatiTabella doInBackground() {
                return controller.eseguiConsultazione(
                    report.queryFactory().create(start, end)
                );
            }

            @Override
            protected void done() {
                try {
                    final DatiTabella data = get();
                    report.model().setDataVector(
                        data.righe().stream()
                            .map(List::toArray)
                            .toArray(Object[][]::new),
                        data.colonne().toArray()
                    );
                    status.setText(data.righe().size() + " risultati");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Calcolo interrotto");
                } catch (ExecutionException exception) {
                    final Throwable cause = exception.getCause();
                    showError(
                        cause == null || cause.getMessage() == null
                            ? "Errore inatteso"
                            : cause.getMessage()
                    );
                }
            }
        }.execute();
    }

    private void showError(final String message) {
        status.setText("Operazione non completata");
        JOptionPane.showMessageDialog(
            this,
            message,
            "Riepiloghi",
            JOptionPane.ERROR_MESSAGE
        );
    }

    @FunctionalInterface
    private interface QueryFactory {
        String create(LocalDate start, LocalDate end);
    }

    private record ReportTab(
        QueryFactory queryFactory,
        DefaultTableModel model
    ) {
    }
}
