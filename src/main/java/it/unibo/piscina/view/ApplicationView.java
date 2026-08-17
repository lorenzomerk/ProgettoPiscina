package it.unibo.piscina.view;

import it.unibo.piscina.controller.ApplicationController;
import it.unibo.piscina.controller.AuthController;
import it.unibo.piscina.controller.GestioneController;
import it.unibo.piscina.controller.UtentiController;
import it.unibo.piscina.model.SessioneUtente;
import it.unibo.piscina.view.auth.AuthPanel;
import it.unibo.piscina.view.dashboard.DashboardPanel;
import it.unibo.piscina.view.gestione.ModuloDominioPanel;
import it.unibo.piscina.view.gestione.RiepiloghiPanel;
import it.unibo.piscina.view.utenti.UtentiPanel;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/** Finestra principale e navigazione tra autenticazione e aree applicative. */
public final class ApplicationView extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final String AUTH = "auth";
    private static final String DASHBOARD = "dashboard";
    private static final String UTENTI = "utenti";
    private static final String DOMAIN_MODULE = "domain-module";
    private static final String REPORTS = "reports";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    private final AuthPanel authPanel;
    private final UtentiPanel utentiPanel;
    private final GestioneController gestioneController;
    private DashboardPanel dashboardPanel;
    private ModuloDominioPanel domainModulePanel;
    private RiepiloghiPanel reportsPanel;
    private ApplicationController controller;
    private boolean databaseConnected;

    public ApplicationView(
            final AuthController authController,
            final UtentiController utentiController,
            final GestioneController gestioneController) {

        super("Gestione Piscina");
        Objects.requireNonNull(authController);
        Objects.requireNonNull(utentiController);
        this.gestioneController =
            Objects.requireNonNull(gestioneController);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(960, 620));
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent event) {
                requestApplicationExit();
            }
        });

        authPanel = new AuthPanel(
            authController,
            this::authenticationSucceeded
        );
        utentiPanel = new UtentiPanel(
            utentiController,
            this::requestMainMenu
        );
        cards.add(authPanel, AUTH);
        cards.add(utentiPanel, UTENTI);
        setContentPane(cards);
    }

    public void setController(final ApplicationController controller) {
        this.controller = Objects.requireNonNull(controller);
    }

    public void showLogin() {
        authPanel.reset();
        cardLayout.show(cards, AUTH);
        showWindow();
    }

    public void showDashboard(final SessioneUtente session) {
        if (dashboardPanel != null) {
            cards.remove(dashboardPanel);
        }
        dashboardPanel = new DashboardPanel(
            session,
            this::selectSection,
            this::requestLogout,
            this::requestApplicationExit
        );
        dashboardPanel.setDatabaseConnected(databaseConnected);
        cards.add(dashboardPanel, DASHBOARD);
        cards.revalidate();
        cardLayout.show(cards, DASHBOARD);
        showWindow();
    }

    public void showUsersMenu() {
        cardLayout.show(cards, UTENTI);
        utentiPanel.reloadData();
        showWindow();
    }

    public void showDomainSection(
            final SessioneUtente session,
            final String section) {

        if (domainModulePanel != null) {
            cards.remove(domainModulePanel);
        }
        domainModulePanel = new ModuloDominioPanel(
            gestioneController,
            session,
            section,
            this::requestMainMenu
        );
        cards.add(domainModulePanel, DOMAIN_MODULE);
        cards.revalidate();
        cardLayout.show(cards, DOMAIN_MODULE);
        domainModulePanel.reloadData();
        showWindow();
    }

    public void showReports() {
        if (reportsPanel != null) {
            cards.remove(reportsPanel);
        }
        reportsPanel = new RiepiloghiPanel(
            gestioneController,
            this::requestMainMenu
        );
        cards.add(reportsPanel, REPORTS);
        cards.revalidate();
        cardLayout.show(cards, REPORTS);
        reportsPanel.reloadData();
        showWindow();
    }

    public void setDatabaseConnected(final boolean connected) {
        databaseConnected = connected;
        authPanel.setDatabaseConnected(connected);
        if (dashboardPanel != null) {
            dashboardPanel.setDatabaseConnected(connected);
        }
    }

    public void showAccessDenied(final String section) {
        JOptionPane.showMessageDialog(
            this,
            "Il ruolo corrente non può accedere alla sezione \""
                + section + "\".",
            "Accesso non autorizzato",
            JOptionPane.WARNING_MESSAGE
        );
    }

    public void showMessage(final String message) {
        JOptionPane.showMessageDialog(
            this,
            message,
            "Gestione Piscina",
            JOptionPane.ERROR_MESSAGE
        );
    }

    public void closeApplication() {
        final int selectedOption = JOptionPane.showConfirmDialog(
            this,
            "Vuoi davvero chiudere Gestione Piscina?",
            "Conferma chiusura",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        if (selectedOption == JOptionPane.YES_OPTION) {
            dispose();
        }
    }

    private void authenticationSucceeded(final SessioneUtente session) {
        if (controller != null) {
            controller.userAuthenticated(session);
        }
    }

    private void selectSection(final String section) {
        if (controller != null) {
            controller.userSelectedSection(section);
        }
    }

    private void requestMainMenu() {
        if (controller != null) {
            controller.userRequestedMainMenu();
        }
    }

    private void requestLogout() {
        if (controller != null) {
            controller.userRequestedLogout();
        }
    }

    private void requestApplicationExit() {
        if (controller != null) {
            controller.userRequestedExit();
        } else {
            closeApplication();
        }
    }

    private void showWindow() {
        if (!isVisible()) {
            pack();
        }
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setVisible(true);
    }
}
