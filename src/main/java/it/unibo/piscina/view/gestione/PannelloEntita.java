package it.unibo.piscina.view.gestione;

import static it.unibo.piscina.view.theme.PoolTheme.BODY_FONT;
import static it.unibo.piscina.view.theme.PoolTheme.DANGER;
import static it.unibo.piscina.view.theme.PoolTheme.MUTED_TEXT;
import static it.unibo.piscina.view.theme.PoolTheme.SURFACE;
import static it.unibo.piscina.view.theme.PoolTheme.TEXT;

import it.unibo.piscina.controller.GestioneController;
import it.unibo.piscina.model.DatiTabella;
import it.unibo.piscina.model.DefinizioneEntita;
import it.unibo.piscina.view.theme.PoolTheme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/** Tabella e comandi CRUD di una singola entità o associazione. */
final class PannelloEntita extends JPanel {

    private static final long serialVersionUID = 1L;

    private final GestioneController controller;
    private final DefinizioneEntita definition;
    private final DefaultTableModel model = new DefaultTableModel() {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(
                final int row,
                final int column) {

            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JLabel status = new JLabel(" ");
    private final JButton addButton = new JButton("Nuovo");
    private final JButton editButton = new JButton("Modifica");
    private final JButton deleteButton = new JButton("Rimuovi");
    private final JButton reloadButton = new JButton("Aggiorna");
    private List<List<Object>> rows = List.of();

    PannelloEntita(
            final GestioneController controller,
            final DefinizioneEntita definition) {

        this.controller = Objects.requireNonNull(controller);
        this.definition = Objects.requireNonNull(definition);
        configureLayout();
        configureActions();
    }

    void reloadData() {
        setBusy(true, "Caricamento...");
        new SwingWorker<DatiTabella, Void>() {
            @Override
            protected DatiTabella doInBackground() {
                return controller.carica(definition);
            }

            @Override
            protected void done() {
                try {
                    showData(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Caricamento interrotto");
                } catch (ExecutionException exception) {
                    showError(message(exception.getCause()));
                } finally {
                    setBusy(false, null);
                }
            }
        }.execute();
    }

    private void showData(final DatiTabella data) {
        rows = data.righe();
        model.setDataVector(
            rows.stream().map(List::toArray).toArray(Object[][]::new),
            data.colonne().toArray()
        );
        status.setText(rows.size() + " record");
    }

    private void configureLayout() {
        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        final JPanel top = new JPanel(new BorderLayout(12, 8));
        top.setOpaque(false);
        final JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        final JLabel title = new JLabel(definition.titolo());
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        title.setForeground(TEXT);
        final JLabel description = new JLabel(definition.descrizione());
        description.setFont(BODY_FONT);
        description.setForeground(MUTED_TEXT);
        heading.add(title, BorderLayout.NORTH);
        heading.add(description, BorderLayout.SOUTH);
        top.add(heading, BorderLayout.NORTH);
        top.add(createToolbar(), BorderLayout.SOUTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(27);
        table.setFont(BODY_FONT);
        table.getTableHeader().setFont(
            new Font(Font.SANS_SERIF, Font.BOLD, 12)
        );
        table.getTableHeader().setBackground(new Color(220, 238, 244));
        table.setGridColor(new Color(224, 234, 238));
        table.setFillsViewportHeight(true);
        final JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(SURFACE);
        scroll.setBorder(
            BorderFactory.createLineBorder(new Color(205, 220, 226))
        );

        status.setFont(BODY_FONT);
        status.setForeground(MUTED_TEXT);
        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
    }

    private JPanel createToolbar() {
        final JPanel toolbar = new JPanel(
            new FlowLayout(FlowLayout.LEFT, 8, 0)
        );
        toolbar.setOpaque(false);
        PoolTheme.stylePrimaryButton(addButton);
        styleSecondary(editButton);
        styleDanger(deleteButton);
        styleSecondary(reloadButton);
        addButton.setVisible(definition.inseribile());
        editButton.setVisible(definition.modificabile());
        deleteButton.setVisible(definition.eliminabile());
        toolbar.add(addButton);
        toolbar.add(editButton);
        toolbar.add(deleteButton);
        toolbar.add(reloadButton);
        return toolbar;
    }

    private void configureActions() {
        addButton.addActionListener(event -> {
            final List<Object> values = EntitaFormDialog.show(
                this, controller, definition, null, false
            );
            if (values != null) {
                runMutation(() -> controller.inserisci(definition, values));
            }
        });
        editButton.addActionListener(event -> {
            final List<Object> selected = selectedRow();
            if (selected == null) {
                return;
            }
            final List<Object> values = EntitaFormDialog.show(
                this, controller, definition, selected, true
            );
            if (values != null) {
                runMutation(() ->
                    controller.modifica(definition, selected, values));
            }
        });
        deleteButton.addActionListener(event -> deleteSelected());
        reloadButton.addActionListener(event -> reloadData());
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(final java.awt.event.MouseEvent event) {
                if (event.getClickCount() == 2
                        && definition.modificabile()) {
                    editButton.doClick();
                }
            }
        });
    }

    private void deleteSelected() {
        final List<Object> selected = selectedRow();
        if (selected == null) {
            return;
        }
        final int answer = JOptionPane.showConfirmDialog(
            this,
            "Rimuovere il collegamento selezionato?\n"
                + "Le entità storiche non vengono eliminate da questa vista.",
            "Conferma rimozione",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        if (answer == JOptionPane.YES_OPTION) {
            runMutation(() -> controller.elimina(definition, selected));
        }
    }

    private List<Object> selectedRow() {
        final int selectedViewRow = table.getSelectedRow();
        if (selectedViewRow < 0) {
            JOptionPane.showMessageDialog(
                this,
                "Seleziona prima una riga.",
                definition.titolo(),
                JOptionPane.INFORMATION_MESSAGE
            );
            return null;
        }
        return rows.get(table.convertRowIndexToModel(selectedViewRow));
    }

    private void runMutation(final Runnable mutation) {
        setBusy(true, "Salvataggio...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                mutation.run();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    reloadData();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Operazione interrotta");
                    setBusy(false, null);
                } catch (ExecutionException exception) {
                    showError(message(exception.getCause()));
                    setBusy(false, null);
                }
            }
        }.execute();
    }

    private void setBusy(final boolean busy, final String text) {
        addButton.setEnabled(!busy);
        editButton.setEnabled(!busy);
        deleteButton.setEnabled(!busy);
        reloadButton.setEnabled(!busy);
        table.setEnabled(!busy);
        if (text != null) {
            status.setText(text);
        }
    }

    private void styleSecondary(final JButton button) {
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        button.setForeground(TEXT);
        button.setBackground(SURFACE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 204, 214)),
            BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
    }

    private void styleDanger(final JButton button) {
        styleSecondary(button);
        button.setForeground(DANGER);
    }

    private void showError(final String text) {
        JOptionPane.showMessageDialog(
            this,
            text,
            definition.titolo(),
            JOptionPane.ERROR_MESSAGE
        );
    }

    private String message(final Throwable throwable) {
        return throwable == null || throwable.getMessage() == null
            ? "Errore inatteso"
            : throwable.getMessage();
    }
}
