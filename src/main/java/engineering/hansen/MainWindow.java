/* This file is (c) 2026, Robert J. Hansen <rjh@sixdemonbag.org>.
 *
 * This is Free Software, released under the Apache 2.0 license.
 */

package engineering.hansen;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.util.SystemFileChooser;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.HexFormat;
import java.util.Objects;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Hasher extends SwingWorker<byte[], Integer> {
    private final MainWindow mw;
    private MessageDigest digest;
    private String absPath;

    public Hasher(MainWindow thingy, String digestName, String path) {
        mw = thingy;
        try {
            digest = MessageDigest.getInstance(digestName);
            absPath = path;
        } catch (NoSuchAlgorithmException e) {
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(mw,
                            "An error occurred while creating the hash engine:\n\n" + e.getMessage(),
                            "I’m sorry…",
                            JOptionPane.ERROR_MESSAGE));
            digest = null;
            absPath = null;
        }
    }

    @Override
    protected byte[] doInBackground() throws Exception {
        if (digest == null || absPath == null) return null;

        digest.reset();
        long totalBytesRead = 0;

        try (var fh = new FileInputStream(absPath)) {
            var filesize = Files.size(Paths.get(absPath));
            var bytes = fh.readNBytes(1048576);

            while (!isCancelled() && bytes.length > 0) {
                digest.update(bytes);
                totalBytesRead += bytes.length;
                int fracDone = (int) (100.0 * ((float) totalBytesRead / (float) filesize));
                publish(fracDone);
                bytes = fh.readNBytes(1048576);
            }
        }

        return isCancelled() ? null : digest.digest();
    }

    @Override
    protected void process(java.util.List<Integer> chunks) {
        mw.updateProgressBar(chunks.getLast());
    }

    @Override
    protected void done() {
        mw.endFileHashing();

        if (isCancelled()) {
            mw.setFileHash(false, null);
            return;
        }

        try {
            byte[] result = get();
            mw.setFileHash(result != null, result);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            JOptionPane.showMessageDialog(mw,
                    "An error occurred while reading the file:\n\n" + cause.getMessage(),
                    "I’m sorry…",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}

public class MainWindow extends JFrame {
    final JTextArea textArea = new JTextArea();
    final JTextField textHash = new JTextField();
    final JTextField fileHash = new JTextField();
    final JRadioButtonMenuItem hashesCommon = new JRadioButtonMenuItem("Only show commonly-used hashes");
    final JRadioButtonMenuItem hashesExotic = new JRadioButtonMenuItem("Show all hashes, including exotics");
    final JButton hashControl = new JButton("Start");
    final JProgressBar progressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
    final JComboBox<String> fileHashBox = new JComboBox<>();
    final JComboBox<String> textHashBox = new JComboBox<>();
    final JComboBox<String> fileBox = makeFileBox();
    final JTabbedPane tabPane = new JTabbedPane();
    MessageDigest textDigest = null;
    boolean textEntered = false;
    Color originalColor;
    Hasher hasher;

    void endFileHashing() {
        updateProgressBar(0);
        fileHashBox.setEnabled(true);
        progressBar.setString("Choose a file and algorithm, then click ‘Start’");
        hashControl.setText("Start");
        hashControl.setEnabled(true);
        fileBox.setEnabled(true);
    }

    void setFileHash(boolean complete, byte[] contents) {
        if (complete) {
            fileHash.setText(formatHash(contents));
        } else {
            fileHash.setText("Operation cancelled.");
        }
    }

    String formatHash(byte[] bytes) {
        var hex = HexFormat.of().formatHex(bytes);
        var withSpaces = new StringBuilder();
        for (int i = 0 ; i < hex.length() ; i++) {
            if ((i > 0) && (0 == i % 8)) withSpaces.append(' ');
            withSpaces.append(hex.charAt(i));
        }
        return withSpaces.toString();
    }

    private JScrollPane makeTextEntryRegion() {
        textArea.setLineWrap(false);
        textArea.setEditable(true);
        textArea.setEnabled(true);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        originalColor = textArea.getForeground();
        textArea.setForeground(Color.GRAY);
        textArea.setText("Anything you type here will be hashed.");
        textArea.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (! textEntered) {
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

    private JPanel makeFileTab() {
        fileHash.setFont(new Font("Monospaced", Font.BOLD, 12));
        fileHash.setEditable(false);
        fileHash.setText("");
        fileHash.setToolTipText("The hash is displayed here grouped in blocks of eight hexadecimal digits");
        progressBar.setToolTipText("This progress bar shows how much of the file has been read");
        var jsp = new JScrollPane(fileHash);
        jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        jsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        jsp.setBorder(BorderFactory.createTitledBorder(textHash.getBorder(), "Hash value"));

        var fileTab = new JPanel();
        fileTab.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        fileTab.setLayout(new BorderLayout());

        var topPanel = new JPanel();
        topPanel.setLayout(new GridBagLayout());
        var label = new JLabel("Hash this: ");
        var gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        topPanel.add(label, gbc);

        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        topPanel.add(fileBox, gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        topPanel.add(new JLabel(" with "), gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 0;
        topPanel.add(fileHashBox, gbc);

        hashControl.setEnabled(false);
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 0;
        topPanel.add(hashControl, gbc);

        fileTab.add(topPanel, BorderLayout.NORTH);
        var p = new JPanel();
        p.setLayout(new BorderLayout());
        var q = new JPanel();
        q.setLayout(new BorderLayout());
        q.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        q.add(new JLabel("Progress: "), BorderLayout.WEST);
        q.add(progressBar, BorderLayout.CENTER);
        p.add(q, BorderLayout.NORTH);
        p.add(jsp, BorderLayout.SOUTH);
        fileTab.add(p, BorderLayout.CENTER);

        hashControl.setToolTipText("This button starts (and cancels) hashing");
        hashControl.addActionListener(_ -> {
            if (Objects.equals(hashControl.getText(), "Start")) {
                hashControl.setText("Cancel");
                fileHash.setText("Calculating hash...");
                fileBox.setEnabled(false);
                fileHashBox.setEnabled(false);
                updateProgressBar(0);

                hasher = new Hasher(this,
                        Objects.requireNonNull(fileHashBox.getSelectedItem()).toString(),
                        Objects.requireNonNull(fileBox.getSelectedItem()).toString());
                hasher.execute();

            } else { // we're stopping
                if (hasher != null) {
                    hasher.cancel(true);
                }
            }
        });

        return fileTab;
    }

    void updateProgressBar(int val) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(val);
            progressBar.setString((val == 0) ? "Choose a file and algorithm, then click ‘Start’" : (val + " %"));
        });
    }

    private JPanel makeTextTab() {
        var textEntryRegion = makeTextEntryRegion();
        textArea.setToolTipText("Enter your text here");
        textHash.setToolTipText("The hash is displayed here grouped in blocks of eight hexadecimal digits");
        textHashBox.setEditable(false);
        textHashBox.setSelectedIndex(0);
        textHashBox.addActionListener(_ -> {
            if (textHashBox.getModel().getSize() == 0) {
                return;
            }
            try {
                textDigest = MessageDigest.getInstance(Objects.requireNonNull(textHashBox.getSelectedItem()).toString());
            } catch (NoSuchAlgorithmException e) {
                JOptionPane.showMessageDialog(this,
                        "An internal error occurred.\n\nPlease file a bug.",
                        "Internal error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            String text = (textArea.getForeground() == Color.GRAY) ? "" : textArea.getText();
            var hash = formatHash(textDigest.digest(text.getBytes(StandardCharsets.UTF_8)));
            textHash.setText(hash);
        });
        textHash.setFont(new Font("Monospaced", Font.BOLD, 12));
        textHash.setEditable(false);
        textHash.setText("");
        try {
            textDigest = MessageDigest.getInstance(Objects.requireNonNull(textHashBox.getSelectedItem()).toString());
        } catch (NoSuchAlgorithmException e) {
            JOptionPane.showMessageDialog(this,
                    "An internal error occurred.\n\nPlease file a bug.",
                    "Internal error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        var hash = textDigest.digest(new byte[] {});
        textHash.setText(formatHash(hash));

        var textTab = new JPanel();
        textTab.setLayout(new GridBagLayout());
        textTab.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        var gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        textTab.add(textEntryRegion, gbc);

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
        mid.add(textHashBox);
        textTab.add(mid, gbc);

        var jsp = new JScrollPane(textHash);
        jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        jsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        jsp.setBorder(BorderFactory.createTitledBorder(textHash.getBorder(), "Hash value"));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        textTab.add(jsp, gbc);

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
                textDigest.reset();
                var hash = textDigest.digest(textArea.getText().getBytes(StandardCharsets.UTF_8));
                textHash.setText(formatHash((hash)));
            }
        });

        return textTab;
    }

        private JComboBox<String> makeFileBox () {
            var model = new DefaultComboBoxModel<String>();
            var _this = this;
            var fileBox = new JComboBox<>(model);
            fileBox.setFont(new Font("Monospaced", Font.PLAIN, 12));
            fileBox.setEditable(false);
            fileBox.addPopupMenuListener(new PopupMenuListener() {
                @Override
                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                    SwingUtilities.invokeLater(() -> fileBox.setPopupVisible(false));

                    var fileChooser = new SystemFileChooser();
                    int result = fileChooser.showOpenDialog(_this);

                    if (result == SystemFileChooser.APPROVE_OPTION) {
                        hashControl.setEnabled(true);
                        model.removeAllElements();
                        File selectedFile = fileChooser.getSelectedFile();
                        String path = selectedFile.getAbsolutePath();
                        if (((DefaultComboBoxModel<String>) fileBox.getModel()).getIndexOf(path) == -1) {
                            model.addElement(path);
                        }
                        fileBox.setSelectedItem(path);
                    } else {
                        model.removeAllElements();
                        hashControl.setEnabled(false);
                    }
                }

                @Override
                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                }

                @Override
                public void popupMenuCanceled(PopupMenuEvent e) {
                }
            });
            return fileBox;
        }

        void showAbout () {
            JOptionPane.showMessageDialog(this,
                    """
                            QuickerHash 1.0 is a simple, effective tool for computing hashes.
                            
                            Copyright ©️ 2026, Robert J. Hansen <rob@hansen.engineering>.
                            
                            This is Free Software: you may use it, share it, and change it
                            under terms of the Apache 2.0 License.""",
                    "About QuickerHash",
                    JOptionPane.INFORMATION_MESSAGE);
        }

    void populateHashBoxes() {
        textHashBox.removeAllItems();
        fileHashBox.removeAllItems();
        if (hashesCommon.isSelected()) {
            for (String algo : new String[]{"MD5", "SHA-1", "SHA-256", "SHA3-256"}) {
                textHashBox.addItem(algo);
                fileHashBox.addItem(algo);
            }
        } else {
            Pattern p = Pattern.compile("^(OID\\.)?\\d+(\\.\\d+)*$");
            for (String algo : new TreeSet<>(Security.getAlgorithms("MessageDigest"))) {
                Matcher m = p.matcher(algo);
                if (m.matches()) continue;
                textHashBox.addItem(algo);
                fileHashBox.addItem(algo);
            }
        }
        textHashBox.setSelectedIndex(0);
        fileHashBox.setSelectedIndex(0);
        fileHash.setText("");
    }

        JMenuBar makeMenuBar () {
            var mb = new JMenuBar();
            var file = new JMenu("File");
            var hashes = new JMenu("Hashes");
            var help = new JMenu("Help");
            var fileQuit = new JMenuItem("Quit");
            fileQuit.setToolTipText("Quit QuickerHash");
            fileQuit.setIcon(new FlatSVGIcon(getClass().getResource("/icons/quit.svg")).derive(16, 16));
            var bg = new ButtonGroup();
            bg.add(hashesCommon);
            bg.add(hashesExotic);
            hashesCommon.setSelected(true);

            var helpAbout = new JMenuItem("About");
            helpAbout.setToolTipText("Show information about QuickerHash");
            helpAbout.setIcon(new FlatSVGIcon(getClass().getResource("/icons/about.svg")).derive(16, 16));
            var helpLatest = new JMenuItem("Get the latest release");
            helpLatest.setToolTipText("Visit the QuickerHash release page (requires internet connection)");
            helpLatest.setIcon(new FlatSVGIcon(getClass().getResource("/icons/download.svg")).derive(16, 16));
            var helpReport = new JMenuItem("Report a Bug");
            helpReport.setToolTipText("Report a bug to the QuickerHash maintainer (requires internet connection)");
            helpReport.setIcon(new FlatSVGIcon(getClass().getResource("/icons/bug.svg")).derive(16, 16));

            fileQuit.addActionListener(_ -> dispose());
            helpAbout.addActionListener(_ -> showAbout());
            hashesCommon.addActionListener(_ -> populateHashBoxes());
            hashesCommon.setToolTipText("Only show hash algorithms commonly used in the United States");
            hashesExotic.addActionListener(_ -> populateHashBoxes());
            hashesExotic.setToolTipText("Show all the hash algorithms QuickerHash supports");
            hashesExotic.setIcon(new FlatSVGIcon(getClass().getResource("/icons/propeller.svg")).derive(16, 16));

            helpLatest.addActionListener(_ -> {
                try {
                    Desktop.getDesktop().browse(new URI("https://github.com/rjhansen/quickerhash/releases"));
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this,
                            "I/O error: " + e.getMessage(),
                            "I/O error",
                            JOptionPane.ERROR_MESSAGE);
                } catch (URISyntaxException _) {
                    JOptionPane.showMessageDialog(this,
                            "Malformed URI: this is a weird bug",
                            "Malformed URI",
                            JOptionPane.ERROR_MESSAGE);
                }
            });

            helpReport.addActionListener(_ -> {
                try {
                    Desktop.getDesktop().browse(new URI("https://github.com/rjhansen/quickerhash/issues"));
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this,
                            "I/O error: " + e.getMessage(),
                            "I/O error",
                            JOptionPane.ERROR_MESSAGE);
                } catch (URISyntaxException e) {
                    JOptionPane.showMessageDialog(this,
                            "Malformed URI: this is a weird bug",
                            "Malformed URI",
                            JOptionPane.ERROR_MESSAGE);
                }
            });
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
                    desktop.setAboutHandler(_ -> showAbout());
                }
            }

            mb.add(file);
            mb.add(hashes);
            mb.add(help);

            file.add(fileQuit);
            hashes.add(hashesCommon);
            hashes.add(hashesExotic);
            help.add(helpAbout);
            help.add(helpLatest);
            help.add(helpReport);
            return mb;
        }

    public MainWindow() {
            super("QuickerHash");
            setJMenuBar(makeMenuBar());
            populateHashBoxes();
            progressBar.setStringPainted(true);
            progressBar.setString("Choose a file and algorithm, then click ‘Start’");
            tabPane.addTab("Hash text", makeTextTab());
            tabPane.addTab("Hash a file", makeFileTab());
            getContentPane().add(tabPane, BorderLayout.CENTER);
        }
    }
