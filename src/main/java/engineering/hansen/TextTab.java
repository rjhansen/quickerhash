package engineering.hansen;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import javax.swing.text.AbstractDocument;

public class TextTab extends JPanel {
    final JTextArea textArea = new JTextArea();
    final JComboBox<String> hashBox = new JComboBox<>();
    MessageDigest digest = null;
    boolean textEntered = false;
    Color originalColor;
    final HashComparator hc = new HashComparator();

    public TextTab() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(makeTextEntryRegion(), gbc(0, 0, 1.0, 1.0, GridBagConstraints.BOTH));
        add(makeAlgorithmPanel(), gbc(0, 1, 0.0, 0.0, GridBagConstraints.BOTH));
        add(hc, gbc(0, 2, 0.0, 0.0, GridBagConstraints.BOTH));

        wireListeners();
    }

    private static GridBagConstraints gbc(int x, int y, double weightx, double weighty, int fill) {
        return gbc(x, y, weightx, weighty, fill, GridBagConstraints.CENTER);
    }

    private static GridBagConstraints gbc(int x, int y, double weightx, double weighty, int fill, int anchor) {
        var c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.weightx = weightx;
        c.weighty = weighty;
        c.fill = fill;
        c.anchor = anchor;
        return c;
    }

    private JScrollPane makeTextEntryRegion() {
        textArea.setLineWrap(false);
        textArea.setEditable(true);
        textArea.setEnabled(true);
        textArea.setFont(AllTabs.getMonospaceFont(12));
        textArea.setToolTipText("Enter your text here");
        originalColor = textArea.getForeground();
        textArea.setForeground(Color.GRAY);
        textArea.setText("Anything you type here will be hashed.");
        textArea.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (!textEntered) {
                    textArea.setForeground(originalColor);
                    textArea.setText("");
                    textEntered = true;
                }
            }
        });

        var scrollPane = new JScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setBorder(BorderFactory.createTitledBorder(scrollPane.getBorder(), "Enter text here"));
        return scrollPane;
    }

    private JPanel makeAlgorithmPanel() {
        hashBox.setEditable(false);

        var panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        panel.setLayout(new FlowLayout());
        panel.add(new JLabel("Hash algorithm: "));
        panel.add(hashBox);
        return panel;
    }

    private void wireListeners() {
        hashBox.addActionListener(_ -> onAlgorithmChanged());
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onTextChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onTextChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onTextChanged();
            }
        });
    }

    private void onAlgorithmChanged() {
        if (hashBox.getModel().getSize() == 0) {
            return;
        }
        try {
            digest = MessageDigest.getInstance(Objects.requireNonNull(hashBox.getSelectedItem()).toString());
        } catch (NoSuchAlgorithmException e) {
            JOptionPane.showMessageDialog(this,
                    "An internal error occurred.\n\nPlease file a bug.",
                    "Internal error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        String text = textEntered ? textArea.getText() : "";
        hc.getData().setText(AllTabs.formatHash(digest.digest(text.getBytes(StandardCharsets.UTF_8))));
    }

    private void onTextChanged() {
        digest.reset();
        hc.getData().setText(AllTabs.formatHash(digest.digest(textArea.getText().getBytes(StandardCharsets.UTF_8))));
    }

}
