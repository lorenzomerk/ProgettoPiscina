package it.unibo.piscina.view.utenti;

import it.unibo.piscina.model.Utente;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** Form modale per creare o modificare un'anagrafica. */
final class UtenteFormDialog {

    private UtenteFormDialog() {
    }

    static Utente show(final Component parent, final Utente existing) {
        final JTextField fiscalCode = new JTextField(22);
        final JTextField name = new JTextField(22);
        final JTextField surname = new JTextField(22);
        final JTextField birthDate = new JTextField(22);
        final JTextField email = new JTextField(22);
        final JTextField phone = new JTextField(22);
        final JTextField certificate = new JTextField(22);
        final JCheckBox athlete = new JCheckBox("Atleta");
        final JCheckBox instructor = new JCheckBox("Istruttore");
        final JTextField qualification = new JTextField(22);
        final JCheckBox active = new JCheckBox("Utente attivo", true);
        athlete.setOpaque(false);
        instructor.setOpaque(false);
        active.setOpaque(false);
        qualification.setEnabled(false);
        instructor.addActionListener(event ->
            qualification.setEnabled(instructor.isSelected()));

        populate(
            existing,
            fiscalCode,
            name,
            surname,
            birthDate,
            email,
            phone,
            certificate,
            athlete,
            instructor,
            qualification,
            active
        );
        final JPanel form = createForm(
            existing,
            fiscalCode,
            name,
            surname,
            birthDate,
            email,
            phone,
            certificate,
            athlete,
            instructor,
            qualification,
            active
        );

        while (true) {
            final int answer = JOptionPane.showConfirmDialog(
                parent,
                form,
                existing == null ? "Nuovo utente" : "Modifica utente",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
            );
            if (answer != JOptionPane.OK_OPTION) {
                return null;
            }
            try {
                return new Utente(
                    existing == null ? null : existing.id(),
                    fiscalCode.getText(),
                    name.getText(),
                    surname.getText(),
                    parseRequiredDate(birthDate.getText(), "data di nascita"),
                    email.getText(),
                    phone.getText(),
                    parseOptionalDate(
                        certificate.getText(),
                        "scadenza del certificato"
                    ),
                    existing == null || active.isSelected(),
                    athlete.isSelected(),
                    instructor.isSelected(),
                    qualification.getText(),
                    existing == null
                        ? LocalDate.now()
                        : existing.dataRegistrazione()
                );
            } catch (IllegalArgumentException exception) {
                JOptionPane.showMessageDialog(
                    parent,
                    exception.getMessage(),
                    "Gestione utenti",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private static void populate(
            final Utente existing,
            final JTextField fiscalCode,
            final JTextField name,
            final JTextField surname,
            final JTextField birthDate,
            final JTextField email,
            final JTextField phone,
            final JTextField certificate,
            final JCheckBox athlete,
            final JCheckBox instructor,
            final JTextField qualification,
            final JCheckBox active) {

        if (existing == null) {
            return;
        }
        fiscalCode.setText(existing.codiceFiscale());
        name.setText(existing.nome());
        surname.setText(existing.cognome());
        birthDate.setText(existing.dataNascita().toString());
        email.setText(nullToEmpty(existing.email()));
        phone.setText(nullToEmpty(existing.telefono()));
        certificate.setText(
            existing.scadenzaCertificatoMedico() == null
                ? ""
                : existing.scadenzaCertificatoMedico().toString()
        );
        athlete.setSelected(existing.atleta());
        instructor.setSelected(existing.istruttore());
        qualification.setText(
            nullToEmpty(existing.qualificaIstruttore())
        );
        qualification.setEnabled(existing.istruttore());
        active.setSelected(existing.attivo());
    }

    private static JPanel createForm(
            final Utente existing,
            final JTextField fiscalCode,
            final JTextField name,
            final JTextField surname,
            final JTextField birthDate,
            final JTextField email,
            final JTextField phone,
            final JTextField certificate,
            final JCheckBox athlete,
            final JCheckBox instructor,
            final JTextField qualification,
            final JCheckBox active) {

        final JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        int row = 0;
        addRow(form, row++, "Codice fiscale *", fiscalCode);
        addRow(form, row++, "Nome *", name);
        addRow(form, row++, "Cognome *", surname);
        addRow(form, row++, "Data nascita * (AAAA-MM-GG)", birthDate);
        addRow(form, row++, "Email", email);
        addRow(form, row++, "Telefono", phone);
        addRow(
            form,
            row++,
            "Scadenza certificato (AAAA-MM-GG)",
            certificate
        );
        final JPanel roles = new JPanel();
        roles.setOpaque(false);
        roles.add(athlete);
        roles.add(instructor);
        addRow(form, row++, "Ruoli di dominio", roles);
        addRow(form, row++, "Qualifica istruttore", qualification);
        if (existing != null) {
            addRow(form, row, "Stato", active);
        }
        return form;
    }

    private static void addRow(
            final JPanel form,
            final int row,
            final String label,
            final Component field) {

        final GridBagConstraints labelConstraints =
            new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(5, 5, 5, 14);
        form.add(new JLabel(label), labelConstraints);

        final GridBagConstraints fieldConstraints =
            new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(5, 5, 5, 5);
        form.add(field, fieldConstraints);
    }

    private static LocalDate parseRequiredDate(
            final String value,
            final String fieldName) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                "La " + fieldName + " è obbligatoria"
            );
        }
        return parseDate(value, fieldName);
    }

    private static LocalDate parseOptionalDate(
            final String value,
            final String fieldName) {

        return value == null || value.isBlank()
            ? null
            : parseDate(value, fieldName);
    }

    private static LocalDate parseDate(
            final String value,
            final String fieldName) {

        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                "La " + fieldName + " deve usare il formato AAAA-MM-GG"
            );
        }
    }

    private static String nullToEmpty(final String value) {
        return value == null ? "" : value;
    }
}
