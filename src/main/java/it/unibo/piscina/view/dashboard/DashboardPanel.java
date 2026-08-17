package it.unibo.piscina.view.dashboard;

import static it.unibo.piscina.view.theme.PoolTheme.BODY_FONT;
import static it.unibo.piscina.view.theme.PoolTheme.DANGER;
import static it.unibo.piscina.view.theme.PoolTheme.MUTED_TEXT;
import static it.unibo.piscina.view.theme.PoolTheme.NAVY;
import static it.unibo.piscina.view.theme.PoolTheme.PAGE_BACKGROUND;
import static it.unibo.piscina.view.theme.PoolTheme.SECTION_FONT;
import static it.unibo.piscina.view.theme.PoolTheme.SUCCESS;
import static it.unibo.piscina.view.theme.PoolTheme.SUCCESS_BACKGROUND;
import static it.unibo.piscina.view.theme.PoolTheme.SURFACE;
import static it.unibo.piscina.view.theme.PoolTheme.TEXT;
import static it.unibo.piscina.view.theme.PoolTheme.TITLE_FONT;

import it.unibo.piscina.model.SessioneUtente;
import it.unibo.piscina.view.theme.PoolTheme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Dashboard filtrata in base al ruolo dell'account autenticato. */
public final class DashboardPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final List<SectionDefinition> SECTIONS = List.of(
        new SectionDefinition(
            "Utenti", "Utenti e qualifiche",
            "Anagrafiche, contatti, atleti e istruttori"
        ),
        new SectionDefinition(
            "Accessi", "Accessi al nuoto libero",
            "Ingressi effettivi e consumi"
        ),
        new SectionDefinition(
            "Abbonamenti", "Tipi e abbonamenti",
            "Validità, compatibilità e acquisti"
        ),
        new SectionDefinition(
            "Attività", "Corsi e attività",
            "Programmazione, turni e iscrizioni"
        ),
        new SectionDefinition(
            "Struttura", "Vasche e corsie",
            "Spazi e utilizzo nel calendario"
        ),
        new SectionDefinition(
            "Club e squadre", "Società e squadre",
            "Allenamenti e gruppi sportivi"
        ),
        new SectionDefinition(
            "Riepiloghi", "Riepiloghi e statistiche",
            "Capienze, frequenze e indicatori operativi"
        ),
        new SectionDefinition(
            "La mia squadra", "La mia squadra",
            "Appartenenza attiva e attività sportive"
        ),
        new SectionDefinition(
            "Attività assegnate", "Le mie attività",
            "Turni, partecipanti e incarichi"
        ),
        new SectionDefinition(
            "Squadre seguite", "Squadre seguite",
            "Atleti e incarichi attivi"
        )
    );

    private final SessioneUtente session;
    private final Consumer<String> sectionSelection;
    private final Runnable logoutAction;
    private final Runnable exitAction;
    private final JLabel connectionStatus = new JLabel();
    private final JLabel databaseDetail = new JLabel();

    public DashboardPanel(
            final SessioneUtente session,
            final Consumer<String> sectionSelection,
            final Runnable logoutAction,
            final Runnable exitAction) {

        this.session = Objects.requireNonNull(session);
        this.sectionSelection = Objects.requireNonNull(sectionSelection);
        this.logoutAction = Objects.requireNonNull(logoutAction);
        this.exitAction = Objects.requireNonNull(exitAction);
        setLayout(new BorderLayout());
        setBackground(PAGE_BACKGROUND);
        add(createMainHeader(), BorderLayout.NORTH);
        add(createDashboardContent(), BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);
    }

    public void setDatabaseConnected(final boolean connected) {
        connectionStatus.setText(
            connected
                ? "  DATABASE MYSQL CONNESSO  "
                : "  DATABASE NON DISPONIBILE  "
        );
        connectionStatus.setForeground(connected ? SUCCESS : DANGER);
        connectionStatus.setBackground(
            connected ? SUCCESS_BACKGROUND : new Color(252, 231, 231)
        );
        connectionStatus.setBorder(
            BorderFactory.createLineBorder(
                connected
                    ? new Color(145, 211, 181)
                    : new Color(224, 178, 178)
            )
        );
        databaseDetail.setText(
            connected ? "MySQL: operativo" : "MySQL: non disponibile"
        );
        databaseDetail.setForeground(connected ? SUCCESS : DANGER);
    }

    private JPanel createDashboardContent() {
        final JPanel dashboard = new JPanel(new BorderLayout(24, 0));
        dashboard.setOpaque(false);
        dashboard.setBorder(
            BorderFactory.createEmptyBorder(26, 30, 24, 30)
        );
        dashboard.add(createSectionsPanel(), BorderLayout.CENTER);
        dashboard.add(createStatusPanel(), BorderLayout.EAST);
        return dashboard;
    }

    private JPanel createMainHeader() {
        final JPanel header = new JPanel(new BorderLayout(20, 0));
        header.setBackground(NAVY);
        header.setBorder(
            BorderFactory.createEmptyBorder(20, 30, 20, 30)
        );

        final JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));

        final JLabel eyebrow = new JLabel("GESTIONALE CENTRO NATATORIO");
        eyebrow.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        eyebrow.setForeground(new Color(132, 226, 235));
        final JLabel title = new JLabel("Gestione Piscina");
        title.setFont(TITLE_FONT);
        title.setForeground(Color.WHITE);
        final JLabel subtitle = new JLabel(
            session.descrizioneProfilo() + " — " + session.nomeCompleto()
        );
        subtitle.setFont(BODY_FONT);
        subtitle.setForeground(new Color(207, 226, 234));

        brand.add(eyebrow);
        brand.add(Box.createVerticalStrut(4));
        brand.add(title);
        brand.add(Box.createVerticalStrut(3));
        brand.add(subtitle);

        connectionStatus.setFont(
            new Font(Font.SANS_SERIF, Font.BOLD, 11)
        );
        connectionStatus.setOpaque(true);
        setDatabaseConnected(false);
        header.add(brand, BorderLayout.CENTER);
        header.add(connectionStatus, BorderLayout.EAST);
        return header;
    }

    private JPanel createSectionsPanel() {
        final JPanel section = new JPanel(new BorderLayout(0, 16));
        section.setOpaque(false);

        final JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        final JLabel title = new JLabel("Aree disponibili");
        title.setFont(SECTION_FONT);
        title.setForeground(TEXT);
        final JLabel subtitle = new JLabel(
            "Le funzioni dipendono dal profilo applicativo e dalle qualifiche personali."
        );
        subtitle.setFont(BODY_FONT);
        subtitle.setForeground(MUTED_TEXT);
        heading.add(title);
        heading.add(Box.createVerticalStrut(4));
        heading.add(subtitle);

        final List<SectionDefinition> allowed = SECTIONS.stream()
            .filter(item -> session.puoAccedere(item.key()))
            .toList();
        final int rows = Math.max(1, (allowed.size() + 1) / 2);
        final JPanel buttons = new JPanel(new GridLayout(rows, 2, 14, 14));
        buttons.setOpaque(false);
        allowed.forEach(item -> buttons.add(createSectionButton(item)));

        section.add(heading, BorderLayout.NORTH);
        section.add(buttons, BorderLayout.CENTER);
        return section;
    }

    private JButton createSectionButton(final SectionDefinition section) {
        final String label = "<html><div style='padding:4px 2px'>"
            + "<b style='font-size:14px'>" + section.title() + "</b><br>"
            + "<span style='font-size:10px;color:#5b707a'>"
            + section.description() + "</span></div></html>";
        final JButton button = new JButton(label);
        PoolTheme.styleCardButton(button);
        button.setToolTipText("Apri " + section.title().toLowerCase());
        button.addActionListener(
            event -> sectionSelection.accept(section.key())
        );
        return button;
    }

    private JPanel createStatusPanel() {
        final JPanel status = new JPanel();
        status.setBackground(SURFACE);
        status.setPreferredSize(new Dimension(270, 0));
        status.setLayout(new BoxLayout(status, BoxLayout.Y_AXIS));
        status.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(214, 228, 233)),
                BorderFactory.createEmptyBorder(22, 20, 22, 20)
            )
        );

        final JLabel name = new JLabel(session.nomeCompleto());
        name.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        name.setForeground(TEXT);
        final JLabel role = new JLabel(session.descrizioneProfilo());
        role.setFont(BODY_FONT);
        role.setForeground(MUTED_TEXT);
        final JLabel email = new JLabel(session.email());
        email.setFont(BODY_FONT);
        email.setForeground(MUTED_TEXT);
        final JLabel separator = new JLabel("________________________");
        separator.setForeground(new Color(215, 228, 233));
        final JLabel statusTitle = new JLabel("Stato del sistema");
        statusTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        statusTitle.setForeground(TEXT);
        databaseDetail.setFont(BODY_FONT);
        final JLabel areas = new JLabel(
            session.numeroSezioni() + " aree accessibili"
        );
        areas.setFont(BODY_FONT);
        areas.setForeground(MUTED_TEXT);
        final JLabel hint = new JLabel(
            "<html><div style='width:205px'>Le autorizzazioni vengono "
                + "controllate anche dal controller prima di aprire "
                + "una sezione.</div></html>"
        );
        hint.setFont(BODY_FONT);
        hint.setForeground(MUTED_TEXT);

        status.add(name);
        status.add(Box.createVerticalStrut(4));
        status.add(role);
        status.add(Box.createVerticalStrut(4));
        status.add(email);
        status.add(Box.createVerticalStrut(12));
        status.add(separator);
        status.add(Box.createVerticalStrut(16));
        status.add(statusTitle);
        status.add(Box.createVerticalStrut(9));
        status.add(databaseDetail);
        status.add(Box.createVerticalStrut(7));
        status.add(areas);
        status.add(Box.createVerticalGlue());
        status.add(hint);
        return status;
    }

    private JPanel createFooter() {
        final JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(SURFACE);
        footer.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(
                    1, 0, 0, 0, new Color(214, 228, 233)
                ),
                BorderFactory.createEmptyBorder(12, 30, 12, 30)
            )
        );

        final JLabel context = new JLabel(
            "Sessione: " + session.descrizioneProfilo()
        );
        context.setFont(BODY_FONT);
        context.setForeground(MUTED_TEXT);

        final JPanel actions = new JPanel(new FlowLayout(
            FlowLayout.RIGHT,
            10,
            0
        ));
        actions.setOpaque(false);
        final JButton logoutButton = new JButton("Disconnetti");
        PoolTheme.stylePrimaryButton(logoutButton);
        logoutButton.addActionListener(event -> logoutAction.run());

        final JButton exitButton = new JButton("Esci dall'applicazione");
        exitButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        exitButton.setForeground(DANGER);
        exitButton.setBackground(SURFACE);
        exitButton.setFocusPainted(false);
        exitButton.setCursor(
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );
        exitButton.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(224, 178, 178)),
                BorderFactory.createEmptyBorder(7, 14, 7, 14)
            )
        );
        exitButton.addActionListener(event -> exitAction.run());
        actions.add(logoutButton);
        actions.add(exitButton);

        footer.add(context, BorderLayout.WEST);
        footer.add(actions, BorderLayout.EAST);
        return footer;
    }

    private record SectionDefinition(
        String key,
        String title,
        String description
    ) {
    }
}
