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

    void updateProgressBar(int val) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(val);
            progressBar.setString((val == 0) ? "Choose a file and algorithm, then click ‘Start’" : (val + " %"));
        });
    }

    private JComboBox<String> makeFileBox() {
        var model = new DefaultComboBoxModel<String>();
        var _this = this;
        var fileBox = new JComboBox<>(model);
        fileBox.setFont(AllTabs.getMonospaceFont(12));
        fileBox.setEditable(false);
        fileBox.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                SwingUtilities.invokeLater(() -> fileBox.setPopupVisible(false));

                var fileChooser = new SystemFileChooser();
                fileChooser.setMultiSelectionEnabled(false);
                fileChooser.setFileSelectionMode(SystemFileChooser.FILES_ONLY);
                int result = fileChooser.showOpenDialog(_this);

                if (result == SystemFileChooser.APPROVE_OPTION) {
                    control.setEnabled(true);
                    model.removeAllElements();
                    File selectedFile = fileChooser.getSelectedFile();
                    String path = selectedFile.getAbsolutePath();
                    if (((DefaultComboBoxModel<String>) fileBox.getModel()).getIndexOf(path) == -1) {
                        model.addElement(path);
                    }
                    fileBox.setSelectedItem(path);
                } else {
                    model.removeAllElements();
                    control.setEnabled(false);
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

    FileTab() {
        hash.setFont(AllTabs.getMonospaceFont(12));
        hash.setEditable(false);
        hash.setText("");
        copy.setEnabled(false);
        hash.setToolTipText("The hash is displayed here grouped in blocks of eight hexadecimal digits");
        progressBar.setToolTipText("This progress bar shows how much of the file has been read");
        var jsp = new JScrollPane(hash);
        jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        jsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        jsp.setBorder(BorderFactory.createTitledBorder(hash.getBorder(), "Hash value"));

        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setLayout(new BorderLayout());

        var topPanel = new JPanel();
        topPanel.setLayout(new GridBagLayout());
        var label = new JLabel("Hash this file: ");
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

        control.setEnabled(false);
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 0;
        topPanel.add(control, gbc);

        progressBar.setStringPainted(true);
        progressBar.setString("Choose a file and algorithm, then click ‘Start’");

        add(topPanel, BorderLayout.NORTH);
        var p = new JPanel();
        p.setLayout(new BorderLayout());
        var q = new JPanel();
        q.setLayout(new BorderLayout());
        q.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        q.add(new JLabel("Progress: "), BorderLayout.WEST);
        q.add(progressBar, BorderLayout.CENTER);
        p.add(q, BorderLayout.NORTH);
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
        copy.setEnabled(false);
        foo.add(copy, gbc);
        p.add(foo, BorderLayout.SOUTH);
        add(p, BorderLayout.CENTER);

        control.setToolTipText("This button starts (and cancels) hashing");
        control.addActionListener(_ -> {
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
        });

        copy.addActionListener(_ -> {
            StringSelection stringSelection = new StringSelection(hash.getText());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);
        });
        copy.setIcon(new FlatSVGIcon(getClass().getResource("/icons/copy-clipboard.svg")).derive(16, 16));

        hashBox.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED) {
                hash.setText("");
                copy.setEnabled(false);
            }
        });
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
