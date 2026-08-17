package it.unibo.piscina;

import it.unibo.piscina.controller.AuthController;
import it.unibo.piscina.controller.ApplicationController;
import it.unibo.piscina.controller.GestioneController;
import it.unibo.piscina.controller.UtentiController;
import it.unibo.piscina.dao.AccountDAO;
import it.unibo.piscina.dao.JdbcAccountDAO;
import it.unibo.piscina.dao.JdbcGestioneDAO;
import it.unibo.piscina.dao.JdbcUtenteDAO;
import it.unibo.piscina.dao.UtenteDAO;
import it.unibo.piscina.data.ConnectionFactory;
import it.unibo.piscina.data.DAOUtils;
import it.unibo.piscina.data.JdbcConnectionFactory;
import it.unibo.piscina.service.AuthService;
import it.unibo.piscina.service.GestioneService;
import it.unibo.piscina.service.UtenteService;
import it.unibo.piscina.service.security.PasswordHasher;
import it.unibo.piscina.view.ApplicationView;
import javax.swing.SwingUtilities;

/** Entry point dell'applicazione. */
public final class App {

    private App() {
    }

    public static void main(final String[] args) {
        SwingUtilities.invokeLater(App::startApplication);
    }

    private static void startApplication() {
        final ConnectionFactory connectionFactory =
            new JdbcConnectionFactory(DAOUtils.loadDatabaseProperties());

        final AccountDAO accountDAO =
            new JdbcAccountDAO(connectionFactory);
        final AuthService authService = new AuthService(
            accountDAO,
            new PasswordHasher()
        );
        final AuthController authController =
            new AuthController(authService);

        final UtenteDAO utenteDAO =
            new JdbcUtenteDAO(connectionFactory);
        final UtenteService utenteService =
            new UtenteService(utenteDAO);
        final UtentiController utentiController =
            new UtentiController(utenteService);

        final GestioneController gestioneController =
            new GestioneController(
                new GestioneService(
                    new JdbcGestioneDAO(connectionFactory)
                )
            );

        final ApplicationView view =
            new ApplicationView(
                authController,
                utentiController,
                gestioneController
            );
        final ApplicationController controller =
            new ApplicationController(connectionFactory, view);
        view.setController(controller);
        controller.start();
    }
}
