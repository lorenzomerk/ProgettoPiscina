package it.unibo.piscina.view.auth;

import static it.unibo.piscina.view.theme.PoolTheme.BODY_FONT;
import static it.unibo.piscina.view.theme.PoolTheme.POOL_BLUE;
import static it.unibo.piscina.view.theme.PoolTheme.TEXT;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** Stili condivisi dai form di accesso e registrazione. */
final class AuthUi {

    private AuthUi() {
    }

    static JPanel fieldGroup(
            final String label,
            final JComponent field) {

        final JPanel group = new JPanel(new BorderLayout(0, 7));
        group.setOpaque(false);
        group.setPreferredSize(new Dimension(420, 59));

        final JLabel labelComponent = new JLabel(label);
        labelComponent.setForeground(TEXT);
        labelComponent.setFont(
            new Font(Font.SANS_SERIF, Font.BOLD, 13)
        );
        group.add(labelComponent, BorderLayout.NORTH);
        group.add(field, BorderLayout.CENTER);
        return group;
    }

    static void styleTextField(final JTextField field) {
        field.setPreferredSize(new Dimension(420, 36));
        field.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(190, 208, 216)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
            )
        );
        field.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
    }

    static void styleActionButton(final JButton button) {
        button.setBackground(POOL_BLUE);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        button.setPreferredSize(new Dimension(420, 44));
        button.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
    }

    static void setStatus(
            final JLabel label,
            final String message,
            final boolean error) {

        label.setFont(BODY_FONT);
        label.setForeground(
            error ? new Color(174, 45, 45) : new Color(91, 112, 122)
        );
        label.setText(message);
    }

    static String errorMessage(final Throwable throwable) {
        return throwable == null || throwable.getMessage() == null
            ? "Operazione non riuscita"
            : throwable.getMessage();
    }
}
