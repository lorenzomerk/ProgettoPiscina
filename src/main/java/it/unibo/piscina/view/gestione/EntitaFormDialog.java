package it.unibo.piscina.view.gestione;

import it.unibo.piscina.model.CampoEntita;
import it.unibo.piscina.model.DefinizioneEntita;
import it.unibo.piscina.model.OpzioneRiferimento;
import it.unibo.piscina.model.TipoCampo;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/** Costruisce form coerenti con i tipi dei campi dichiarati nel catalogo. */
final class EntitaFormDialog {

    private EntitaFormDialog() {
    }

    static List<Object> show(
            final Component parent,
            final Map<CampoEntita, List<OpzioneRiferimento>> references,
            final DefinizioneEntita definition,
            final List<Object> originalRow,
            final boolean editing) {

        final List<CampoEntita> visibleFields = definition.campi().stream()
            .filter(field -> !field.generato())
            .filter(field -> !editing || !field.chiave())
            .toList();
        final Map<CampoEntita, JComponent> components =
            new LinkedHashMap<>();
        final JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        for (int row = 0; row < visibleFields.size(); row++) {
            final CampoEntita field = visibleFields.get(row);
            final Object current = editing
                ? originalRow.get(definition.campi().indexOf(field))
                : defaultValue(field);
            final JComponent component = createComponent(
                references,
                field,
                current
            );
            components.put(field, component);
            addRow(form, row, field, component);
        }

        final JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(null);
        scroll.setPreferredSize(new Dimension(560, 480));
        while (true) {
            final int answer = JOptionPane.showConfirmDialog(
                parent,
                scroll,
                (editing ? "Modifica " : "Nuovo ")
                    + definition.titolo().toLowerCase(),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
            );
            if (answer != JOptionPane.OK_OPTION) {
                return null;
            }
            try {
                final List<Object> values = new ArrayList<>();
                for (Map.Entry<CampoEntita, JComponent> entry
                        : components.entrySet()) {
                    values.add(readValue(entry.getKey(), entry.getValue()));
                }
                return values;
            } catch (IllegalArgumentException exception) {
                JOptionPane.showMessageDialog(
                    parent,
                    exception.getMessage(),
                    definition.titolo(),
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private static JComponent createComponent(
            final Map<CampoEntita, List<OpzioneRiferimento>> references,
            final CampoEntita field,
            final Object value) {

        if (field.tipo() == TipoCampo.BOOLEANO) {
            final JComboBox<String> combo =
                new JComboBox<>(new String[]{"true", "false"});
            combo.setSelectedItem(booleanText(value));
            return combo;
        }
        if (field.tipo() == TipoCampo.ELENCO) {
            final JComboBox<String> combo = new JComboBox<>(
                field.valoriAmmessi().toArray(String[]::new)
            );
            if (value != null) {
                combo.setSelectedItem(value.toString());
            }
            return combo;
        }
        if (field.tipo() == TipoCampo.RIFERIMENTO) {
            final List<OpzioneRiferimento> options =
                references.getOrDefault(field, List.of());
            final JComboBox<OpzioneRiferimento> combo = new JComboBox<>(
                options.toArray(OpzioneRiferimento[]::new)
            );
            selectReference(combo, value);
            return combo;
        }
        if (field.tipo() == TipoCampo.TESTO_LUNGO) {
            final JTextArea area = new JTextArea(valueText(value), 5, 30);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            final JScrollPane scroll = new JScrollPane(area);
            scroll.setPreferredSize(new Dimension(330, 100));
            return scroll;
        }
        return new JTextField(valueText(value), 28);
    }

    private static void selectReference(
            final JComboBox<OpzioneRiferimento> combo,
            final Object value) {

        if (value == null) {
            return;
        }
        for (int index = 0; index < combo.getItemCount(); index++) {
            final OpzioneRiferimento option = combo.getItemAt(index);
            if (String.valueOf(option.valore()).equals(String.valueOf(value))) {
                combo.setSelectedIndex(index);
                return;
            }
        }
    }

    private static Object readValue(
            final CampoEntita field,
            final JComponent component) {

        if (field.tipo() == TipoCampo.RIFERIMENTO) {
            @SuppressWarnings("unchecked")
            final JComboBox<OpzioneRiferimento> combo =
                (JComboBox<OpzioneRiferimento>) component;
            final OpzioneRiferimento selected =
                (OpzioneRiferimento) combo.getSelectedItem();
            if (selected == null && field.obbligatorio()) {
                throw required(field);
            }
            return selected == null ? null : selected.valore();
        }
        if (field.tipo() == TipoCampo.BOOLEANO
                || field.tipo() == TipoCampo.ELENCO) {
            final Object selected =
                ((JComboBox<?>) component).getSelectedItem();
            return field.tipo() == TipoCampo.BOOLEANO
                ? Boolean.valueOf(String.valueOf(selected))
                : selected;
        }

        final String text;
        if (field.tipo() == TipoCampo.TESTO_LUNGO) {
            final JScrollPane scroll = (JScrollPane) component;
            final JTextArea area = (JTextArea) scroll.getViewport().getView();
            text = area.getText().trim();
        } else {
            text = ((JTextField) component).getText().trim();
        }
        if (text.isEmpty()) {
            if (field.obbligatorio()) {
                throw required(field);
            }
            return null;
        }
        try {
            return switch (field.tipo()) {
                case INTERO -> Long.valueOf(text);
                case DECIMALE -> new BigDecimal(text.replace(',', '.'));
                case DATA -> LocalDate.parse(text);
                case ORA -> LocalTime.parse(normalizeTime(text));
                case DATA_ORA -> LocalDateTime.parse(
                    text.replace(' ', 'T')
                );
                default -> text;
            };
        } catch (NumberFormatException | DateTimeParseException exception) {
            throw new IllegalArgumentException(
                "Valore non valido per \"" + field.etichetta()
                    + "\". " + formatHint(field.tipo())
            );
        }
    }

    private static IllegalArgumentException required(
            final CampoEntita field) {

        return new IllegalArgumentException(
            "Il campo \"" + field.etichetta() + "\" è obbligatorio"
        );
    }

    private static String formatHint(final TipoCampo type) {
        return switch (type) {
            case DATA -> "Usare AAAA-MM-GG.";
            case ORA -> "Usare HH:MM oppure HH:MM:SS.";
            case DATA_ORA -> "Usare AAAA-MM-GG HH:MM:SS.";
            case INTERO -> "Inserire un numero intero.";
            case DECIMALE -> "Inserire un numero.";
            default -> "";
        };
    }

    private static String normalizeTime(final String value) {
        return value.length() == 5 ? value + ":00" : value;
    }

    private static Object defaultValue(final CampoEntita field) {
        return switch (field.tipo()) {
            case DATA -> field.obbligatorio() ? LocalDate.now() : null;
            case DATA_ORA -> LocalDateTime.now().withNano(0);
            case BOOLEANO -> Boolean.FALSE;
            default -> null;
        };
    }

    private static String valueText(final Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().withNano(0).toString()
                .replace('T', ' ');
        }
        return value.toString();
    }

    private static String booleanText(final Object value) {
        if (value instanceof Boolean bool) {
            return bool.toString();
        }
        if (value instanceof Number number) {
            return number.intValue() == 0 ? "false" : "true";
        }
        return value == null ? "true" : value.toString();
    }

    private static void addRow(
            final JPanel form,
            final int row,
            final CampoEntita field,
            final Component component) {

        final GridBagConstraints labelConstraints =
            new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.NORTHWEST;
        labelConstraints.insets = new Insets(7, 5, 7, 16);
        form.add(new JLabel(
            field.etichetta() + (field.obbligatorio() ? " *" : "")
        ), labelConstraints);

        final GridBagConstraints fieldConstraints =
            new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(5, 5, 5, 5);
        form.add(component, fieldConstraints);
    }
}
