package it.unibo.piscina.controller;

import it.unibo.piscina.data.ConnectionFactory;
import it.unibo.piscina.data.DAOUtils;
import it.unibo.piscina.model.SessioneUtente;
import it.unibo.piscina.view.ApplicationView;
import javax.swing.SwingWorker;
import java.util.concurrent.ExecutionException;

/** Coordina il modello e la vista. */
public final class ApplicationController {

    private final ConnectionFactory connectionFactory;
    private final ApplicationView view;
    private SessioneUtente currentSession;

    public ApplicationController(
            final ConnectionFactory connectionFactory,
            final ApplicationView view) {

        this.connectionFactory = connectionFactory;
        this.view = view;
    }

    public void start() {
        view.showLogin();
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                DAOUtils.checkConnection(connectionFactory);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    view.setDatabaseConnected(true);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    view.setDatabaseConnected(false);
                } catch (ExecutionException exception) {
                    view.setDatabaseConnected(false);
                    view.showMessage("Database non disponibile: "
                        + exception.getCause().getMessage()
                        + "\nEsegui doc/sql/01_schema_completo.sql e controlla la configurazione.");
                }
            }
        }.execute();
    }

    public void userAuthenticated(final SessioneUtente session) {
        currentSession = session;
        view.showDashboard(session);
    }

    public void userSelectedSection(final String section) {
        if (currentSession == null) {
            view.showLogin();
            return;
        }
        if (!currentSession.puoAccedere(section)) {
            view.showAccessDenied(section);
            return;
        }
        if ("Utenti".equals(section)) {
            view.showUsersMenu();
        } else if ("Riepiloghi".equals(section)) {
            view.showReports();
        } else {
            view.showDomainSection(currentSession, section);
        }
    }

    public void userRequestedMainMenu() {
        if (currentSession == null) {
            view.showLogin();
        } else {
            view.showDashboard(currentSession);
        }
    }

    public void userRequestedLogout() {
        currentSession = null;
        view.showLogin();
    }

    public void userRequestedExit() {
        view.closeApplication();
    }
}
