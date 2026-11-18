package models.storage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import models.AppConstants;
import models.AsciiText;
import models.Document;
import models.DocumentNotFoundException;
import models.DocumentStorageException;
import models.DocumentTitles;

/**
 * {@link DocumentStore} that keeps every document as a read-only file in one directory
 * (production: {@code ${user.home}/.local-search-engine/documents/}, D4).
 *
 * <p><b>Owner:</b> Member A (Storage &amp; File I/O).</p>
 *
 * <p><b>Requirements covered:</b> D4 (read-only copy that is independent of the user's original),
 * D5 (persistence across runs), D3 (content stored byte-for-byte, line breaks unchanged).</p>
 *
 * <p><b>On-disk format, version 1 (fixed - other tools and the integration tests rely on it):</b></p>
 * <ul>
 *   <li>Each title maps to an <em>entry id</em>: the lower-case hexadecimal SHA-256 of the title's
 *       US-ASCII bytes (64 characters). Rationale: titles are case-sensitive but NTFS/APFS are
 *       case-insensitive ("Foo" and "foo" would collide), titles may contain {@code / \ : * ? " < > |},
 *       and the fixed length stays far below the 255-character file-name limit. See
 *       {@link #entryId(String)}.</li>
 *   <li>Content file {@code <id>.txt}: exactly the document's ASCII bytes; marked read-only.</li>
 *   <li>Metadata file {@code <id>.meta.properties}: a {@code java.util.Properties} file with keys
 *       {@code formatVersion=1}, {@code title}, {@code importedAt} (ISO-8601 {@code Instant}),
 *       {@code length} (content length in bytes). The metadata file is the <em>commit record</em>:
 *       an entry is complete only if both files exist and the content size equals {@code length}.</li>
 *   <li>Temporary files end with {@code .tmp}; they are written in the same directory and moved
 *       into place with {@code Files.move(tmp, target, REPLACE_EXISTING, ATOMIC_MOVE)} (fall back to
 *       {@code REPLACE_EXISTING} alone if {@code AtomicMoveNotSupportedException}). Leftover
 *       {@code .tmp} files and content files without metadata are ignored by
 *       {@link #listStoredTitles()} and may be deleted there (crash recovery).</li>
 * </ul>
 *
 * <p><b>Save order:</b> write {@code <id>.txt.tmp} &rarr; write {@code <id>.meta.properties.tmp}
 * &rarr; if an old content file exists, clear its read-only flag &rarr; move content into place
 * &rarr; mark it read-only &rarr; move metadata into place. A crash at any point leaves either the
 * old entry, the new entry, or an entry detected as corrupt (length mismatch) - never a silently
 * truncated document.</p>
 *
 * <p><b>Windows pitfalls (must be handled):</b> a read-only file can be neither deleted nor
 * replaced by {@code Files.move}: call {@link #makeWritable(Path)} first. Use
 * {@code Path.toFile().setWritable(false)} / {@code setWritable(true)} (works on Windows and POSIX)
 * or the {@code DosFileAttributeView}/{@code PosixFileAttributeView}; do not assume either view
 * exists.</p>
 *
 * <p><b>Implementation notes:</b> {@link #listStoredTitles()} only <em>ignores</em> leftover
 * temporary and orphan files and never deletes them: the repository may list (startup load, R6)
 * while a save is in progress, and deleting that save's {@code .tmp} file would make it fail.
 * {@link #load} also verifies the format version, the stored title, and that every byte is ASCII;
 * any mismatch is reported as a corrupt entry.</p>
 *
 * <p><b>What it does NOT do:</b> no in-memory caching, no locking (the repository serializes
 * mutations), no validation of the user's original file.</p>
 *
 * <p><b>Threading:</b> Not thread-safe for concurrent mutations of the same title (the repository
 * guarantees there are none). Concurrent {@link #load} of different titles is safe because no
 * mutable state is shared.</p>
 */
public final class FileSystemDocumentStore implements DocumentStore {

    /** Logger for crash-recovery cleanup and skipped files. */
    private static final Logger LOGGER = Logger.getLogger(FileSystemDocumentStore.class.getName());

    /** Suffix of content files. */
    static final String CONTENT_SUFFIX = ".txt";

    /** Suffix of metadata (commit record) files. */
    static final String METADATA_SUFFIX = ".meta.properties";

    /** Suffix of temporary files. */
    static final String TEMP_SUFFIX = ".tmp";

    /** Current on-disk format version stored under key {@code formatVersion}. */
    static final int FORMAT_VERSION = 1;

    /** Metadata key: on-disk format version. */
    private static final String KEY_FORMAT_VERSION = "formatVersion";

    /** Metadata key: the exact title. */
    private static final String KEY_TITLE = "title";

    /** Metadata key: import time as an ISO-8601 instant. */
    private static final String KEY_IMPORTED_AT = "importedAt";

    /** Metadata key: content length in bytes. */
    private static final String KEY_LENGTH = "length";

    /** Digest used for entry ids; every JDK must provide it. */
    private static final String DIGEST_ALGORITHM = "SHA-256";

    /** Cause text for access-denied failures, so messages read naturally in error dialogs. */
    private static final String ACCESS_DENIED = "access denied";

    /** Directory holding all entries; created on first use if missing. */
    private final Path directory;

    /**
     * Creates a store for {@code directory}. Does no I/O; the directory is created lazily by the
     * first operation that needs it (and by {@link #listStoredTitles()}).
     *
     * @param directory the storage directory (in tests a {@code @TempDir}), not {@code null}
     * @throws NullPointerException if {@code directory} is {@code null}
     */
    public FileSystemDocumentStore(Path directory) {
        this.directory = Objects.requireNonNull(directory, "directory");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Follows the save order in the class description. Error message example:
     * {@code Could not save the document "Notes": <IOException message>.}
     * Tests: A-20..A-24, A-29.</p>
     */
    @Override
    public void save(Document document) throws DocumentStorageException {
        Objects.requireNonNull(document, "document");
        ensureDirectory();

        String title = document.title();
        String id = entryId(title);
        Path contentPath = contentPath(id);
        Path metadataPath = metadataPath(id);
        Path contentTemp = temporaryPath(contentPath);
        Path metadataTemp = temporaryPath(metadataPath);
        byte[] contentBytes = document.content().getBytes(StandardCharsets.US_ASCII);
        boolean contentMoved = false;
        try {
            Files.write(contentTemp, contentBytes);
            Files.write(metadataTemp, metadataBytes(document, contentBytes.length));
            makeWritable(contentPath);
            moveIntoPlace(contentTemp, contentPath);
            contentMoved = true;
            makeReadOnly(contentPath);
            moveIntoPlace(metadataTemp, metadataPath);
        } catch (IOException ioException) {
            deleteTemporary(contentTemp);
            deleteTemporary(metadataTemp);
            if (!contentMoved && Files.exists(contentPath)) {
                makeReadOnly(contentPath);
            }
            throw new DocumentStorageException("Could not save the document \"" + title + "\": "
                    + describe(ioException) + ".", ioException);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delete metadata first (un-commits the entry), then clear read-only and delete the content
     * file. Missing files are fine. Test: A-25.</p>
     */
    @Override
    public void delete(String title) throws DocumentStorageException {
        Objects.requireNonNull(title, "title");
        String id = entryId(title);
        Path contentPath = contentPath(id);
        try {
            Files.deleteIfExists(metadataPath(id));
            makeWritable(contentPath);
            Files.deleteIfExists(contentPath);
        } catch (IOException ioException) {
            throw new DocumentStorageException("Could not delete the document \"" + title + "\": "
                    + describe(ioException) + ".", ioException);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates the directory if missing; reads every {@code *.meta.properties}; includes the
     * title only if the content file exists and its size equals {@code length}; sorts with
     * {@code AppConstants.TITLE_ORDER}. Incomplete entries are skipped and logged. Tests: A-26, A-29, A-30.</p>
     */
    @Override
    public List<String> listStoredTitles() throws DocumentStorageException {
        ensureDirectory();
        List<String> titles = new ArrayList<>();
        try (DirectoryStream<Path> metadataFiles = Files.newDirectoryStream(directory, "*" + METADATA_SUFFIX)) {
            for (Path metadataFile : metadataFiles) {
                completeEntryTitle(metadataFile).ifPresent(titles::add);
            }
        } catch (IOException ioException) {
            throw new DocumentStorageException("Could not read the document folder \"" + directory + "\": "
                    + describe(ioException) + ".", ioException);
        }
        titles.sort(AppConstants.TITLE_ORDER);
        return List.copyOf(titles);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reads the metadata and the content (US-ASCII), verifies the stored title equals
     * {@code title} and the length matches, and returns
     * {@code new Document(title, content, importedAt)}. Missing metadata &rarr;
     * {@link DocumentNotFoundException}; anything inconsistent &rarr; {@link DocumentStorageException}.
     * Tests: A-20, A-27, A-28.</p>
     */
    @Override
    public Document load(String title) throws DocumentNotFoundException, DocumentStorageException {
        Objects.requireNonNull(title, "title");
        String id = entryId(title);
        Path metadataPath = metadataPath(id);
        if (!Files.isRegularFile(metadataPath)) {
            throw new DocumentNotFoundException(title);
        }
        try {
            Properties metadata = readMetadata(metadataPath);
            Optional<String> problem = metadataProblem(metadata, title);
            if (problem.isPresent()) {
                throw corrupt(title, problem.get());
            }
            long expectedLength = Long.parseLong(metadata.getProperty(KEY_LENGTH));
            Instant importedAt = Instant.parse(metadata.getProperty(KEY_IMPORTED_AT));
            byte[] contentBytes = Files.readAllBytes(contentPath(id));
            if (contentBytes.length != expectedLength) {
                throw corrupt(title, "expected " + expectedLength + " bytes but found " + contentBytes.length);
            }
            for (byte contentByte : contentBytes) {
                if (!AsciiText.isAsciiByte(contentByte)) {
                    throw corrupt(title, "the content contains a non-ASCII byte");
                }
            }
            return new Document(title, new String(contentBytes, StandardCharsets.US_ASCII), importedAt);
        } catch (NoSuchFileException missing) {
            if (Files.isRegularFile(metadataPath)) {
                throw new DocumentStorageException("Could not load the document \"" + title
                        + "\": the stored copy is corrupt (its content file is missing).", missing);
            }
            // The entry was deleted between the existence check and the read.
            DocumentNotFoundException notFound = new DocumentNotFoundException(title);
            notFound.initCause(missing);
            throw notFound;
        } catch (IOException ioException) {
            throw new DocumentStorageException("Could not load the document \"" + title + "\": "
                    + describe(ioException) + ".", ioException);
        }
    }

    /**
     * Computes the entry id of a title: lower-case hex SHA-256 of {@code title.getBytes(US_ASCII)}.
     * Package-private for tests (A-23, A-24).
     *
     * @param title the title
     * @return 64 lower-case hex characters
     */
    static String entryId(String title) {
        Objects.requireNonNull(title, "title");
        try {
            MessageDigest digest = MessageDigest.getInstance(DIGEST_ALGORITHM);
            return HexFormat.of().formatHex(digest.digest(title.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException missingAlgorithm) {
            throw new IllegalStateException("The JDK does not provide " + DIGEST_ALGORITHM, missingAlgorithm);
        }
    }

    /**
     * Returns {@code <directory>/<id>.txt}.
     *
     * @param entryId the entry id
     * @return the content path
     */
    private Path contentPath(String entryId) {
        return directory.resolve(entryId + CONTENT_SUFFIX);
    }

    /**
     * Returns {@code <directory>/<id>.meta.properties}.
     *
     * @param entryId the entry id
     * @return the metadata path
     */
    private Path metadataPath(String entryId) {
        return directory.resolve(entryId + METADATA_SUFFIX);
    }

    /**
     * Returns the temporary sibling of a final path, {@code <target>.tmp}.
     *
     * @param target the final path
     * @return the temporary path in the same directory
     */
    private static Path temporaryPath(Path target) {
        return target.resolveSibling(target.getFileName() + TEMP_SUFFIX);
    }

    /**
     * Creates the storage directory (and parents) if it does not exist.
     *
     * @throws DocumentStorageException if it cannot be created or is not a directory
     */
    private void ensureDirectory() throws DocumentStorageException {
        if (Files.isDirectory(directory)) {
            return;
        }
        if (Files.exists(directory)) {
            throw new DocumentStorageException("The document folder \"" + directory
                    + "\" cannot be used because a file with that name exists.");
        }
        try {
            Files.createDirectories(directory);
        } catch (IOException ioException) {
            throw new DocumentStorageException("Could not create the document folder \"" + directory + "\": "
                    + describe(ioException) + ".", ioException);
        }
    }

    /**
     * Moves a fully written temporary file onto its final path, atomically when supported.
     *
     * @param temporary the temporary file
     * @param target    the final path, replaced if it exists (must be writable on Windows)
     * @throws IOException if the move fails
     */
    private static void moveIntoPlace(Path temporary, Path target) throws IOException {
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException notSupported) {
            LOGGER.log(Level.FINE, "Atomic move not supported, falling back to a plain replace", notSupported);
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Serializes the metadata (commit record) of a document.
     *
     * @param document      the document
     * @param contentLength the content length in bytes
     * @return the properties file bytes (ISO-8859-1 with escapes, ASCII for valid titles)
     * @throws IOException never in practice (in-memory stream), declared by {@link Properties#store}
     */
    private static byte[] metadataBytes(Document document, int contentLength) throws IOException {
        Properties metadata = new Properties();
        metadata.setProperty(KEY_FORMAT_VERSION, Integer.toString(FORMAT_VERSION));
        metadata.setProperty(KEY_TITLE, document.title());
        metadata.setProperty(KEY_IMPORTED_AT, document.importedAt().toString());
        metadata.setProperty(KEY_LENGTH, Integer.toString(contentLength));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        metadata.store(bytes, "Local Search Engine document entry");
        return bytes.toByteArray();
    }

    /**
     * Reads a metadata file.
     *
     * @param metadataPath the file
     * @return the properties
     * @throws IOException if reading fails or the file is not a valid properties file
     */
    private static Properties readMetadata(Path metadataPath) throws IOException {
        Properties metadata = new Properties();
        try (InputStream input = Files.newInputStream(metadataPath)) {
            metadata.load(input);
        } catch (IllegalArgumentException malformed) {
            throw new IOException("the metadata file is malformed", malformed);
        }
        return metadata;
    }

    /**
     * Checks a metadata record for consistency with the expected title.
     *
     * @param metadata      the record
     * @param expectedTitle the title the record must belong to
     * @return a description of the first problem; empty if the record is consistent
     */
    private static Optional<String> metadataProblem(Properties metadata, String expectedTitle) {
        if (!Integer.toString(FORMAT_VERSION).equals(metadata.getProperty(KEY_FORMAT_VERSION))) {
            return Optional.of("unsupported format version " + metadata.getProperty(KEY_FORMAT_VERSION));
        }
        if (!expectedTitle.equals(metadata.getProperty(KEY_TITLE))) {
            return Optional.of("the stored title does not match");
        }
        if (!DocumentTitles.isValid(expectedTitle)) {
            return Optional.of("the stored title is not a valid title");
        }
        try {
            if (Long.parseLong(Objects.requireNonNullElse(metadata.getProperty(KEY_LENGTH), "")) < 0) {
                return Optional.of("negative length");
            }
            Instant.parse(Objects.requireNonNullElse(metadata.getProperty(KEY_IMPORTED_AT), ""));
        } catch (NumberFormatException | DateTimeParseException malformed) {
            return Optional.of("malformed length or import time");
        }
        return Optional.empty();
    }

    /**
     * Returns the title of a complete entry; empty (and logged) if the entry is incomplete,
     * corrupt, or does not belong to its file name.
     *
     * @param metadataFile a {@code *.meta.properties} file in the directory
     * @return the title; empty to skip the entry
     */
    private Optional<String> completeEntryTitle(Path metadataFile) {
        String fileName = metadataFile.getFileName().toString();
        String id = fileName.substring(0, fileName.length() - METADATA_SUFFIX.length());
        try {
            Properties metadata = readMetadata(metadataFile);
            String title = metadata.getProperty(KEY_TITLE);
            if (title == null || !entryId(title).equals(id)) {
                LOGGER.warning("Skipping " + fileName + ": its title does not match the file name.");
                return Optional.empty();
            }
            Optional<String> problem = metadataProblem(metadata, title);
            if (problem.isPresent()) {
                LOGGER.warning("Skipping " + fileName + ": " + problem.get() + ".");
                return Optional.empty();
            }
            Path contentPath = contentPath(id);
            long expectedLength = Long.parseLong(metadata.getProperty(KEY_LENGTH));
            if (!Files.isRegularFile(contentPath) || Files.size(contentPath) != expectedLength) {
                LOGGER.warning("Skipping incomplete entry " + fileName + " (content missing or wrong size).");
                return Optional.empty();
            }
            return Optional.of(title);
        } catch (IOException ioException) {
            LOGGER.log(Level.WARNING, "Skipping unreadable entry " + fileName, ioException);
            return Optional.empty();
        }
    }

    /**
     * Creates the exception for a corrupt entry.
     *
     * @param title   the title
     * @param problem what is wrong
     * @return the exception to throw
     */
    private static DocumentStorageException corrupt(String title, String problem) {
        return new DocumentStorageException("Could not load the document \"" + title
                + "\": the stored copy is corrupt (" + problem + ").");
    }

    /**
     * Deletes a temporary file after a failed save; a failure to delete is logged, not thrown, so it
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
     * Marks a file read-only (D4). Failure to set the flag is logged as a warning, not thrown,
     * because some file systems do not support it.
     *
     * @param file the file
     */
    private static void makeReadOnly(Path file) {
        if (!file.toFile().setWritable(false, false)) {
            LOGGER.warning("Could not mark " + file + " read-only.");
        }
    }

    /**
     * Clears the read-only flag so the file can be replaced or deleted (required on Windows).
     *
     * @param file the file; missing files are ignored
     */
    private static void makeWritable(Path file) {
        if (Files.exists(file) && !file.toFile().setWritable(true)) {
            LOGGER.warning("Could not clear the read-only flag of " + file + ".");
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
        if (ioException instanceof FileSystemException fileSystemException
                && fileSystemException.getReason() != null) {
            return fileSystemException.getReason();
        }
        String message = ioException.getMessage();
        return message != null ? message : ioException.getClass().getSimpleName();
    }
}
