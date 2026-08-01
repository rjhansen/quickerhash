package engineering.hansen;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.util.SystemFileChooser;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ItemEvent;
import java.io.File;
import java.util.Objects;

public class FileTab extends JPanel {
    private final JButton control = new JButton("Start");
    private final JTextField hash = new JTextField();
    final JButton copy = new JButton("Copy");
    private final JProgressBar progressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
    private final JComboBox<String> fileBox = makeFileBox();
    final JComboBox<String> hashBox = new JComboBox<>();
    FileHasher fileHasher;

    FileTab() {
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setLayout(new BorderLayout());

        add(makeTopPanel(), BorderLayout.NORTH);
        add(makeResultsPanel(), BorderLayout.CENTER);

        wireListeners();
    }

    private static GridBagConstraints gbc(int x, int y, double weightx, int fill) {
        return gbc(x, y, weightx, fill, GridBagConstraints.CENTER);
    }

    private static GridBagConstraints gbc(int x, int y, double weightx, int fill, int anchor) {
        var c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.weightx = weightx;
        c.fill = fill;
        c.anchor = anchor;
        return c;
    }

    void updateProgressBar(int val) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(val);
            progressBar.setString((val == 0) ? "Choose a file and algorithm, then click ‘Start’" : (val + " %"));
        });
    }

    private JComboBox<String> makeFileBox() {
        var model = new DefaultComboBoxModel<String>();
        var fileBox = new JComboBox<>(model);
        fileBox.setFont(AllTabs.getMonospaceFont(12));
        fileBox.setEditable(false);
        fileBox.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                SwingUtilities.invokeLater(() -> fileBox.setPopupVisible(false));
                chooseFile(model, fileBox);
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

    private void chooseFile(DefaultComboBoxModel<String> model, JComboBox<String> fileBox) {
        var fileChooser = new SystemFileChooser();
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileSelectionMode(SystemFileChooser.FILES_ONLY);
        int result = fileChooser.showOpenDialog(this);

        if (result == SystemFileChooser.APPROVE_OPTION) {
            control.setEnabled(true);
            model.removeAllElements();
            File selectedFile = fileChooser.getSelectedFile();
            String path = selectedFile.getAbsolutePath();
            if (model.getIndexOf(path) == -1) {
                model.addElement(path);
            }
            fileBox.setSelectedItem(path);
        } else {
            model.removeAllElements();
            control.setEnabled(false);
        }
    }

    private JPanel makeTopPanel() {
        var topPanel = new JPanel();
        topPanel.setLayout(new GridBagLayout());
        topPanel.add(new JLabel("Hash this file: "), gbc(0, 0, 0.0, GridBagConstraints.NONE));
        topPanel.add(fileBox, gbc(1, 0, 1.0, GridBagConstraints.HORIZONTAL));
        topPanel.add(new JLabel(" with "), gbc(2, 0, 0.0, GridBagConstraints.NONE));
        topPanel.add(hashBox, gbc(3, 0, 0.0, GridBagConstraints.NONE));
        control.setEnabled(false);
        topPanel.add(control, gbc(4, 0, 0.0, GridBagConstraints.NONE));
        return topPanel;
    }

    private JPanel makeResultsPanel() {
        hash.setFont(AllTabs.getMonospaceFont(12));
        hash.setEditable(false);
        hash.setText("");
        hash.setToolTipText("The hash is displayed here grouped in blocks of eight hexadecimal digits");
        var jsp = new JScrollPane(hash);
        jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        jsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        jsp.setBorder(BorderFactory.createTitledBorder(hash.getBorder(), "Hash value"));

        progressBar.setToolTipText("This progress bar shows how much of the file has been read");
        progressBar.setStringPainted(true);
        progressBar.setString("Choose a file and algorithm, then click ‘Start’");
        var progressRow = new JPanel();
        progressRow.setLayout(new BorderLayout());
        progressRow.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        progressRow.add(new JLabel("Progress: "), BorderLayout.WEST);
        progressRow.add(progressBar, BorderLayout.CENTER);

        copy.setEnabled(false);
        copy.setIcon(new FlatSVGIcon(getClass().getResource("/icons/copy-clipboard.svg")).derive(16, 16));
        var hashRow = new JPanel();
        hashRow.setLayout(new GridBagLayout());
        hashRow.add(jsp, gbc(0, 0, 1.0, GridBagConstraints.HORIZONTAL, GridBagConstraints.LINE_START));
        hashRow.add(copy, gbc(1, 0, 0.0, GridBagConstraints.VERTICAL, GridBagConstraints.LINE_END));

        var panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(progressRow, BorderLayout.NORTH);
        panel.add(hashRow, BorderLayout.SOUTH);
        return panel;
    }

    private void wireListeners() {
        control.setToolTipText("This button starts (and cancels) hashing");
        control.addActionListener(_ -> startOrCancelHashing());
        copy.addActionListener(_ -> copyHashToClipboard());
        hashBox.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                hash.setText("");
                copy.setEnabled(false);
            }
        });
    }

    private void startOrCancelHashing() {
        if (Objects.equals(control.getText(), "Start")) {
            control.setText("Cancel");
            hash.setText("Calculating hash...");
            copy.setEnabled(false);
            fileBox.setEnabled(false);
            hashBox.setEnabled(false);
            updateProgressBar(0);

            fileHasher = new FileHasher(this,
                    Objects.requireNonNull(hashBox.getSelectedItem()).toString(),
                    Objects.requireNonNull(fileBox.getSelectedItem()).toString());
            fileHasher.execute();
        } else { // we're stopping
            if (fileHasher != null) {
                fileHasher.cancel(true);
            }
        }
    }

    private void copyHashToClipboard() {
        var stringSelection = new StringSelection(hash.getText());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    void endFileHashing() {
        updateProgressBar(0);
        hashBox.setEnabled(true);
        progressBar.setString("Choose a file and algorithm, then click ‘Start’");
        control.setText("Start");
        control.setEnabled(true);
        fileBox.setEnabled(true);
    }

    void setFileHash(boolean complete, byte[] contents) {
        if (complete) {
            hash.setText(AllTabs.formatHash(contents));
            copy.setEnabled(true);
        } else {
            hash.setText("Operation cancelled.");
            copy.setEnabled(false);
        }
    }
}
