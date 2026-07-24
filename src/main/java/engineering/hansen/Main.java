/* This file is (c) 2026, Robert J. Hansen <rjh@sixdemonbag.org>.
 *
 * This is Free Software, released under the Apache 2.0 license.
 */

package engineering.hansen;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.formdev.flatlaf.util.SystemInfo;

import javax.swing.*;
import java.awt.*;

public class Main {
    static void main() {
        SwingUtilities.invokeLater(() -> {
            try {
                if (SystemInfo.isMacOS) {
                    System.setProperty("apple.laf.useScreenMenuBar", "true");
                    System.setProperty("apple.awt.application.name", "QuickerHash");
                    System.setProperty("apple.awt.application.appearance", "system");
                    UIManager.setLookAndFeel(new FlatMacLightLaf());
                } else {
                    UIManager.setLookAndFeel(new FlatLightLaf());
                }
            } catch (Exception ex) {
                System.err.println("Failed to initialize LaF");
            }
            var mw = new MainWindow();
            if (SystemInfo.isMacFullWindowContentSupported)
                mw.getRootPane().putClientProperty("apple.awt.transparentTitleBar", true);

            mw.setLocationRelativeTo(null);
            mw.setSize(1000, 300);
            mw.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            mw.setVisible(true);
        });
    }
}
