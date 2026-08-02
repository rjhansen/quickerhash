package engineering.hansen;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Objects;

public class HashComparator extends JPanel {
    final int SZ = 64;
    final JTextField data = new JTextField();
    final JTextField exemplar = new JTextField();
    final JLabel okBad = new JLabel();
    final FlatSVGIcon okIcon = new FlatSVGIcon(Objects.requireNonNull(this.getClass().getResource("/icons/ok.svg"))).derive(SZ, SZ);
    final FlatSVGIcon badIcon = new FlatSVGIcon(Objects.requireNonNull(this.getClass().getResource("/icons/bad.svg"))).derive(SZ, SZ);
    final JButton copy = new JButton();

    JTextField getData() {
        return data;
    }

    JTextField getExemplar() {
        return exemplar;
    }

    JButton getCopy() {
        return copy;
    }

    HashComparator() {
        setLayout(new BorderLayout());

        copy.setIcon(new FlatSVGIcon(Objects.requireNonNull(this.getClass().getResource("/icons/copy-clipboard.svg"))).derive(SZ, SZ));
        copy.addActionListener(_ -> {
                    var stringSelection = new StringSelection(data.getText());
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipboard.setContents(stringSelection, null);
                });
        copy.setToolTipText("Copy the computed hash to the clipboard");
        okBad.setToolTipText("Shows whether the expected hash matches the computed hash");

        data.setFont(AllTabs.getMonospaceFont(12));
        data.setEditable(false);
        data.setToolTipText("The hash is displayed here grouped in blocks of eight hexadecimal digits");

        exemplar.setFont(AllTabs.getMonospaceFont(12));
        exemplar.setToolTipText("Enter the hash to match against here");
        ((AbstractDocument) exemplar.getDocument()).setDocumentFilter(AllTabs.hexOrSpaceFilter());

        var scrData = new JScrollPane(data);
        var scrExem = new JScrollPane(exemplar);
        scrData.setBorder(new TitledBorder("Hash value"));
        scrExem.setBorder(new TitledBorder("Expected hash value"));
        add(okBad, BorderLayout.WEST);
        add(copy, BorderLayout.EAST);

        var box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.PAGE_AXIS));
        box.add(scrData);
        box.add(scrExem);
        add(box, BorderLayout.CENTER);

        Runnable update = () -> {
            String actual = data.getText().replace(" ", "");
            String expected = exemplar.getText().replace(" ", "");
            boolean matches = actual.equalsIgnoreCase(expected);
            var icon = matches ? okIcon : badIcon;
            SwingUtilities.invokeLater(() -> okBad.setIcon(icon));
        };
        var listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                update.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                update.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                update.run();
            }
        };
        data.getDocument().addDocumentListener(listener);
        exemplar.getDocument().addDocumentListener(listener);
        update.run();
    }
}
