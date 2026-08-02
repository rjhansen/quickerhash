package engineering.hansen;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.util.SystemFileChooser;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.util.Objects;

public class FileTab extends JPanel {
    private final JButton control = new JButton("Start");
    private final JProgressBar progressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
    private final JComboBox<String> fileBox = makeFileBox();
    final JComboBox<String> hashBox = new JComboBox<>();
    final HashComparator hc = new HashComparator();
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
        progressBar.setToolTipText("This progress bar shows how much of the file has been read");
        progressBar.setStringPainted(true);
        progressBar.setString("Choose a file and algorithm, then click ‘Start’");
        var progressRow = new JPanel();
        progressRow.setLayout(new BorderLayout());
        progressRow.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        progressRow.add(new JLabel("Progress: "), BorderLayout.WEST);
        progressRow.add(progressBar, BorderLayout.CENTER);

        var panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(progressRow, BorderLayout.NORTH);
        panel.add(hc, BorderLayout.SOUTH);
        return panel;
    }

    private void wireListeners() {
        control.setToolTipText("This button starts (and cancels) hashing");
        control.addActionListener(_ -> startOrCancelHashing());

        hashBox.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                hc.getData().setText("");
                hc.getCopy().setEnabled(false);
            }
        });
    }

    private void startOrCancelHashing() {
        if (Objects.equals(control.getText(), "Start")) {
            control.setText("Cancel");
            hc.getData().setText("Calculating hash...");
            hc.getCopy().setEnabled(false);
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
            hc.getData().setText(AllTabs.formatHash(contents));
            hc.getCopy().setEnabled(true);
        } else {
            hc.getData().setText("Operation cancelled.");
            hc.getCopy().setEnabled(false);
        }
    }
}
