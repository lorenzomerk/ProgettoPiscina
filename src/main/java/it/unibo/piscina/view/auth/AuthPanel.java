package it.unibo.piscina.view.auth;

import static it.unibo.piscina.view.theme.PoolTheme.MUTED_TEXT;
import static it.unibo.piscina.view.theme.PoolTheme.NAVY;
import static it.unibo.piscina.view.theme.PoolTheme.PAGE_BACKGROUND;
import static it.unibo.piscina.view.theme.PoolTheme.POOL_BLUE;
import static it.unibo.piscina.view.theme.PoolTheme.TEXT;

import it.unibo.piscina.controller.AuthController;
import it.unibo.piscina.model.SessioneUtente;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/** Schermata primaria con accesso e creazione account. */
public final class AuthPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String LOGIN = "login";
    private static final String REGISTER = "register";

    private final CardLayout formLayout = new CardLayout();
    private final JPanel formCards = new JPanel(formLayout);
    private final LoginFormPanel loginForm;
    private final RegistrationFormPanel registrationForm;
    private final JLabel databaseStatus = new JLabel();
    private JButton loginTab;
    private JButton registerTab;

    public AuthPanel(
            final AuthController controller,
            final Consumer<SessioneUtente> successAction) {

        Objects.requireNonNull(controller);
        Objects.requireNonNull(successAction);
        loginForm = new LoginFormPanel(controller, successAction);
        registrationForm = new RegistrationFormPanel(
            controller,
            successAction
        );
        configureLayout();
    }

    public void reset() {
        loginForm.reset();
        registrationForm.reset();
        formLayout.show(formCards, LOGIN);
        if (loginTab != null && registerTab != null) {
            selectTab(loginTab, registerTab);
        }
    }

    public void setDatabaseConnected(final boolean connected) {
        databaseStatus.setText(
            connected
                ? "Database MySQL connesso"
                : "Database non disponibile"
        );
        databaseStatus.setForeground(
            connected ? new Color(25, 118, 84) : new Color(174, 45, 45)
        );
    }

    private void configureLayout() {
        setLayout(new GridBagLayout());
        setBackground(PAGE_BACKGROUND);

        final JPanel card = new JPanel(new BorderLayout(0, 16));
        card.setPreferredSize(new Dimension(520, 690));
        card.setBackground(Color.WHITE);
        card.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(205, 220, 226)),
                BorderFactory.createEmptyBorder(22, 38, 26, 38)
            )
        );
        card.add(createHeader(), BorderLayout.NORTH);
        card.add(createForms(), BorderLayout.CENTER);
        add(card);
    }

    private JPanel createHeader() {
        final JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        final JLabel badge = new JLabel("GESTIONE PISCINA");
        badge.setOpaque(true);
        badge.setBackground(NAVY);
        badge.setForeground(Color.WHITE);
        badge.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
        badge.setAlignmentX(CENTER_ALIGNMENT);
        badge.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        final JLabel title = new JLabel("Benvenuto");
        title.setAlignmentX(CENTER_ALIGNMENT);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        title.setForeground(TEXT);

        final JLabel subtitle = new JLabel(
            "Accedi ai servizi del centro natatorio"
        );
        subtitle.setAlignmentX(CENTER_ALIGNMENT);
        subtitle.setForeground(MUTED_TEXT);
        databaseStatus.setAlignmentX(CENTER_ALIGNMENT);
        databaseStatus.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        setDatabaseConnected(false);

        header.add(badge);
        header.add(Box.createVerticalStrut(10));
        header.add(title);
        header.add(Box.createVerticalStrut(5));
        header.add(subtitle);
        header.add(Box.createVerticalStrut(7));
        header.add(databaseStatus);
        return header;
    }

    private JPanel createForms() {
        final JPanel container = new JPanel(new BorderLayout(0, 16));
        container.setOpaque(false);

        final JPanel switcher = new JPanel(new GridLayout(1, 2, 8, 0));
        switcher.setBackground(Color.WHITE);
        switcher.setOpaque(true);
        loginTab = tabButton("Accedi", true);
        registerTab = tabButton("Crea account", false);
        loginTab.addActionListener(event -> {
            selectTab(loginTab, registerTab);
            formLayout.show(formCards, LOGIN);
        });
        registerTab.addActionListener(event -> {
            selectTab(registerTab, loginTab);
            formLayout.show(formCards, REGISTER);
        });
        switcher.add(loginTab);
        switcher.add(registerTab);

        formCards.setOpaque(false);
        formCards.add(loginForm, LOGIN);
        formCards.add(registrationForm, REGISTER);
        final JScrollPane formScroll = new JScrollPane(formCards);
        formScroll.setBorder(null);
        formScroll.setOpaque(false);
        formScroll.getViewport().setOpaque(false);
        formScroll.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        formScroll.getVerticalScrollBar().setUnitIncrement(14);
        container.add(switcher, BorderLayout.NORTH);
        container.add(formScroll, BorderLayout.CENTER);
        return container;
    }

    private JButton tabButton(final String text, final boolean selected) {
        final JButton button = new FlatTabButton(text);
        button.setFocusPainted(false);
        button.setFocusable(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        button.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
        button.setBackground(selected ? POOL_BLUE : Color.WHITE);
        button.setForeground(selected ? Color.WHITE : TEXT);
        return button;
    }

    private void selectTab(
            final JButton selected,
            final JButton other) {

        selected.setBackground(POOL_BLUE);
        selected.setForeground(Color.WHITE);
        other.setBackground(Color.WHITE);
        other.setForeground(TEXT);
        selected.repaint();
        other.repaint();
    }

    /** Pulsante piatto che evita i bordi residui del Look & Feel Swing. */
    private static final class FlatTabButton extends JButton {

        private static final long serialVersionUID = 1L;

        FlatTabButton(final String text) {
            super(text);
        }

        @Override
        protected void paintComponent(final Graphics graphics) {
            final Graphics copy = graphics.create();
            try {
                copy.setColor(getBackground());
                copy.fillRect(0, 0, getWidth(), getHeight());
            } finally {
                copy.dispose();
            }
            super.paintComponent(graphics);
        }
    }
}
