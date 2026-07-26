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
import java.util.*;
import java.util.prefs.Preferences;

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
    final Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
    final JTextArea textArea = new JTextArea();
    final JTextField textHash = new JTextField();
    final JTextField fileHash = new JTextField();
    final JCheckBoxMenuItem hashesCommon = new JCheckBoxMenuItem("Common");
    final JCheckBoxMenuItem hashesObsolete = new JCheckBoxMenuItem("Obsolete");
    final JCheckBoxMenuItem hashesUS = new JCheckBoxMenuItem("U.S.");
    final JCheckBoxMenuItem hashesRussian = new JCheckBoxMenuItem("Russian");
    final JCheckBoxMenuItem hashesUkrainian = new JCheckBoxMenuItem("Ukrainian");
    final JCheckBoxMenuItem hashesChinese = new JCheckBoxMenuItem("Chinese");
    final JCheckBoxMenuItem hashesExotic = new JCheckBoxMenuItem("Exotics");
    final JButton hashControl = new JButton("Start");
    final JProgressBar progressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
    final JComboBox<String> fileHashBox = new JComboBox<>();
    final JComboBox<String> textHashBox = new JComboBox<>();
    final JComboBox<String> fileBox = makeFileBox();
    final JTabbedPane tabPane = new JTabbedPane();
    final HashMap<String, HashSet<String>> hashCategories = new HashMap<>();
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
                            QuickerHash 1.1 is a simple, effective tool for computing hashes.
                            
                            Copyright ©️ 2026, Robert J. Hansen <rob@hansen.engineering>.
                            
                            This is Free Software: you may use it, share it, and change it
                            under terms of the Apache 2.0 License.""",
                    "About QuickerHash",
                    JOptionPane.INFORMATION_MESSAGE);
        }

    void populateHashBoxes() {
        textHashBox.removeAllItems();
        fileHashBox.removeAllItems();
        HashSet<String> enabled = new HashSet<>();
        int prefNum = 0;
        if (hashesCommon.isSelected()) {
            enabled.addAll(hashCategories.get("Common"));
            prefNum ^= 1;
        }
        if (hashesObsolete.isSelected()) {
            enabled.addAll(hashCategories.get("Obsolete"));
            prefNum ^= 2;
        }
        if (hashesUS.isSelected()) {
            enabled.addAll(hashCategories.get("US"));
            prefNum ^= 4;
        }
        if (hashesUkrainian.isSelected()) {
            enabled.addAll(hashCategories.get("Ukrainian"));
            prefNum ^= 8;
        }
        if (hashesRussian.isSelected()) {
            enabled.addAll(hashCategories.get("Russian"));
            prefNum ^= 16;
        }
        if (hashesChinese.isSelected()) {
            enabled.addAll(hashCategories.get("Chinese"));
            prefNum ^= 32;
        }
        if (hashesExotic.isSelected()) {
            enabled.addAll(hashCategories.get("Exotic"));
            prefNum ^= 64;
        }
        prefs.putInt("enabled-hashes", prefNum);
        if (enabled.isEmpty())
            enabled.add("MD5");

        String[] enArr = enabled.toArray(new String[]{});
        Arrays.sort(enArr);
        for (var s: enArr) {
            textHashBox.addItem(s);
            fileHashBox.addItem(s);
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
            hashesCommon.setToolTipText("Hashes in common use");
            hashesCommon.setIcon(new FlatSVGIcon(getClass().getResource("/icons/common.svg")).derive(16, 16));
            hashesObsolete.addActionListener(_ -> populateHashBoxes());
            hashesObsolete.setToolTipText("Obsolete, insecure hashes");
            hashesObsolete.setIcon(new FlatSVGIcon(getClass().getResource("/icons/warning.svg")).derive(16, 16));
            hashesUS.addActionListener(_ -> populateHashBoxes());
            hashesUS.setToolTipText("U.S. government standard hashes");
            hashesUS.setIcon(new FlatSVGIcon(getClass().getResource("/icons/us.svg")).derive(16, 16));
            hashesUkrainian.addActionListener(_ -> populateHashBoxes());
            hashesUkrainian.setToolTipText("Ukrainian government standard hashes");
            hashesUkrainian.setIcon(new FlatSVGIcon(getClass().getResource("/icons/ukraine.svg")).derive(16, 16));
            hashesRussian.addActionListener(_ -> populateHashBoxes());
            hashesRussian.setToolTipText("Russian government standard hashes");
            hashesRussian.setIcon(new FlatSVGIcon(getClass().getResource("/icons/russia.svg")).derive(16, 16));
            hashesChinese.addActionListener(_ -> populateHashBoxes());
            hashesChinese.setToolTipText("Chinese government standard hashes");;
            hashesChinese.setIcon(new FlatSVGIcon(getClass().getResource("/icons/china.svg")).derive(16, 16));
            hashesExotic.addActionListener(_ -> populateHashBoxes());
            hashesExotic.setToolTipText("Exotic hashes");
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
            hashes.add(hashesObsolete);
            hashes.add(hashesUS);
            hashes.add(hashesUkrainian);
            hashes.add(hashesRussian);
            hashes.add(hashesChinese);
            hashes.add(hashesExotic);
            help.add(helpAbout);
            help.add(helpLatest);
            help.add(helpReport);

            int enabledHashes = prefs.getInt("enabled-hashes", 1);
            hashesCommon.setSelected((enabledHashes & 1) > 0);
            hashesObsolete.setSelected((enabledHashes & 2) > 0);
            hashesUS.setSelected((enabledHashes & 4) > 0);
            hashesUkrainian.setSelected((enabledHashes & 8) > 0);
            hashesRussian.setSelected((enabledHashes & 16) > 0);
            hashesChinese.setSelected((enabledHashes & 32) > 0);
            hashesExotic.setSelected((enabledHashes & 64) > 0);
            return mb;
        }

    public MainWindow() {
            super("QuickerHash");
            for (var foo: new String[] { "Common", "Obsolete", "US", "Ukrainian", "Russian", "Chinese", "Exotic"}) {
                hashCategories.put(foo, new HashSet<>());
            }

            for (var common: new String[] {"MD5", "SHA-1", "SHA-256"}) hashCategories.get("Common").add(common);
            for (var obs: new String[] {"MD2", "MD4", "MD5", "SHA-1", "RIPEMD128", "RIPEMD160"}) hashCategories.get("Obsolete").add(obs);
            for (var US: new String[] {"SHA-224", "SHA-256", "SHA-384", "SHA-512", "SHA-512/224",
                    "SHA-512/256", "SHA3-224", "SHA3-256", "SHA3-384", "SHA3-512"}) hashCategories.get("US").add(US);
            for (var UA: new String[] {"DSTU7564-256", "DSTU7564-384", "DSTU7564-512"}) hashCategories.get("Ukrainian").add(UA);
            for (var RU: new String[] {"GOST3411", "GOST3411-2012-256", "GOST3411-2012-512"}) hashCategories.get("Russian").add(RU);
            for (var CN: new String[] {"SM3"}) hashCategories.get("Chinese").add(CN);
            for (var ex: new String[] {"BLAKE2B-160", "BLAKE2B-256", "BLAKE2B-384", "BLAKE2B-512",
                    "BLAKE2S-128", "BLAKE2S-160", "BLAKE2S-224", "BLAKE2S-256", "BLAKE3-256",
                    "HARAKA-256", "HARAKA-512", "KECCAK-224", "KECCAK-256", "KECCAK-288", "KECCAK-384",
                    "KECCAK-512", "PARALLELHASH128-256", "PARALLELHASH256-512", "RIPEMD256",
                    "RIPEMD320", "SHAKE128-256", "SHAKE256-512", "SKEIN-1024-1024", "SKEIN-1024-384",
                    "SKEIN-1024-512", "SKEIN-256-128", "SKEIN-256-160", "SKEIN-256-224", "SKEIN-256-256",
                    "SKEIN-512-128", "SKEIN-512-160", "SKEIN-512-224", "SKEIN-512-256", "SKEIN-512-384",
                    "SKEIN-512-512", "TIGER", "TUPLEHASH128-256", "TUPLEHASH256-512", "WHIRLPOOL"})
                hashCategories.get("Exotic").add(ex);

            setJMenuBar(makeMenuBar());
            populateHashBoxes();
            progressBar.setStringPainted(true);
            progressBar.setString("Choose a file and algorithm, then click ‘Start’");
            tabPane.addTab("Hash text", makeTextTab());
            tabPane.addTab("Hash a file", makeFileTab());
            getContentPane().add(tabPane, BorderLayout.CENTER);
        }
    }
