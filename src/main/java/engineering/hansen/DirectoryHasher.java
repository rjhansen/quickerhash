package engineering.hansen;

import javax.swing.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Stream;

class DirectoryHasher extends SwingWorker<Void, DirectoryHasher.Update> {
    private static final int BUFFER_SIZE = 1048576;

    sealed interface Update permits Progress, Completed {}
    record Progress(String path, int percent) implements Update {}
    record Completed(String path, String hash) implements Update {}

    private final DirectoryTab owner;
    private MessageDigest digest;
    private String absPath;

    public DirectoryHasher(DirectoryTab owner, String digestName, String startRecursionAt) {
        this.owner = owner;
        try {
            digest = MessageDigest.getInstance(digestName);
            absPath = startRecursionAt;
        } catch (NoSuchAlgorithmException e) {
            reportError("An error occurred while creating the hash engine:\n\n" + e.getMessage());
            digest = null;
            absPath = null;
        }
    }

    @Override
    protected Void doInBackground() throws Exception {
        if (digest == null || absPath == null) return null;

        var buffer = new byte[BUFFER_SIZE];

        try (Stream<Path> stream = Files.walk(Path.of(absPath))) {
            var files = stream.filter(Files::isRegularFile)
                    .filter(Files::isReadable)
                    .map(Path::toAbsolutePath)
                    .map(Path::toString)
                    .iterator();

            while (!isCancelled() && files.hasNext()) {
                hashOneFile(files.next(), buffer);
            }
        }
        return null;
    }

    private void hashOneFile(String path, byte[] buffer) {
        digest.reset();
        publish(new Progress(path, 0));

        try (FileInputStream fh = new FileInputStream(path)) {
            var filesize = Files.size(Paths.get(path));
            long totalBytesRead = 0;
            int bytesRead;

            while (!isCancelled() && (bytesRead = fh.read(buffer)) > 0) {
                digest.update(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
                publish(new Progress(path, AllTabs.percentComplete(totalBytesRead, filesize)));
            }

            if (!isCancelled()) {
                publish(new Completed(path, AllTabs.formatHash(digest.digest())));
            }
        } catch (IOException e) {
            // Skip this file but keep walking the rest of the directory,
            // rather than aborting the whole recursive hash.
            publish(new Completed(path, "ERROR: " + e.getMessage()));
        }
    }

    @Override
    protected void process(List<Update> chunks) {
        for (var update : chunks) {
            switch (update) {
                case Progress prog -> {
                    owner.getProgressBar().setValue(prog.percent());
                    owner.getProgressBar().setString(prog.path());
                }
                case Completed done -> {
                    owner.getProgressBar().setValue(0);
                    owner.getTableModel().addRow(new String[]{done.path(), done.hash()});
                }
            }
        }
    }

    @Override
    protected void done() {
        owner.endRecursiveHashing();
        if (!isCancelled()) {
            try {
                get(); // surfaces any exception thrown out of doInBackground()
            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                reportError("An error occurred while hashing the directory:\n\n" + cause.getMessage());
            }
        }
    }

    private void reportError(String message) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(owner, message, "I’m sorry…", JOptionPane.ERROR_MESSAGE));
    }
}
