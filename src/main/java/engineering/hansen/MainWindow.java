/* This file is (c) 2026, Robert J. Hansen <rjh@sixdemonbag.org>.
 *
 * This is Free Software, released under the Apache 2.0 license.
 */

package engineering.hansen;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.prefs.Preferences;

public class MainWindow extends JFrame {
    private record HashCategory(JCheckBoxMenuItem checkbox, String key, int bit) {}

    final Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
    final JCheckBoxMenuItem hashesCommon = new JCheckBoxMenuItem("Common");
    final JCheckBoxMenuItem hashesObsolete = new JCheckBoxMenuItem("Obsolete");
    final JCheckBoxMenuItem hashesUS = new JCheckBoxMenuItem("U.S.");
    final JCheckBoxMenuItem hashesRussian = new JCheckBoxMenuItem("Russian");
    final JCheckBoxMenuItem hashesUkrainian = new JCheckBoxMenuItem("Ukrainian");
    final JCheckBoxMenuItem hashesChinese = new JCheckBoxMenuItem("Chinese");
    final JCheckBoxMenuItem hashesExotic = new JCheckBoxMenuItem("Exotics");
    final java.util.List<HashCategory> hashCategoryConfig = java.util.List.of(
            new HashCategory(hashesCommon, "Common", 1),
            new HashCategory(hashesObsolete, "Obsolete", 2),
            new HashCategory(hashesUS, "US", 4),
            new HashCategory(hashesUkrainian, "Ukrainian", 8),
            new HashCategory(hashesRussian, "Russian", 16),
            new HashCategory(hashesChinese, "Chinese", 32),
            new HashCategory(hashesExotic, "Exotic", 64));
    final JTabbedPane tabPane = new JTabbedPane();
    final HashMap<String, HashSet<String>> hashCategories = new HashMap<>();
    final TextTab textTab = new TextTab();
    final FileTab fileTab = new FileTab();
    final DirectoryTab directoryTab = new DirectoryTab();

    void showAbout() {
        JOptionPane.showMessageDialog(this,
                """
                        QuickerHash 1.2 is a simple, effective tool for computing hashes.
                        
                        Copyright ©️ 2026, Robert J. Hansen <rob@hansen.engineering>.
                        
                        This is Free Software: you may use it, share it, and change it
                        under terms of the Apache 2.0 License.""",
                "About QuickerHash",
                JOptionPane.INFORMATION_MESSAGE);
    }

    void populateHashBoxes() {
        var enabled = computeEnabledHashes();
        resetHashBoxes(enabled.stream().sorted().toList());
        refreshTextTabHash();
        fileTab.copy.setEnabled(false);
        directoryTab.copyBtn.setEnabled(false);
        directoryTab.getTableModel().setRowCount(0);
    }

    private HashSet<String> computeEnabledHashes() {
        var enabled = new HashSet<String>();
        int prefNum = 0;
        for (var category : hashCategoryConfig) {
            if (category.checkbox().isSelected()) {
                enabled.addAll(hashCategories.get(category.key()));
                prefNum |= category.bit();
            }
        }
        prefs.putInt("enabled-hashes", prefNum);
        if (enabled.isEmpty())
            enabled.add("MD5");
        return enabled;
    }

    private void resetHashBoxes(java.util.List<String> hashes) {
        textTab.hashBox.removeAllItems();
        fileTab.hashBox.removeAllItems();
        directoryTab.hashBox.removeAllItems();
        for (var hash : hashes) {
            textTab.hashBox.addItem(hash);
            fileTab.hashBox.addItem(hash);
            directoryTab.hashBox.addItem(hash);
        }
        textTab.hashBox.setSelectedIndex(0);
        fileTab.hashBox.setSelectedIndex(0);
        directoryTab.hashBox.setSelectedIndex(0);
    }

    private void refreshTextTabHash() {
        try {
            textTab.digest = MessageDigest.getInstance(Objects.requireNonNull(textTab.hashBox.getSelectedItem()).toString());
        } catch (NoSuchAlgorithmException e) {
            JOptionPane.showMessageDialog(this,
                    "An internal error occurred.\n\nPlease file a bug.",
                    "Internal error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        var hash = textTab.digest.digest(textTab.textEntered ?
                textTab.textArea.getText().getBytes(StandardCharsets.UTF_8) :
                new byte[]{});
        textTab.hash.setText(AllTabs.formatHash(hash));
    }

    JMenuBar makeMenuBar() {
        var mb = new JMenuBar();

        var file = new JMenu("File");
        var fileQuit = new JMenuItem("Quit");
        fileQuit.setToolTipText("Quit QuickerHash");
        fileQuit.setIcon(new FlatSVGIcon(getClass().getResource("/icons/quit.svg")).derive(16, 16));
        fileQuit.addActionListener(_ -> dispose());
        file.add(fileQuit);

        var hashes = new JMenu("Hashes");
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
        hashesChinese.setToolTipText("Chinese government standard hashes");
        hashesChinese.setIcon(new FlatSVGIcon(getClass().getResource("/icons/china.svg")).derive(16, 16));
        hashesExotic.addActionListener(_ -> populateHashBoxes());
        hashesExotic.setToolTipText("Exotic hashes");
        hashesExotic.setIcon(new FlatSVGIcon(getClass().getResource("/icons/propeller.svg")).derive(16, 16));
        hashes.add(hashesCommon);
        hashes.add(hashesObsolete);
        hashes.add(hashesUS);
        hashes.add(hashesUkrainian);
        hashes.add(hashesRussian);
        hashes.add(hashesChinese);
        hashes.add(hashesExotic);
        int enabledHashes = prefs.getInt("enabled-hashes", 1);
        hashesCommon.setSelected((enabledHashes & 1) > 0);
        hashesObsolete.setSelected((enabledHashes & 2) > 0);
        hashesUS.setSelected((enabledHashes & 4) > 0);
        hashesUkrainian.setSelected((enabledHashes & 8) > 0);
        hashesRussian.setSelected((enabledHashes & 16) > 0);
        hashesChinese.setSelected((enabledHashes & 32) > 0);
        hashesExotic.setSelected((enabledHashes & 64) > 0);

        var help = new JMenu("Help");
        var helpAbout = new JMenuItem("About");
        helpAbout.setToolTipText("Show information about QuickerHash");
        helpAbout.setIcon(new FlatSVGIcon(getClass().getResource("/icons/about.svg")).derive(16, 16));
        helpAbout.addActionListener(_ -> showAbout());
        var helpLatest = new JMenuItem("Get the latest release");
        helpLatest.setToolTipText("Visit the QuickerHash release page (requires internet connection)");
        helpLatest.setIcon(new FlatSVGIcon(getClass().getResource("/icons/download.svg")).derive(16, 16));
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
        var helpReport = new JMenuItem("Report a Bug");
        helpReport.setToolTipText("Report a bug to the QuickerHash maintainer (requires internet connection)");
        helpReport.setIcon(new FlatSVGIcon(getClass().getResource("/icons/bug.svg")).derive(16, 16));
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
        help.add(helpAbout);
        help.add(helpLatest);
        help.add(helpReport);

        mb.add(file);
        mb.add(hashes);
        mb.add(help);
        return mb;
    }

    private static java.util.List<Image> loadIconImages() {
        try {
            Image original = ImageIO.read(Objects.requireNonNull(QuickerHash.class.getResource("/icons/QuickerHash.png")));
            return java.util.List.of(
                    original.getScaledInstance(16, 16, Image.SCALE_SMOOTH),
                    original.getScaledInstance(32, 32, Image.SCALE_SMOOTH),
                    original.getScaledInstance(48, 48, Image.SCALE_SMOOTH),
                    original.getScaledInstance(64, 64, Image.SCALE_SMOOTH),
                    original.getScaledInstance(128, 128, Image.SCALE_SMOOTH),
                    original.getScaledInstance(256, 256, Image.SCALE_SMOOTH));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void initHashCategories() {
        for (var foo : new String[]{"Common", "Obsolete", "US", "Ukrainian", "Russian", "Chinese", "Exotic"})
            hashCategories.put(foo, new HashSet<>());
        for (var common : new String[]{"MD5", "SHA-1", "SHA-256"}) hashCategories.get("Common").add(common);
        for (var obs : new String[]{"MD2", "MD4", "MD5", "SHA-1", "RIPEMD128", "RIPEMD160"})
            hashCategories.get("Obsolete").add(obs);
        for (var US : new String[]{"SHA-224", "SHA-256", "SHA-384", "SHA-512", "SHA-512/224",
                "SHA-512/256", "SHA3-224", "SHA3-256", "SHA3-384", "SHA3-512"})
            hashCategories.get("US").add(US);
        for (var UA : new String[]{"DSTU7564-256", "DSTU7564-384", "DSTU7564-512"})
            hashCategories.get("Ukrainian").add(UA);
        for (var RU : new String[]{"GOST3411", "GOST3411-2012-256", "GOST3411-2012-512"})
            hashCategories.get("Russian").add(RU);
        for (var CN : new String[]{"SM3"}) hashCategories.get("Chinese").add(CN);
        for (var ex : new String[]{"BLAKE2B-160", "BLAKE2B-256", "BLAKE2B-384", "BLAKE2B-512",
                "BLAKE2S-128", "BLAKE2S-160", "BLAKE2S-224", "BLAKE2S-256", "BLAKE3-256",
                "HARAKA-256", "HARAKA-512", "KECCAK-224", "KECCAK-256", "KECCAK-288", "KECCAK-384",
                "KECCAK-512", "PARALLELHASH128-256", "PARALLELHASH256-512", "RIPEMD256",
                "RIPEMD320", "SHAKE128-256", "SHAKE256-512", "SKEIN-1024-1024", "SKEIN-1024-384",
                "SKEIN-1024-512", "SKEIN-256-128", "SKEIN-256-160", "SKEIN-256-224", "SKEIN-256-256",
                "SKEIN-512-128", "SKEIN-512-160", "SKEIN-512-224", "SKEIN-512-256", "SKEIN-512-384",
                "SKEIN-512-512", "TIGER", "TUPLEHASH128-256", "TUPLEHASH256-512", "WHIRLPOOL"})
            hashCategories.get("Exotic").add(ex);
    }

    public MainWindow() {
        super("QuickerHash");
        setIconImages(loadIconImages());
        initHashCategories();
        setJMenuBar(makeMenuBar());
        populateHashBoxes();
        tabPane.addTab("Hash text", textTab);
        tabPane.addTab("Hash a file", fileTab);
        tabPane.addTab("Hash a directory", directoryTab);
        getContentPane().add(tabPane, BorderLayout.CENTER);
    }
}
