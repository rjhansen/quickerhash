package engineering.hansen;

import java.awt.*;
import java.io.IOException;

public class AllTabs {
    private static Font baseFont;

    public static Font getMonospaceFont(float pointSize) {
        if (baseFont == null) {
            baseFont = loadBaseFont();
        }
        return baseFont.deriveFont(pointSize);
    }

    private static Font loadBaseFont() {
        try {
            var fontFile = AllTabs.class.getResource("/fonts/JetBrainsMono-Regular.ttf");
            assert fontFile != null;
            return Font.createFont(Font.TRUETYPE_FONT, fontFile.openStream());
        } catch (IOException | FontFormatException e) {
            return new Font(Font.MONOSPACED, Font.PLAIN, 12);
        }
    }

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    public static String formatHash(byte[] bytes) {
        var out = new StringBuilder(bytes.length * 2 + bytes.length / 4);
        int hexIndex = 0;
        for (byte b : bytes) {
            for (int shift = 4; shift >= 0; shift -= 4) {
                if (hexIndex > 0 && hexIndex % 8 == 0) out.append(' ');
                out.append(HEX_DIGITS[(b >> shift) & 0xF]);
                hexIndex++;
            }
        }
        return out.toString();
    }

    public static int percentComplete(long bytesRead, long totalBytes) {
        return (int) (100.0 * ((float) bytesRead / (float) totalBytes));
    }
}
