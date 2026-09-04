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

/** Form di registrazione pubblica degli account cliente. */
final class RegistrationFormPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final AuthController controller;
    private final Consumer<SessioneUtente> successAction;
    private final JTextField nameField = new JTextField();
    private final JTextField surnameField = new JTextField();
    private final JTextField fiscalCodeField = new JTextField();
    private final JTextField birthDateField = new JTextField();
    private final JTextField emailField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JPasswordField confirmationField = new JPasswordField();
    private final JButton registerButton = new JButton("Crea account");
    private final JLabel statusLabel = new JLabel(" ");

    RegistrationFormPanel(
            final AuthController controller,
            final Consumer<SessioneUtente> successAction) {

        this.controller = Objects.requireNonNull(controller);
        this.successAction = Objects.requireNonNull(successAction);
        configureLayout();
    }

    void reset() {
        nameField.setText("");
        surnameField.setText("");
        fiscalCodeField.setText("");
        birthDateField.setText("");
        emailField.setText("");
        passwordField.setText("");
        confirmationField.setText("");
        statusLabel.setText(" ");
        registerButton.setEnabled(true);
        nameField.setEnabled(true);
        surnameField.setEnabled(true);
        fiscalCodeField.setEnabled(true);
        birthDateField.setEnabled(true);
        emailField.setEnabled(true);
        passwordField.setEnabled(true);
        confirmationField.setEnabled(true);
    }

    private void configureLayout() {
        setLayout(new GridBagLayout());
        setOpaque(false);
        AuthUi.styleTextField(nameField);
        AuthUi.styleTextField(surnameField);
        AuthUi.styleTextField(fiscalCodeField);
        AuthUi.styleTextField(birthDateField);
        AuthUi.styleTextField(emailField);
        AuthUi.styleTextField(passwordField);
        AuthUi.styleTextField(confirmationField);
        AuthUi.styleActionButton(registerButton);

        final JLabel passwordHint = new JLabel(
            "Minimo 8 caratteri, con maiuscola, minuscola e numero"
        );
        AuthUi.setStatus(passwordHint, passwordHint.getText(), false);

        final JCheckBox showPassword = new JCheckBox("Mostra password");
        showPassword.setOpaque(false);
        final char echo = passwordField.getEchoChar();
        showPassword.addActionListener(event -> {
            final char selectedEcho = showPassword.isSelected()
                ? (char) 0
                : echo;
            passwordField.setEchoChar(selectedEcho);
            confirmationField.setEchoChar(selectedEcho);
        });
        registerButton.addActionListener(event -> register());
        confirmationField.addActionListener(event -> register());

        final GridBagConstraints constraints = constraints();
        int row = 0;
        addRow(AuthUi.fieldGroup("Nome", nameField), constraints, row++, 0);
        addRow(
            AuthUi.fieldGroup("Cognome", surnameField),
            constraints,
            row++,
            7
        );
        addRow(
            AuthUi.fieldGroup("Codice fiscale", fiscalCodeField),
            constraints,
            row++,
            7
        );
        addRow(
            AuthUi.fieldGroup(
                "Data di nascita (AAAA-MM-GG)",
                birthDateField
            ),
            constraints,
            row++,
            7
        );
        addRow(
            AuthUi.fieldGroup("Email", emailField),
            constraints,
            row++,
            7
        );
        addRow(
            AuthUi.fieldGroup("Password", passwordField),
            constraints,
            row++,
            7
        );
        addRow(passwordHint, constraints, row++, 2);
        addRow(
            AuthUi.fieldGroup("Conferma password", confirmationField),
            constraints,
            row++,
            7
        );
        addRow(showPassword, constraints, row++, 2);
        addRow(registerButton, constraints, row++, 14);
        addRow(statusLabel, constraints, row, 8);
    }

    private void register() {
        final char[] password = passwordField.getPassword();
        final char[] confirmation = confirmationField.getPassword();
        setBusy(true, "Creazione account...");
        new SwingWorker<SessioneUtente, Void>() {
            @Override
            protected SessioneUtente doInBackground() {
                try {
                    return controller.registra(
                        nameField.getText(),
                        surnameField.getText(),
                        fiscalCodeField.getText(),
                        birthDateField.getText(),
                        emailField.getText(),
                        password,
                        confirmation
                    );
                } finally {
                    Arrays.fill(password, '\0');
                    Arrays.fill(confirmation, '\0');
                }
            }

            @Override
            protected void done() {
                try {
                    final SessioneUtente session = get();
                    passwordField.setText("");
                    confirmationField.setText("");
                    successAction.accept(session);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    setBusy(false, "Registrazione interrotta");
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
        registerButton.setEnabled(!busy);
        nameField.setEnabled(!busy);
        surnameField.setEnabled(!busy);
        fiscalCodeField.setEnabled(!busy);
        birthDateField.setEnabled(!busy);
        emailField.setEnabled(!busy);
        passwordField.setEnabled(!busy);
        confirmationField.setEnabled(!busy);
        AuthUi.setStatus(statusLabel, message, !busy);
    }

    private void addRow(
            final java.awt.Component component,
            final GridBagConstraints constraints,
            final int row,
            final int topInset) {

        constraints.gridy = row;
        constraints.insets = new Insets(topInset, 0, 0, 0);
        add(component, constraints);
    }

    private GridBagConstraints constraints() {
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        return constraints;
    }
}
