package engineering.hansen;

import java.awt.*;
import java.io.IOException;
import java.util.HexFormat;
import java.util.Objects;

public class AllTabs {
    public static Font jbMono = null;

    public static Font getMonospaceFont(float pointSize) {
        if (jbMono == null) {
            try {
                var fontFile = AllTabs.class.getResource("/fonts/JetBrainsMono-Regular.ttf");
                jbMono = Font.createFont(Font.TRUETYPE_FONT, fontFile.openStream());
            } catch (IOException | FontFormatException e) {
                jbMono = new Font(Font.MONOSPACED, Font.PLAIN, 12);
            }
        }
        return jbMono.deriveFont(pointSize);
    }

    public static String formatHash(byte[] bytes) {
        var hex = HexFormat.of().formatHex(bytes);
        var withSpaces = new StringBuilder();
        for (int i = 0; i < hex.length(); i++) {
            if ((i > 0) && (0 == i % 8)) withSpaces.append(' ');
            withSpaces.append(hex.charAt(i));
        }
        return withSpaces.toString();
    }
}
