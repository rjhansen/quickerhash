package engineering.hansen;

import javax.swing.*;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class FileHasher extends SwingWorker<byte[], Integer> {
    private final FileTab mw;
    private MessageDigest digest;
    private String absPath;

    public FileHasher(FileTab thingy, String digestName, String path) {
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