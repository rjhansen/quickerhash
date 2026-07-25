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
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Objects;

class Hasher implements Runnable {
    private final MainWindow mw;
    private final MessageDigest digest;
    private final String absPath;

    public Hasher(MainWindow thingy) {
        mw = thingy;
        digest = mw.engines.get(mw.hashBox.getSelectedIndex());
        absPath = Objects.requireNonNull(mw.fileBox.getSelectedItem()).toString();
    }

    @Override
    public void run() {
        FileInputStream fh = null;
        long totalBytesRead = 0;
        digest.reset();

        try {
            var filesize = Files.size(Paths.get(absPath));
            fh = new FileInputStream(absPath);
            var bytes = fh.readNBytes(1048576);

            while (mw.getIsHashing() && 0 < bytes.length) {
                digest.update(bytes);
                totalBytesRead += bytes.length;
                int fracDone = (int) (100.0 * ((float) totalBytesRead / (float) filesize));
                mw.updateProgressBar(fracDone);
                bytes = fh.readNBytes(1048576);
            }

            if (mw.getIsHashing()) { // we ended normally at EOF
                SwingUtilities.invokeLater(() -> mw.fileHash.setText(mw.formatHash(digest.digest())));
            } else {
                SwingUtilities.invokeLater(() -> mw.fileHash.setText(""));
            }
        } catch (Exception e) {
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(mw,
                            "An error occurred while reading the file:\n\n" + e.getMessage(),
                            "I’m sorry…",
                            JOptionPane.ERROR_MESSAGE));
        } finally {
            try {
                if (null != fh)
                    fh.close();
            } catch (IOException ioe) {
                // we've done all we can
            }
            SwingUtilities.invokeLater(() -> {
                mw.updateProgressBar(0);
                mw.hashBox.setEnabled(true);
                mw.progressBar.setString("Choose a file and algorithm, then click ‘Start’");
                mw.hashControl.setText("Start");
                mw.hashControl.setEnabled(true);
                mw.fileBox.setEnabled(true);
                mw.tabPane.setEnabled(true);
            });
            mw.setIsHashing(false);
        }
    }
}

public class MainWindow extends JFrame {
    final JTextArea textArea = new JTextArea();
    final JTextField textHash = new JTextField();
    final JTextField fileHash = new JTextField();
    final ArrayList<MessageDigest> engines = new ArrayList<>();
    final JButton hashControl = new JButton("Start");
    final JProgressBar progressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
    final JComboBox<String> hashBox;
    final JComboBox<String> fileBox;
    final JTabbedPane tabPane = new JTabbedPane();
    private boolean isHashing = false;
    private boolean textEntered = false;
    private Color originalColor;

    String formatHash(byte[] bytes) {
        var hex = HexFormat.of().formatHex(bytes);
        var withSpaces = new StringBuilder();
        for (int i = 0 ; i < hex.length() ; i++) {
            if ((i > 0) && (0 == i % 8)) withSpaces.append(' ');
            withSpaces.append(hex.charAt(i));
        }
        return withSpaces.toString();
    }

    synchronized boolean getIsHashing() {
        return isHashing;
    }

    synchronized void setIsHashing(boolean val) {
        isHashing = val;
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
        topPanel.add(hashBox, gbc);

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

        hashControl.addActionListener(_ -> {
            if (Objects.equals(hashControl.getText(), "Start")) {
                hashControl.setText("Cancel");
                fileHash.setText("Calculating hash...");
                tabPane.setEnabled(false);
                fileBox.setEnabled(false);
                hashBox.setEnabled(false);
                setIsHashing(true);
                updateProgressBar(0);
                new Thread(new Hasher(this)).start();

            } else { // we're stopping
                tabPane.setEnabled(true);
                fileBox.setEnabled(true);
                fileHash.setText("");
                hashControl.setText("Start");
                hashBox.setEnabled(true);
                setIsHashing(false);
                updateProgressBar(0);
                progressBar.setString("Choose a file and algorithm, then click ‘Start’");
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
        var textHashBox = makeHashBox();
        textHashBox.setEditable(false);
        textHashBox.setSelectedIndex(0);
        textHashBox.addActionListener(_ -> {
            var md = engines.get(textHashBox.getSelectedIndex());
            md.reset();
            var hash = md.digest(textArea.getText().getBytes(StandardCharsets.UTF_8));
            textHash.setText(formatHash(hash));
        });
        textHash.setFont(new Font("Monospaced", Font.BOLD, 12));
        textHash.setEditable(false);
        textHash.setText("");
        var md = engines.get(textHashBox.getSelectedIndex());
        md.reset();
        var hash = md.digest(textArea.getText().getBytes(StandardCharsets.UTF_8));
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
            public void insertUpdate(DocumentEvent e) { onChange(); }
            @Override
            public void removeUpdate(DocumentEvent e) { onChange(); }
            @Override
            public void changedUpdate(DocumentEvent e) { onChange(); }

            private void onChange() {
                var md = engines.get(textHashBox.getSelectedIndex());
                md.reset();
                var hash = md.digest(textArea.getText().getBytes(StandardCharsets.UTF_8));
                textHash.setText(formatHash((hash)));
            }
        });

        return textTab;
    }

    private JComboBox<String> makeHashBox() {
        var hashBox = new JComboBox<String>();
        for (MessageDigest digest: engines) {
            hashBox.addItem(digest.getAlgorithm());
        }
        return hashBox;
    }

    private JComboBox<String> makeFileBox() {
        var _this = this;
        var model = new DefaultComboBoxModel<String>();
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
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {}
        });
        return fileBox;
    }

    void showAbout() {
        JOptionPane.showMessageDialog(this,
                """
                        QuickerHash 1.0 is a simple, effective tool for computing hashes.
                        
                        Copyright ©️ 2026, Robert J. Hansen <rob@hansen.engineering>.
                        
                        This is Free Software: you may use it, share it, and change it
                        under terms of the Apache 2.0 License.""",
                "About QuickerHash",
                JOptionPane.INFORMATION_MESSAGE);
    }

    void populateEngine() {
        for (String algoName: new String[]{
                "MD2", "MD5", "SHA-1", "SHA-224", "SHA-256", "SHA-384", "SHA-512",
                "SHA-512/224", "SHA-512/256", "SHA3-224", "SHA3-256", "SHA3-384",
                "SHA3-512", "SHAKE128-256", "SHAKE256-512"
        }) {
            try {
                var md = MessageDigest.getInstance(algoName);
                engines.add(md);
            } catch (NoSuchAlgorithmException nse) {
                // pass: we kind of expect it
            }
        }
    }

    JMenuBar makeMenuBar() {
        var mb = new JMenuBar();
        var file = new JMenu("File");
        var help = new JMenu("Help");
        var fileQuit = new JMenuItem("Quit");
        fileQuit.setIcon(new FlatSVGIcon(getClass().getResource("/icons/quit.svg")).derive(16, 16));
        var helpAbout = new JMenuItem("About");
        helpAbout.setIcon(new FlatSVGIcon(getClass().getResource("/icons/about.svg")).derive(16, 16));
        var helpLatest = new JMenuItem("Get the latest release");
        helpLatest.setIcon(new FlatSVGIcon(getClass().getResource("/icons/download.svg")).derive(16, 16));
        var helpReport = new JMenuItem("Report a Bug");
        helpReport.setIcon(new FlatSVGIcon(getClass().getResource("/icons/bug.svg")).derive(16, 16));

        fileQuit.addActionListener(_ -> dispose());
        helpAbout.addActionListener(_ -> showAbout());

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
        mb.add(help);
        file.add(fileQuit);
        help.add(helpAbout);
        help.add(helpLatest);
        help.add(helpReport);
        return mb;
    }

    public MainWindow() {
        super("QuickerHash");
        setJMenuBar(makeMenuBar());
        populateEngine();
        progressBar.setStringPainted(true);
        progressBar.setString("Choose a file and algorithm, then click ‘Start’");
        hashBox = makeHashBox();
        fileBox = makeFileBox();
        tabPane.addTab("Text", makeTextTab());
        tabPane.addTab("File", makeFileTab());
        getContentPane().add(tabPane, BorderLayout.CENTER);
    }
}
