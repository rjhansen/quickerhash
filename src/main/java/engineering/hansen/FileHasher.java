package engineering.hansen;

import javax.swing.*;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

class FileHasher extends SwingWorker<byte[], Integer> {
    private static final int BUFFER_SIZE = 1048576;

    private final FileTab owner;
    private MessageDigest digest;
    private String absPath;

    public FileHasher(FileTab owner, String digestName, String path) {
        this.owner = owner;
        try {
            digest = MessageDigest.getInstance(digestName);
            absPath = path;
        } catch (NoSuchAlgorithmException e) {
            reportError("An error occurred while creating the hash engine:\n\n" + e.getMessage());
            digest = null;
            absPath = null;
        }
    }

    @Override
    protected byte[] doInBackground() throws Exception {
        if (digest == null || absPath == null) return null;
        hashFile();
        return isCancelled() ? null : digest.digest();
    }

    private void hashFile() throws Exception {
        digest.reset();
        long totalBytesRead = 0;
        var buffer = new byte[BUFFER_SIZE];

        try (var fh = new FileInputStream(absPath)) {
            var filesize = Files.size(Paths.get(absPath));
            int bytesRead;

            while (!isCancelled() && (bytesRead = fh.read(buffer)) > 0) {
                digest.update(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
                publish(AllTabs.percentComplete(totalBytesRead, filesize));
            }
        }
    }

    @Override
    protected void process(List<Integer> chunks) {
        owner.updateProgressBar(chunks.getLast());
    }

    @Override
    protected void done() {
        owner.endFileHashing();

        if (isCancelled()) {
            owner.setFileHash(false, null);
            return;
        }

        try {
            byte[] result = get();
            owner.setFileHash(result != null, result);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            reportError("An error occurred while reading the file:\n\n" + cause.getMessage());
        }
    }

    private void reportError(String message) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(owner, message, "I’m sorry…", JOptionPane.ERROR_MESSAGE));
    }
}
