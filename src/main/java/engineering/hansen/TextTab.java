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

public class TextTab extends JPanel {
    final JTextField hash = new JTextField();
    final JButton copyBtn = new JButton("Copy");
    final JTextArea textArea = new JTextArea();
    final JComboBox<String> hashBox = new JComboBox<>();
    MessageDigest digest = null;
    boolean textEntered = false;
    Color originalColor;

    public TextTab() {
        var textEntryRegion = makeTextEntryRegion();
        textArea.setToolTipText("Enter your text here");
        hash.setToolTipText("The hash is displayed here grouped in blocks of eight hexadecimal digits");
        hashBox.setEditable(false);
        hashBox.addActionListener(_ -> {
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
            String text = (textArea.getForeground() == Color.GRAY) ? "" : textArea.getText();
            var hash = AllTabs.formatHash(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
            this.hash.setText(hash);
        });
        hash.setFont(AllTabs.getMonospaceFont(12));
        hash.setEditable(false);
        hash.setText("");

        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        var gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        add(textEntryRegion, gbc);

        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        var mid = new JPanel();
        mid.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        mid.setLayout(new FlowLayout());
        mid.add(new JLabel("Hash algorithm: "));
        mid.add(hashBox);
        add(mid, gbc);

        var jsp = new JScrollPane(this.hash);
        jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        jsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        jsp.setBorder(BorderFactory.createTitledBorder(this.hash.getBorder(), "Hash value"));

        var foo = new JPanel();
        foo.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        foo.add(jsp, gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.anchor = GridBagConstraints.LINE_END;
        copyBtn.setEnabled(true);
        foo.add(copyBtn, gbc);

        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        add(foo, gbc);

        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onChange();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onChange();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onChange();
            }

            private void onChange() {
                digest.reset();
                var hash = digest.digest(textArea.getText().getBytes(StandardCharsets.UTF_8));
                TextTab.this.hash.setText(AllTabs.formatHash((hash)));
            }
        });

        copyBtn.addActionListener(_ -> {
            StringSelection stringSelection = new StringSelection(this.hash.getText());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);
        });
        copyBtn.setIcon(new FlatSVGIcon(getClass().getResource("/icons/copy-clipboard.svg")).derive(16, 16));
    }

    private JScrollPane makeTextEntryRegion() {
        textArea.setLineWrap(false);
        textArea.setEditable(true);
        textArea.setEnabled(true);
        textArea.setFont(AllTabs.getMonospaceFont(12));
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

        var foo = new JScrollPane(textArea);
        foo.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        foo.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        foo.setBorder(BorderFactory.createTitledBorder(foo.getBorder(), "Enter text here"));
        return foo;
    }
}
