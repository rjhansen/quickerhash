package engineering.hansen;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.util.SystemFileChooser;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.io.StringWriter;
import java.util.Objects;

public class DirectoryTab extends JPanel {
    final JButton hashControl = new JButton("Start");
    final JButton copyBtn = new JButton("Copy");
    final JProgressBar progressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
    final JComboBox<String> hashBox = new JComboBox<>();
    final JComboBox<String> directoryBox = makeDirectoryBox();
    final DefaultTableModel model = makeTableModel();
    final JTable directoryHash = makeTable(model);
    DirectoryHasher hasher;

    DefaultTableModel getTableModel() {
        return model;
    }

    JProgressBar getProgressBar() {
        return progressBar;
    }

    DirectoryTab() {
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setLayout(new BorderLayout());

        add(makeTopPanel(), BorderLayout.NORTH);
        add(makeResultsPanel());
        add(makeBottomPanel(), BorderLayout.SOUTH);

        wireListeners();
        configureTooltips();
    }

    private static DefaultTableModel makeTableModel() {
        return new DefaultTableModel() {
            final String[] columnNames = {"Filename", "Hash"};

            @Override
            public int getColumnCount() {
                return columnNames.length;
            }

            @Override
            public String getColumnName(int index) {
                return columnNames[index];
            }
        };
    }

    private static JTable makeTable(DefaultTableModel model) {
        return new JTable(model) {
            @Serial
            private static final long serialVersionUID = 1L;

            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private JComboBox<String> makeDirectoryBox() {
        var model = new DefaultComboBoxModel<String>();
        var directoryBox = new JComboBox<>(model);
        directoryBox.setToolTipText("Click here to choose which directory to hash");
        directoryBox.setFont(AllTabs.getMonospaceFont(12));
        directoryBox.setEditable(false);
        directoryBox.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                SwingUtilities.invokeLater(() -> directoryBox.setPopupVisible(false));
                chooseDirectory(model, directoryBox);
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
        return directoryBox;
    }

    private void chooseDirectory(DefaultComboBoxModel<String> model, JComboBox<String> directoryBox) {
        var directoryChooser = new SystemFileChooser();
        directoryChooser.setMultiSelectionEnabled(false);
        directoryChooser.setFileSelectionMode(SystemFileChooser.DIRECTORIES_ONLY);
        int result = directoryChooser.showOpenDialog(this);

        if (result == SystemFileChooser.APPROVE_OPTION) {
            hashControl.setEnabled(true);
            hashControl.setText("Start");
            model.removeAllElements();
            File selectedFile = directoryChooser.getSelectedFile();
            String path = selectedFile.getAbsolutePath();
            if (model.getIndexOf(path) == -1) {
                model.addElement(path);
            }
            directoryBox.setSelectedItem(path);
        } else {
            model.removeAllElements();
            hashControl.setEnabled(false);
        }
    }

    private JPanel makeTopPanel() {
        var topPanel = new JPanel();
        topPanel.setLayout(new GridBagLayout());
        topPanel.add(new JLabel("Hash this directory: "), gbc(0, 0, 0.0, GridBagConstraints.NONE));
        topPanel.add(directoryBox, gbc(1, 0, 1.0, GridBagConstraints.HORIZONTAL));
        topPanel.add(new JLabel(" with "), gbc(2, 0, 0.0, GridBagConstraints.NONE));
        topPanel.add(hashBox, gbc(3, 0, 0.0, GridBagConstraints.NONE));
        hashControl.setEnabled(false);
        topPanel.add(hashControl, gbc(4, 0, 0.0, GridBagConstraints.NONE));
        return topPanel;
    }

    private JPanel makeResultsPanel() {
        directoryHash.setFont(AllTabs.getMonospaceFont(12));
        var jsp = new JScrollPane(directoryHash);
        jsp.setBorder(BorderFactory.createTitledBorder(jsp.getBorder(), "Hash results"));

        var resultsPanel = new JPanel();
        resultsPanel.setLayout(new BorderLayout());
        resultsPanel.add(jsp, BorderLayout.CENTER);
        return resultsPanel;
    }

    private JPanel makeBottomPanel() {
        progressBar.setBorder(BorderFactory.createTitledBorder(progressBar.getBorder(), "Hash progress"));
        progressBar.setValue(0);
        progressBar.setFont(AllTabs.getMonospaceFont(10));
        progressBar.setStringPainted(true);
        progressBar.setString("");
        copyBtn.setIcon(new FlatSVGIcon(getClass().getResource("/icons/copy-clipboard.svg")).derive(16, 16));

        var bottomPanel = new JPanel();
        bottomPanel.setLayout(new GridBagLayout());
        bottomPanel.add(progressBar, gbc(0, 0, 1.0, GridBagConstraints.BOTH));
        bottomPanel.add(copyBtn, gbc(1, 0, 0.0, GridBagConstraints.BOTH));
        return bottomPanel;
    }

    private static GridBagConstraints gbc(int x, int y, double weightx, int fill) {
        var c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.weightx = weightx;
        c.fill = fill;
        return c;
    }

    private void wireListeners() {
        hashBox.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                model.setRowCount(0);
                copyBtn.setEnabled(false);
            }
        });
        hashControl.addActionListener(_ -> startOrCancelHashing());
        copyBtn.addActionListener(_ -> copyResultsToClipboard());
    }

    private void startOrCancelHashing() {
        if (Objects.equals(hashControl.getText(), "Start")) {
            model.setRowCount(0);
            directoryHash.setEnabled(false);
            hashControl.setText("Cancel");
            copyBtn.setEnabled(false);
            directoryBox.setEnabled(false);
            hashBox.setEnabled(false);

            hasher = new DirectoryHasher(this,
                    Objects.requireNonNull(hashBox.getSelectedItem()).toString(),
                    Objects.requireNonNull(directoryBox.getSelectedItem()).toString());
            hasher.execute();
        } else { // we're stopping
            if (hasher != null) {
                hasher.cancel(true);
            }
        }
    }

    private void copyResultsToClipboard() {
        var sw = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(sw, CSVFormat.EXCEL)) {
            printer.printRecord("Filename", "Hash");
            for (int i = 0; i < model.getRowCount(); i++) {
                printer.printRecord(model.getValueAt(i, 0), model.getValueAt(i, 1));
            }
        } catch (IOException ex) {
            // pass: it can't fail, as we're writing to memory
        }
        var stringSelection = new StringSelection(sw.toString());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    private void configureTooltips() {
        progressBar.setToolTipText("Shows the progress of the current file being hashed");
        copyBtn.setToolTipText("Click here to copy your hashes to the clipboard in Excel’s CSV format");
        directoryHash.setToolTipText("Filenames and hashes are displayed here");
    }

    void endRecursiveHashing() {
        progressBar.setString("");
        progressBar.setValue(0);
        hashBox.setEnabled(true);
        hashControl.setText("Start");
        hashControl.setEnabled(true);
        directoryBox.setEnabled(true);
        directoryHash.setEnabled(true);
        copyBtn.setEnabled(model.getRowCount() > 0);
    }
}
