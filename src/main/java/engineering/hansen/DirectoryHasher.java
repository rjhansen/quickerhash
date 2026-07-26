package engineering.hansen;

import javax.swing.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;

class DirectoryHasher extends SwingWorker<Void, DirectoryHasher.Update> {
    sealed interface Update permits Progress, Completed {}
    record Progress(String path, int percent) implements Update {}
    record Completed(String path, String hash) implements Update {}

    private final MainWindow mw;
    private MessageDigest digest;
    private String absPath;

    public DirectoryHasher(MainWindow thingy, String digestName, String startRecursionAt) {
        mw = thingy;
        try {
            digest = MessageDigest.getInstance(digestName);
            absPath = startRecursionAt;
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
    protected Void doInBackground() throws Exception {
        if (digest == null || absPath == null) return null;

        try (Stream<Path> stream = Files.walk(Path.of(absPath))) {
            var it = stream.filter(Files::isRegularFile)
                    .filter(Files::isReadable)
                    .map(Path::toAbsolutePath)
                    .map(Path::toString)
                    .iterator();

            while (!isCancelled() && it.hasNext()) {
                String p = it.next();
                digest.reset();
                publish(new Progress(p, 0));

                try (FileInputStream fh = new FileInputStream(p)) {
                    var filesize = Files.size(Paths.get(p));
                    long totalBytesRead = 0;
                    var bytes = fh.readNBytes(1048576);

                    while (!isCancelled() && bytes.length > 0) {
                        digest.update(bytes);
                        totalBytesRead += bytes.length;
                        int fracDone = (int) (100.0 * ((float) totalBytesRead / (float) filesize));
                        publish(new Progress(p, fracDone));
                        bytes = fh.readNBytes(1048576);
                    }

                    if (!isCancelled()) {
                        publish(new Completed(p, mw.formatHash(digest.digest())));
                    }
                } catch (IOException e) {
                    // Skip this file but keep walking the rest of the directory,
                    // rather than aborting the whole recursive hash.
                    publish(new Completed(p, "ERROR: " + e.getMessage()));
                }
            }
        }
        return null;
    }

    @Override
    protected void process(java.util.List<Update> chunks) {
        for (var update : chunks) {
            switch (update) {
                case Progress prog -> {
                    mw.getDirectoryProgressBar().setValue(prog.percent());
                    mw.getDirectoryProgressBar().setString(prog.path());
                }
                case Completed done -> {
                    mw.getDirectoryProgressBar().setValue(0);
                    mw.getTableModel().addRow(new String[] { done.path(), done.hash() });
                }
            }
        }
    }

    @Override
    protected void done() {
        mw.endRecursiveHashing();
        if (!isCancelled()) {
            try {
                get(); // surfaces any exception thrown out of doInBackground()
            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                JOptionPane.showMessageDialog(mw,
                        "An error occurred while hashing the directory:\n\n" + cause.getMessage(),
                        "I’m sorry…",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}