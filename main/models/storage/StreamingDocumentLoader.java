package models.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

import models.AppConstants;
import models.AsciiText;
import models.CancellationToken;
import models.DocumentStorageException;
import models.InvalidDocumentException;
import models.ProgressListener;

/**
 * {@link DocumentLoader} that streams a file in fixed-size buffers, validating ASCII and size on
 * the fly and reporting byte progress (UC1, D3, U2).
 *
 * <p><b>Owner:</b> Member A (Storage &amp; File I/O).</p>
 *
 * <p><b>Algorithm (required):</b></p>
 * <ol>
 *   <li>If {@code !Files.isRegularFile(source)} &rarr; {@link InvalidDocumentException} with reason
 *       {@code NOT_A_REGULAR_FILE} and message {@code "<path>" is not a readable file.}</li>
 *   <li>{@code total = Files.size(source)}; if {@code total > maxFileSizeBytes} &rarr; reason
 *       {@code FILE_TOO_LARGE}, message
 *       {@code The file is 25.3 MB; the maximum allowed size is 20 MB.} - <em>without reading the
 *       content</em>.</li>
 *   <li>Allocate {@code byte[] content = new byte[(int) total]}; read with an {@code InputStream}
 *       into {@code AppConstants.IO_BUFFER_SIZE_BYTES} chunks. After each chunk: check the token,
 *       scan the new bytes with {@code AsciiText.isAsciiByte} (first offending byte &rarr; reason
 *       {@code NON_ASCII_CONTENT}, message {@code The file contains a non-ASCII byte (0xE9) at
 *       position 18. Only plain ASCII text files can be imported.}), report
 *       {@code progress.onProgress(bytesRead, total)}.</li>
 *   <li>If the stream delivers more bytes than {@code total} (file grew) and the new size exceeds
 *       the limit &rarr; {@code FILE_TOO_LARGE}; if it grew within the limit, grow the buffer.
 *       If it delivers fewer bytes (file shrank), use what was read.</li>
 *   <li>Return {@code new String(content, 0, bytesRead, StandardCharsets.US_ASCII)}. Never use a
 *       {@code Reader} or {@code Files.readAllLines}: they would alter or drop line breaks.</li>
 * </ol>
 *
 * <p>{@code IOException}s are wrapped in {@link DocumentStorageException} with message
 * {@code Could not read "<path>": <cause>.} Cancellation surfaces as
 * {@code OperationCancelledException} (unchecked); the stream is closed in every case
 * (try-with-resources).</p>
 *
 * <p><b>Performance:</b> loading 5 MB must take well under 1 s (target: &lt; 100 ms; test A-39).</p>
 *
 * <p><b>Threading:</b> Immutable, thread-safe (no mutable fields); called concurrently by
 * several import threads.</p>
 */
public final class StreamingDocumentLoader implements DocumentLoader {

    /** Bytes per megabyte for size messages (1 MB = 1024 * 1024 bytes). */
    private static final double BYTES_PER_MEGABYTE = 1024.0 * 1024.0;

    /** Suffix stripped from formatted sizes so that 20.0 MB is shown as 20 MB. */
    private static final String ZERO_DECIMAL_SUFFIX = ".0";

    /** Mask that turns a signed byte into its unsigned value 0..255 for messages. */
    private static final int UNSIGNED_BYTE_MASK = 0xFF;

    /** Maximum accepted file size in bytes; injectable so tests can use a small limit (A-34). */
    private final long maxFileSizeBytes;

    /** Creates a loader with the production limit {@link AppConstants#MAX_FILE_SIZE_BYTES}. */
    public StreamingDocumentLoader() {
        this(AppConstants.MAX_FILE_SIZE_BYTES);
    }

    /**
     * Creates a loader with a custom size limit (tests).
     *
     * @param maxFileSizeBytes the limit, {@code >= 0}
     * @throws IllegalArgumentException if negative
     */
    public StreamingDocumentLoader(long maxFileSizeBytes) {
        if (maxFileSizeBytes < 0) {
            throw new IllegalArgumentException("maxFileSizeBytes must be >= 0");
        }
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    /**
     * {@inheritDoc}
     *
     * <p>See the algorithm in the class description. Tests: A-31..A-39.</p>
     */
    @Override
    public String load(Path source, ProgressListener progress, CancellationToken token)
            throws InvalidDocumentException, DocumentStorageException {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(progress, "progress");
        Objects.requireNonNull(token, "token");

        if (!Files.isRegularFile(source) || !Files.isReadable(source)) {
            throw new InvalidDocumentException(InvalidDocumentException.Reason.NOT_A_REGULAR_FILE,
                    "\"" + source + "\" is not a readable file.");
        }
        try {
            long total = Files.size(source);
            if (total > maxFileSizeBytes) {
                throw fileTooLarge(total);
            }
            return readValidated(source, total, progress, token);
        } catch (IOException ioException) {
            throw new DocumentStorageException("Could not read \"" + source + "\": "
                    + describe(ioException) + ".", ioException);
        }
    }

    /**
     * Streams the file into a buffer of the expected size, validating and reporting each chunk.
     *
     * @param source   the file, already checked to be a regular file
     * @param total    the size reported by the file system before reading, {@code <= maxFileSizeBytes}
     * @param progress receives byte progress
     * @param token    checked once per chunk
     * @return the decoded content
     * @throws InvalidDocumentException if a non-ASCII byte is found or the file grows past the limit
     * @throws IOException              if reading fails
     */
    private String readValidated(Path source, long total, ProgressListener progress, CancellationToken token)
            throws InvalidDocumentException, IOException {
        byte[] content = new byte[Math.toIntExact(total)];
        int bytesRead = 0;
        long lastReportedCompleted = -1;
        long lastReportedTotal = -1;
        try (InputStream input = Files.newInputStream(source)) {
            while (true) {
                if (bytesRead == content.length) {
                    // Buffer full: the file is either complete or has grown since Files.size (R14).
                    int nextByte = input.read();
                    if (nextByte < 0) {
                        break;
                    }
                    content = grow(content, bytesRead, source);
                    content[bytesRead] = (byte) nextByte;
                    token.throwIfCancelled();
                    checkAscii(content, bytesRead, bytesRead + 1);
                    bytesRead++;
                } else {
                    int requested = Math.min(AppConstants.IO_BUFFER_SIZE_BYTES, content.length - bytesRead);
                    int chunkLength = input.read(content, bytesRead, requested);
                    if (chunkLength < 0) {
                        break;
                    }
                    token.throwIfCancelled();
                    checkAscii(content, bytesRead, bytesRead + chunkLength);
                    bytesRead += chunkLength;
                }
                lastReportedCompleted = bytesRead;
                lastReportedTotal = Math.max(total, bytesRead);
                progress.onProgress(lastReportedCompleted, lastReportedTotal);
            }
        }
        // Final report, so the bar ends at 100 % even for empty files or files that shrank.
        if (lastReportedCompleted != bytesRead || lastReportedTotal != bytesRead) {
            progress.onProgress(bytesRead, bytesRead);
        }
        return new String(content, 0, bytesRead, StandardCharsets.US_ASCII);
    }

    /**
     * Enlarges a full buffer because the file grew while being read, enforcing the size limit.
     *
     * @param content   the full buffer
     * @param bytesRead number of valid bytes, equal to {@code content.length}
     * @param source    the file, used to report its current size
     * @return a larger buffer holding the same bytes
     * @throws InvalidDocumentException if one more byte would exceed the limit
     * @throws IOException              if the current size cannot be determined
     */
    private byte[] grow(byte[] content, int bytesRead, Path source) throws InvalidDocumentException, IOException {
        long neededSize = (long) bytesRead + 1;
        if (neededSize > maxFileSizeBytes) {
            throw fileTooLarge(Math.max(neededSize, Files.size(source)));
        }
        long doubledSize = Math.max((long) content.length * 2, (long) AppConstants.IO_BUFFER_SIZE_BYTES);
        int newCapacity = Math.toIntExact(Math.min(maxFileSizeBytes, Math.max(neededSize, doubledSize)));
        return Arrays.copyOf(content, newCapacity);
    }

    /**
     * Throws at the first byte in {@code content[from, to)} that is not ASCII.
     *
     * @param content the buffer
     * @param from    first index to check, inclusive
     * @param to      last index to check, exclusive
     * @throws InvalidDocumentException with reason {@code NON_ASCII_CONTENT} naming the byte and its offset
     */
    private static void checkAscii(byte[] content, int from, int to) throws InvalidDocumentException {
        for (int i = from; i < to; i++) {
            if (!AsciiText.isAsciiByte(content[i])) {
                String byteText = String.format(Locale.ROOT, "0x%02X", content[i] & UNSIGNED_BYTE_MASK);
                throw new InvalidDocumentException(InvalidDocumentException.Reason.NON_ASCII_CONTENT,
                        "The file contains a non-ASCII byte (" + byteText + ") at position " + i
                                + ". Only plain ASCII text files can be imported.");
            }
        }
    }

    /**
     * Creates the {@code FILE_TOO_LARGE} exception. If size and limit round to the same text
     * (e.g. both {@code 20 MB}), exact byte counts are shown instead so the message makes sense.
     *
     * @param size the file size in bytes, {@code > maxFileSizeBytes}
     * @return the exception to throw
     */
    private InvalidDocumentException fileTooLarge(long size) {
        String sizeText = formatMegabytes(size);
        String limitText = formatMegabytes(maxFileSizeBytes);
        if (sizeText.equals(limitText)) {
            sizeText = size + " bytes";
            limitText = maxFileSizeBytes + " bytes";
        }
        return new InvalidDocumentException(InvalidDocumentException.Reason.FILE_TOO_LARGE,
                "The file is " + sizeText + "; the maximum allowed size is " + limitText + ".");
    }

    /**
     * Returns a short description of an I/O failure for messages.
     *
     * @param ioException the failure
     * @return its message, or its class name if it has none
     */
    private static String describe(IOException ioException) {
        String message = ioException.getMessage();
        return message != null ? message : ioException.getClass().getSimpleName();
    }

    /**
     * Formats a byte count for messages, e.g. {@code 25.3 MB} (1 MB = 1024 * 1024 bytes).
     *
     * @param bytes the byte count
     * @return the formatted size
     */
    private static String formatMegabytes(long bytes) {
        String number = String.format(Locale.ROOT, "%.1f", bytes / BYTES_PER_MEGABYTE);
        if (number.endsWith(ZERO_DECIMAL_SUFFIX)) {
            number = number.substring(0, number.length() - ZERO_DECIMAL_SUFFIX.length());
        }
        return number + " MB";
    }
}
