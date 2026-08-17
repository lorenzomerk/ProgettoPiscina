package it.unibo.piscina.view.theme;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Colori e componenti comuni dell'interfaccia. */
public final class PoolTheme {

    public static final Color NAVY = new Color(8, 49, 77);
    public static final Color POOL_BLUE = new Color(0, 126, 167);
    public static final Color AQUA = new Color(0, 188, 212);
    public static final Color PALE_AQUA = new Color(226, 247, 250);
    public static final Color PAGE_BACKGROUND = new Color(241, 247, 249);
    public static final Color SURFACE = Color.WHITE;
    public static final Color TEXT = new Color(29, 53, 65);
    public static final Color MUTED_TEXT = new Color(91, 112, 122);
    public static final Color SUCCESS = new Color(25, 118, 84);
    public static final Color SUCCESS_BACKGROUND =
        new Color(220, 245, 234);
    public static final Color DANGER = new Color(174, 45, 45);

    public static final Font TITLE_FONT =
        new Font(Font.SANS_SERIF, Font.BOLD, 28);
    public static final Font PAGE_TITLE_FONT =
        new Font(Font.SANS_SERIF, Font.BOLD, 22);
    public static final Font SECTION_FONT =
        new Font(Font.SANS_SERIF, Font.BOLD, 18);
    public static final Font BODY_FONT =
        new Font(Font.SANS_SERIF, Font.PLAIN, 13);

    private PoolTheme() {
    }

    public static JPanel createPageHeader(
            final String title,
            final String subtitle) {

        final JPanel header = new JPanel();
        header.setBackground(NAVY);
        header.setLayout(new javax.swing.BoxLayout(
            header,
            javax.swing.BoxLayout.Y_AXIS
        ));
        header.setBorder(
            BorderFactory.createEmptyBorder(22, 30, 22, 30)
        );

        final JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(PAGE_TITLE_FONT);
        titleLabel.setForeground(Color.WHITE);

        final JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(BODY_FONT);
        subtitleLabel.setForeground(new Color(207, 226, 234));

        header.add(titleLabel);
        header.add(javax.swing.Box.createVerticalStrut(5));
        header.add(subtitleLabel);
        return header;
    }

    public static void stylePrimaryButton(final JButton button) {
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(POOL_BLUE);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));
    }

    public static void styleCardButton(final JButton button) {
        button.setFont(BODY_FONT);
        button.setForeground(TEXT);
        button.setBackground(SURFACE);
        button.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, AQUA),
                BorderFactory.createEmptyBorder(12, 16, 12, 14)
            )
        );
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(final MouseEvent event) {
                button.setBackground(PALE_AQUA);
            }

            @Override
            public void mouseExited(final MouseEvent event) {
                button.setBackground(SURFACE);
            }
        });
    }
}
