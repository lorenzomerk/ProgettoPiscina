package it.unibo.piscina.view.gestione;

import static it.unibo.piscina.view.theme.PoolTheme.PAGE_BACKGROUND;

import it.unibo.piscina.controller.GestioneController;
import it.unibo.piscina.model.DefinizioneEntita;
import it.unibo.piscina.model.SessioneUtente;
import it.unibo.piscina.view.theme.PoolTheme;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

/** Contenitore delle entità che compongono una sezione del gestionale. */
public final class ModuloDominioPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final List<PannelloEntita> panels = new ArrayList<>();

    public ModuloDominioPanel(
            final GestioneController controller,
            final SessioneUtente session,
            final String section,
            final Runnable backAction) {

        Objects.requireNonNull(controller);
        Objects.requireNonNull(session);
        Objects.requireNonNull(backAction);
        setLayout(new BorderLayout());
        setBackground(PAGE_BACKGROUND);
        add(
            PoolTheme.createPageHeader(
                section,
                subtitle(section)
            ),
            BorderLayout.NORTH
        );

        final List<DefinizioneEntita> definitions =
            controller.definizioniPerSezione(section, session);
        final JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);
        content.setBorder(
            BorderFactory.createEmptyBorder(12, 24, 16, 24)
        );
        if (definitions.isEmpty()) {
            content.add(
                new JLabel(
                    "Nessun dato associato al profilo corrente.",
                    SwingConstants.CENTER
                ),
                BorderLayout.CENTER
            );
        } else {
            final JTabbedPane tabs = new JTabbedPane();
            for (DefinizioneEntita definition : definitions) {
                final PannelloEntita panel =
                    new PannelloEntita(controller, definition);
                panels.add(panel);
                tabs.addTab(definition.titolo(), panel);
            }
            tabs.addChangeListener(event -> {
                final int index = tabs.getSelectedIndex();
                if (index >= 0) {
                    panels.get(index).reloadData();
                }
            });
            content.add(tabs, BorderLayout.CENTER);
        }

        final JButton back = new JButton("Torna alla dashboard");
        PoolTheme.stylePrimaryButton(back);
        back.addActionListener(event -> backAction.run());
        final JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(back, BorderLayout.EAST);
        content.add(footer, BorderLayout.SOUTH);
        add(content, BorderLayout.CENTER);
    }

    public void reloadData() {
        if (!panels.isEmpty()) {
            panels.getFirst().reloadData();
        }
    }

    private static String subtitle(final String section) {
        return switch (section) {
            case "Abbonamenti" ->
                "Tipi, compatibilità e abbonamenti acquistati";
            case "Attività" ->
                "Programmazione, istruttori e iscrizioni";
            case "Accessi" ->
                "Registrazione degli ingressi al nuoto libero";
            case "Struttura" ->
                "Vasche, corsie e occupazione degli spazi";
            case "Club e squadre" ->
                "Club, squadre e storico dei rapporti sportivi";
            default -> "Consultazione dei dati di competenza";
        };
    }
}
