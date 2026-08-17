package it.unibo.piscina.view.auth;

import it.unibo.piscina.controller.AuthController;
import it.unibo.piscina.model.SessioneUtente;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

/** Form di accesso con email e password. */
final class LoginFormPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final AuthController controller;
    private final Consumer<SessioneUtente> successAction;
    private final JTextField emailField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JButton loginButton = new JButton("Accedi");
    private final JLabel statusLabel = new JLabel(" ");

    LoginFormPanel(
            final AuthController controller,
            final Consumer<SessioneUtente> successAction) {

        this.controller = Objects.requireNonNull(controller);
        this.successAction = Objects.requireNonNull(successAction);
        configureLayout();
    }

    void reset() {
        emailField.setText("");
        passwordField.setText("");
        statusLabel.setText(" ");
        loginButton.setEnabled(true);
        emailField.setEnabled(true);
        passwordField.setEnabled(true);
    }

    private void configureLayout() {
        setLayout(new GridBagLayout());
        setOpaque(false);
        AuthUi.styleTextField(emailField);
        AuthUi.styleTextField(passwordField);
        AuthUi.styleActionButton(loginButton);

        final JCheckBox showPassword = new JCheckBox("Mostra password");
        showPassword.setOpaque(false);
        final char echo = passwordField.getEchoChar();
        showPassword.addActionListener(event -> passwordField.setEchoChar(
            showPassword.isSelected() ? (char) 0 : echo
        ));

        loginButton.addActionListener(event -> authenticate());
        passwordField.addActionListener(event -> authenticate());

        final GridBagConstraints constraints = constraints();
        constraints.gridy = 0;
        add(AuthUi.fieldGroup("Email", emailField), constraints);
        constraints.gridy = 1;
        constraints.insets = new Insets(14, 0, 0, 0);
        add(AuthUi.fieldGroup("Password", passwordField), constraints);
        constraints.gridy = 2;
        constraints.insets = new Insets(4, 0, 0, 0);
        add(showPassword, constraints);
        constraints.gridy = 3;
        constraints.insets = new Insets(22, 0, 0, 0);
        add(loginButton, constraints);
        constraints.gridy = 4;
        constraints.insets = new Insets(10, 0, 0, 0);
        add(statusLabel, constraints);
    }

    private void authenticate() {
        final char[] password = passwordField.getPassword();
        setBusy(true, "Accesso in corso...");
        new SwingWorker<SessioneUtente, Void>() {
            @Override
            protected SessioneUtente doInBackground() {
                try {
                    return controller.accedi(emailField.getText(), password);
                } finally {
                    Arrays.fill(password, '\0');
                }
            }

            @Override
            protected void done() {
                try {
                    final SessioneUtente session = get();
                    passwordField.setText("");
                    successAction.accept(session);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    setBusy(false, "Accesso interrotto");
                } catch (ExecutionException exception) {
                    setBusy(
                        false,
                        AuthUi.errorMessage(exception.getCause())
                    );
                }
            }
        }.execute();
    }

    private void setBusy(final boolean busy, final String message) {
        loginButton.setEnabled(!busy);
        emailField.setEnabled(!busy);
        passwordField.setEnabled(!busy);
        AuthUi.setStatus(statusLabel, message, !busy);
    }

    private GridBagConstraints constraints() {
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        return constraints;
    }
}
