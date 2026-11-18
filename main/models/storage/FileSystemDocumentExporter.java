package models.storage;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import models.AppConstants;
import models.CancellationToken;
import models.Document;
import models.DocumentStorageException;

/**
 * {@link DocumentExporter} that writes a document snapshot to a user-chosen file (UC5, D8).
 *
 * <p><b>Owner:</b> Member A (Storage &amp; File I/O).</p>
 *
 * <p><b>Algorithm (required):</b></p>
 * <ol>
 *   <li>{@code parent = target.toAbsolutePath().getParent()}; if it is not an existing directory
 *       &rarr; {@link DocumentStorageException} {@code The folder "<parent>" does not exist.}</li>
 *   <li>Create a temporary file in {@code parent} ({@code Files.createTempFile(parent, ".download-", ".tmp")}).</li>
 *   <li>Write {@code document.content().getBytes(US_ASCII)} in
 *       {@code AppConstants.IO_BUFFER_SIZE_BYTES} chunks, checking the token after each chunk.</li>
 *   <li>Move the temporary file onto {@code target} with {@code REPLACE_EXISTING} (and
 *       {@code ATOMIC_MOVE} when supported).</li>
 *   <li>On any failure or cancellation: delete the temporary file (ignore errors), leave the
 *       target untouched, then throw / re-throw.</li>
 * </ol>
 *
 * <p>Messages name the target and the cause, e.g.
 * {@code Could not write "C:\out\Notes.txt": access denied.} An {@code AccessDeniedException}
 * (read-only target or folder) must produce a message containing "access denied".</p>
 *
 * <p><b>Implementation notes:</b> a read-only target is rejected with "access denied" <em>before</em>
 * anything is written. Windows refuses to replace a read-only file anyway, but POSIX file systems
 * would silently replace it (a rename only needs write access to the folder); the explicit check
 * makes all platforms behave the same. A target that is a folder is rejected, because
 * {@code REPLACE_EXISTING} would otherwise delete an empty folder. On POSIX file systems the
 * temporary file is given {@code rw-r--r--} permissions, because {@code createTempFile} creates it
 * owner-only.</p>
 *
 * <p><b>Snapshot semantics:</b> the exporter only uses the {@link Document} it receives; it never
 * looks the title up in the repository, so a document deleted after it was opened can still be
 * downloaded (D8).</p>
 *
 * <p><b>Threading:</b> Immutable, thread-safe (no fields).</p>
 */
public final class FileSystemDocumentExporter implements DocumentExporter {

    private static final Logger LOGGER = Logger.getLogger(FileSystemDocumentExporter.class.getName());

    /** Prefix of the temporary file written next to the target. */
    private static final String TEMP_PREFIX = ".download-";

    /** Suffix of the temporary file written next to the target. */
    private static final String TEMP_SUFFIX = ".tmp";

    /** Name of the POSIX file attribute view, used to detect POSIX file systems. */
    private static final String POSIX_VIEW = "posix";

    /** Permissions of a downloaded file on POSIX file systems (a normal user file). */
    private static final Set<PosixFilePermission> DOWNLOAD_PERMISSIONS = PosixFilePermissions.fromString("rw-r--r--");

    /** Cause text for access-denied failures; required wording (see class description). */
    private static final String ACCESS_DENIED = "access denied";

    /** Creates the exporter. */
    public FileSystemDocumentExporter() {
        // stateless
    }

    /**
     * {@inheritDoc}
     *
     * <p>See the algorithm in the class description. Tests: A-40..A-44.</p>
     */
    @Override
    public void export(Document document, Path target, CancellationToken token) throws DocumentStorageException {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(token, "token");

        Path absoluteTarget = target.toAbsolutePath();
        Path parent = absoluteTarget.getParent();
        if (parent == null || !Files.isDirectory(parent)) {
            throw new DocumentStorageException("The folder \"" + parent + "\" does not exist.");
        }
        if (Files.isDirectory(absoluteTarget)) {
            throw new DocumentStorageException("Could not write \"" + target + "\": it is a folder.");
        }
        if (Files.exists(absoluteTarget) && !Files.isWritable(absoluteTarget)) {
            throw new DocumentStorageException("Could not write \"" + target + "\": " + ACCESS_DENIED + ".");
        }

        Path temporary = null;
        boolean moved = false;
        try {
            temporary = Files.createTempFile(parent, TEMP_PREFIX, TEMP_SUFFIX);
            if (Files.getFileStore(temporary).supportsFileAttributeView(POSIX_VIEW)) {
                Files.setPosixFilePermissions(temporary, DOWNLOAD_PERMISSIONS);
            }
            writeChunks(document.content().getBytes(StandardCharsets.US_ASCII), temporary, token);
            moveOnto(temporary, absoluteTarget);
            moved = true;
        } catch (IOException ioException) {
            throw new DocumentStorageException("Could not write \"" + target + "\": "
                    + describe(ioException) + ".", ioException);
        } finally {
            if (!moved && temporary != null) {
                deleteTemporary(temporary);
            }
        }
    }

    /**
     * Writes the bytes in {@code IO_BUFFER_SIZE_BYTES} chunks, checking the token before the first
     * and after every chunk.
     *
     * @param bytes     the content
     * @param temporary the file to write
     * @param token     the cancellation token
     * @throws IOException if writing fails
     */
    private static void writeChunks(byte[] bytes, Path temporary, CancellationToken token) throws IOException {
        token.throwIfCancelled();
        try (OutputStream output = Files.newOutputStream(temporary)) {
            for (int offset = 0; offset < bytes.length; offset += AppConstants.IO_BUFFER_SIZE_BYTES) {
                int length = Math.min(AppConstants.IO_BUFFER_SIZE_BYTES, bytes.length - offset);
                output.write(bytes, offset, length);
                token.throwIfCancelled();
            }
        }
    }

    /**
     * Moves the temporary file onto the target, atomically when the file system supports it.
     *
     * @param temporary the fully written temporary file
     * @param target    the destination, replaced if it exists
     * @throws IOException if the move fails
     */
    private static void moveOnto(Path temporary, Path target) throws IOException {
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException notSupported) {
            LOGGER.log(Level.FINE, "Atomic move not supported, falling back to a plain replace", notSupported);
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Deletes the temporary file after a failure; a failure to delete is logged, not thrown, so it
     * never hides the original problem.
     *
     * @param temporary the file to delete
     */
    private static void deleteTemporary(Path temporary) {
        try {
            Files.deleteIfExists(temporary);
        } catch (IOException ioException) {
            LOGGER.log(Level.WARNING, "Could not delete temporary file " + temporary, ioException);
        }
    }

    /**
     * Returns a short, user-presentable cause for an I/O failure.
     *
     * @param ioException the failure
     * @return the cause text, e.g. {@code access denied}
     */
    private static String describe(IOException ioException) {
        if (ioException instanceof AccessDeniedException) {
            return ACCESS_DENIED;
        }
        if (ioException instanceof NoSuchFileException) {
            return "the folder no longer exists";
        }
        if (ioException instanceof FileSystemException fileSystemException
                && fileSystemException.getReason() != null) {
            return fileSystemException.getReason();
        }
        String message = ioException.getMessage();
        return message != null ? message : ioException.getClass().getSimpleName();
    }
}
