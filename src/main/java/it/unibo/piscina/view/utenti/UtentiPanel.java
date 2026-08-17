package it.unibo.piscina.view.utenti;

import static it.unibo.piscina.view.theme.PoolTheme.BODY_FONT;
import static it.unibo.piscina.view.theme.PoolTheme.DANGER;
import static it.unibo.piscina.view.theme.PoolTheme.MUTED_TEXT;
import static it.unibo.piscina.view.theme.PoolTheme.PAGE_BACKGROUND;
import static it.unibo.piscina.view.theme.PoolTheme.SURFACE;
import static it.unibo.piscina.view.theme.PoolTheme.TEXT;

import it.unibo.piscina.controller.UtentiController;
import it.unibo.piscina.model.Utente;
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

/** Elenco e operazioni CRUD delle anagrafiche degli utenti. */
public final class UtentiPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final UtentiController controller;
    private final Runnable backAction;
    private final UtentiTableModel tableModel = new UtentiTableModel();
    private final JTable table = new JTable(tableModel);
    private final JLabel statusLabel = new JLabel(" ");
    private final JButton newButton = new JButton("Nuovo utente");
    private final JButton editButton = new JButton("Modifica");
    private final JButton deactivateButton = new JButton("Disattiva");
    private final JButton refreshButton = new JButton("Aggiorna");

    public UtentiPanel(
            final UtentiController controller,
            final Runnable backAction) {

        this.controller = Objects.requireNonNull(controller);
        this.backAction = Objects.requireNonNull(backAction);
        configureLayout();
        configureActions();
    }

    public void reloadData() {
        setBusy(true, "Caricamento utenti...");
        new SwingWorker<List<Utente>, Void>() {
            @Override
            protected List<Utente> doInBackground() {
                return controller.caricaUtenti();
            }

            @Override
            protected void done() {
                try {
                    final List<Utente> utenti = get();
                    tableModel.setUtenti(utenti);
                    statusLabel.setText(
                        utenti.size() + " utenti registrati"
                    );
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Caricamento interrotto");
                } catch (ExecutionException exception) {
                    showError(errorMessage(exception.getCause()));
                } finally {
                    setBusy(false, null);
                }
            }
        }.execute();
    }

    private void configureLayout() {
        setLayout(new BorderLayout());
        setBackground(PAGE_BACKGROUND);
        add(
            PoolTheme.createPageHeader(
                "Utenti e qualifiche",
                "Anagrafiche, certificati, atleti e istruttori"
            ),
            BorderLayout.NORTH
        );

        final JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setOpaque(false);
        content.setBorder(
            BorderFactory.createEmptyBorder(22, 30, 22, 30)
        );
        content.add(createToolbar(), BorderLayout.NORTH);
        content.add(createTable(), BorderLayout.CENTER);
        content.add(createFooter(), BorderLayout.SOUTH);
        add(content, BorderLayout.CENTER);
    }

    private JPanel createToolbar() {
        final JPanel toolbar = new JPanel(new FlowLayout(
            FlowLayout.LEFT,
            10,
            0
        ));
        toolbar.setOpaque(false);

        PoolTheme.stylePrimaryButton(newButton);
        styleSecondaryButton(editButton);
        styleDangerButton(deactivateButton);
        styleSecondaryButton(refreshButton);

        toolbar.add(newButton);
        toolbar.add(editButton);
        toolbar.add(deactivateButton);
        toolbar.add(refreshButton);
        return toolbar;
    }

    private JScrollPane createTable() {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(28);
        table.setFont(BODY_FONT);
        table.getTableHeader().setFont(
            new Font(Font.SANS_SERIF, Font.BOLD, 12)
        );
        table.getTableHeader().setBackground(new Color(220, 238, 244));
        table.setGridColor(new Color(224, 234, 238));
        table.setFillsViewportHeight(true);

        final JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(
            BorderFactory.createLineBorder(new Color(205, 220, 226))
        );
        scrollPane.getViewport().setBackground(SURFACE);
        return scrollPane;
    }

    private JPanel createFooter() {
        final JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);

        statusLabel.setFont(BODY_FONT);
        statusLabel.setForeground(MUTED_TEXT);

        final JButton backButton = new JButton("Torna alla dashboard");
        PoolTheme.stylePrimaryButton(backButton);
        backButton.addActionListener(event -> backAction.run());

        footer.add(statusLabel, BorderLayout.WEST);
        footer.add(backButton, BorderLayout.EAST);
        return footer;
    }

    private void configureActions() {
        newButton.addActionListener(event -> {
            final Utente input = UtenteFormDialog.show(this, null);
            if (input != null) {
                runMutation(() -> controller.creaUtente(input));
            }
        });
        editButton.addActionListener(event -> {
            final Utente selected = selectedUser();
            if (selected == null) {
                return;
            }
            final Utente input = UtenteFormDialog.show(this, selected);
            if (input != null) {
                runMutation(() -> controller.modificaUtente(input));
            }
        });
        deactivateButton.addActionListener(event -> deactivateSelected());
        refreshButton.addActionListener(event -> reloadData());
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(final java.awt.event.MouseEvent event) {
                if (event.getClickCount() == 2) {
                    editButton.doClick();
                }
            }
        });
    }

    private void deactivateSelected() {
        final Utente selected = selectedUser();
        if (selected == null) {
            return;
        }
        if (!selected.attivo()) {
            JOptionPane.showMessageDialog(
                this,
                "L'utente è già disattivato.",
                "Gestione utenti",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        final int answer = JOptionPane.showConfirmDialog(
            this,
            "Disattivare " + selected.nome() + " "
                + selected.cognome() + "?\n"
                + "Lo storico resterà disponibile.",
            "Conferma disattivazione",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        if (answer == JOptionPane.YES_OPTION) {
            runMutation(() -> controller.disattivaUtente(selected.id()));
        }
    }

    private Utente selectedUser() {
        final int selectedViewRow = table.getSelectedRow();
        if (selectedViewRow < 0) {
            JOptionPane.showMessageDialog(
                this,
                "Seleziona prima un utente dalla tabella.",
                "Nessun utente selezionato",
                JOptionPane.INFORMATION_MESSAGE
            );
            return null;
        }
        final int modelRow = table.convertRowIndexToModel(selectedViewRow);
        return tableModel.getUtente(modelRow);
    }

    private void runMutation(final Runnable mutation) {
        setBusy(true, "Salvataggio in corso...");
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
                    showError(errorMessage(exception.getCause()));
                    setBusy(false, null);
                }
            }
        }.execute();
    }

    private void setBusy(final boolean busy, final String message) {
        newButton.setEnabled(!busy);
        editButton.setEnabled(!busy);
        deactivateButton.setEnabled(!busy);
        refreshButton.setEnabled(!busy);
        table.setEnabled(!busy);
        if (message != null) {
            statusLabel.setText(message);
        }
    }

    private void styleSecondaryButton(final JButton button) {
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        button.setForeground(TEXT);
        button.setBackground(SURFACE);
        button.setFocusPainted(false);
        button.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 204, 214)),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
            )
        );
    }

    private void styleDangerButton(final JButton button) {
        styleSecondaryButton(button);
        button.setForeground(DANGER);
    }

    private void showError(final String message) {
        JOptionPane.showMessageDialog(
            this,
            message,
            "Gestione utenti",
            JOptionPane.ERROR_MESSAGE
        );
    }

    private String errorMessage(final Throwable throwable) {
        return throwable == null || throwable.getMessage() == null
            ? "Errore inatteso"
            : throwable.getMessage();
    }
}
