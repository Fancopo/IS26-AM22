package it.polimi.ingsw.am22.controller.server.persistence;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Disk-backed store of in-progress matches.
 *
 * <p>Each started match is written to its own {@code <matchId>.ser} file under
 * a fixed directory. The server saves periodically (and after every move) so
 * that, after a crash, {@link #loadAll()} can resurrect the matches and let
 * players reconnect with their original nicknames.
 *
 * <p>The disk is assumed to be totally reliable (project assumption), so no
 * checksums or replication are needed — only a write-to-temp-then-rename step
 * to avoid leaving a half-written file if the server dies mid-save.
 */
public final class MatchPersistence {

    private static final String DEFAULT_DIR = "persisted_matches";
    private static final String EXT = ".ser";

    private final Path directory;

    public MatchPersistence() {
        this(Paths.get(DEFAULT_DIR));
    }

    public MatchPersistence(Path directory) {
        this.directory = directory;
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            System.err.println("[persistence] Could not create directory "
                    + directory + ": " + e.getMessage());
        }
    }

    /**
     * Writes (or overwrites) the snapshot of one match. Serialization happens
     * onto a temporary file which is then atomically renamed, so a crash
     * during the write never corrupts the previous good snapshot.
     */
    public synchronized void save(MatchSnapshot snapshot) {
        Path target = fileFor(snapshot.matchId());
        Path tmp = target.resolveSibling(snapshot.matchId() + EXT + ".tmp");
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(tmp))) {
            out.writeObject(snapshot);
            out.flush();
        } catch (IOException e) {
            System.err.println("[persistence] Failed to save match "
                    + snapshot.matchId() + ": " + e.getMessage());
            return;
        }
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                System.err.println("[persistence] Failed to commit match "
                        + snapshot.matchId() + ": " + ex.getMessage());
            }
        }
    }

    /** Reads every persisted match. Corrupt or unreadable files are skipped. */
    public synchronized List<MatchSnapshot> loadAll() {
        List<MatchSnapshot> result = new ArrayList<>();
        if (!Files.isDirectory(directory)) {
            return result;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*" + EXT)) {
            for (Path file : stream) {
                MatchSnapshot snapshot = readSnapshot(file);
                if (snapshot != null) {
                    result.add(snapshot);
                }
            }
        } catch (IOException e) {
            System.err.println("[persistence] Could not list saved matches: " + e.getMessage());
        }
        return result;
    }

    /** Removes the persisted snapshot of a match (called when it ends or is aborted). */
    public synchronized void delete(String matchId) {
        try {
            Files.deleteIfExists(fileFor(matchId));
        } catch (IOException e) {
            System.err.println("[persistence] Could not delete match "
                    + matchId + ": " + e.getMessage());
        }
    }

    private MatchSnapshot readSnapshot(Path file) {
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            return (MatchSnapshot) in.readObject();
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            System.err.println("[persistence] Skipping unreadable snapshot "
                    + file + ": " + e.getMessage());
            return null;
        }
    }

    private Path fileFor(String matchId) {
        return directory.resolve(matchId + EXT);
    }
}
