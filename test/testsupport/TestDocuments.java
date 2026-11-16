package testsupport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import models.Document;

/**
 * Factory for test documents, test texts, temporary files and committed test resources.
 *
 * <p>All modules use this class instead of hand-rolling test data so that the same S1 examples and
 * the same text generator are used everywhere.</p>
 *
 * <p><b>Resources:</b> files under {@code /test/resources} are located with
 * {@link #resource(String)}. Tests must be run with the project root as working directory, or the
 * system property {@value #RESOURCES_DIR_PROPERTY} must point to the resources directory (see
 * {@code design/developer-guide.md}, "Running tests").</p>
 *
 * <p>Owner: Eric (contract layer test support, frozen v1.0).</p>
 *
 * <p>Threading: Immutable, thread-safe (static methods; {@link #asciiText} creates its own
 * {@link Random}).</p>
 */
public final class TestDocuments {

    /** System property that overrides the location of {@code /test/resources}. */
    public static final String RESOURCES_DIR_PROPERTY = "test.resources.dir";

    /** Deterministic timestamp for documents created in tests. */
    public static final Instant FIXED_INSTANT = Instant.parse("2026-11-09T12:00:00Z");

    /** The query used by every S1 example in the requirements. */
    public static final String S1_QUERY = "Computer Algorithm";

    /** Texts that must match {@link #S1_QUERY} (S1, original UC2 examples). */
    public static final List<String> S1_POSITIVE_TEXTS = List.of(
            "computer algorithm",
            "CoMpUtEr aLgOriThm",
            "The computer algorithm",
            "Computer Algorithms",
            "Randomized Computer Algorithms");

    /** Texts that must not match {@link #S1_QUERY} (S1, original UC2 examples plus whitespace cases). */
    public static final List<String> S1_NEGATIVE_TEXTS = List.of(
            "Computing algorithms",
            "computer randomized algorithms",
            "Computer_Algorithm",
            "Computer  Algorithm",
            "Computer\tAlgorithm",
            "Computer\nAlgorithm",
            "Computer\r\nAlgorithm");

    /** Resource: a small multi-line ASCII sample with LF line breaks. */
    public static final String RESOURCE_SAMPLE_SMALL = "sample-small.txt";

    /** Number of matches of {@link #S1_QUERY} in {@link #RESOURCE_SAMPLE_SMALL}. */
    public static final int SAMPLE_SMALL_EXPECTED_MATCHES = 2;

    /** Resource: one line per S1 positive and negative example. */
    public static final String RESOURCE_S1_CASES = "s1-match-cases.txt";

    /** Number of matches of {@link #S1_QUERY} in {@link #RESOURCE_S1_CASES} (one per positive line). */
    public static final int S1_CASES_EXPECTED_MATCHES = 5;

    /** Resource: ASCII text containing one byte 0xE9 at offset {@value #NON_ASCII_OFFSET}. */
    public static final String RESOURCE_NON_ASCII = "non-ascii.txt";

    /** Byte offset of the first non-ASCII byte in {@link #RESOURCE_NON_ASCII}. */
    public static final int NON_ASCII_OFFSET = 18;

    /** Resource: an empty file. */
    public static final String RESOURCE_EMPTY = "empty.txt";

    /** Resource: text with CRLF, LF and lone CR line breaks. */
    public static final String RESOURCE_MIXED_LINE_BREAKS = "mixed-line-breaks.txt";

    /** Resource: a text designed for snippet boundary tests (see file header line). */
    public static final String RESOURCE_SNIPPET_BOUNDARIES = "snippet-boundaries.txt";

    /** Resource: overlapping-pattern text "aaaa" etc. */
    public static final String RESOURCE_OVERLAP = "overlap-cases.txt";

    private static final String[] WORDS = {
        "computer", "algorithm", "search", "engine", "document", "the", "a", "of", "and",
        "data", "structure", "random", "local", "query", "match", "swing", "thread", "java",
    };

    private TestDocuments() {
        throw new AssertionError("TestDocuments is a utility class and must not be instantiated");
    }

    /**
     * Creates a document with {@link #FIXED_INSTANT}.
     *
     * @param title   a valid title
     * @param content the content
     * @return the document
     */
    public static Document document(String title, String content) {
        return new Document(title, content, FIXED_INSTANT);
    }

    /**
     * Creates a document whose content is {@code "Content of <title>."}.
     *
     * @param title a valid title
     * @return the document
     */
    public static Document document(String title) {
        return document(title, "Content of " + title + ".");
    }

    /**
     * Generates deterministic pseudo-English ASCII text of exactly {@code length} characters:
     * lower-case words separated by single spaces, with an LF roughly every 80 characters.
     *
     * @param length desired length, {@code >= 0}
     * @param seed   random seed; the same seed always gives the same text
     * @return the text
     */
    public static String asciiText(int length, long seed) {
        if (length < 0) {
            throw new IllegalArgumentException("length must be >= 0");
        }
        Random random = new Random(seed);
        StringBuilder builder = new StringBuilder(length + 16);
        int lineLength = 0;
        while (builder.length() < length) {
            String word = WORDS[random.nextInt(WORDS.length)];
            builder.append(word);
            lineLength += word.length();
            if (lineLength > 80) {
                builder.append('\n');
                lineLength = 0;
            } else {
                builder.append(' ');
                lineLength++;
            }
        }
        builder.setLength(length);
        return builder.toString();
    }

    /**
     * Repeats {@code unit} until the result has exactly {@code length} characters.
     *
     * @param unit   a non-empty string
     * @param length desired length, {@code >= 0}
     * @return the repeated text, possibly ending with a partial unit
     */
    public static String repeatedText(String unit, int length) {
        Objects.requireNonNull(unit, "unit");
        if (unit.isEmpty() || length < 0) {
            throw new IllegalArgumentException("unit must be non-empty and length >= 0");
        }
        StringBuilder builder = new StringBuilder(length + unit.length());
        while (builder.length() < length) {
            builder.append(unit);
        }
        builder.setLength(length);
        return builder.toString();
    }

    /**
     * Writes ASCII text to a new file inside {@code directory} (typically a {@code @TempDir}).
     *
     * @param directory existing directory
     * @param fileName  file name
     * @param content   ASCII content
     * @return the path of the written file
     * @throws IOException if writing fails
     */
    public static Path writeAsciiFile(Path directory, String fileName, String content) throws IOException {
        return writeBytes(directory, fileName, content.getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * Writes raw bytes to a new file inside {@code directory}.
     *
     * @param directory existing directory
     * @param fileName  file name
     * @param bytes     content
     * @return the path of the written file
     * @throws IOException if writing fails
     */
    public static Path writeBytes(Path directory, String fileName, byte[] bytes) throws IOException {
        return Files.write(directory.resolve(fileName), bytes);
    }

    /**
     * Returns the directory {@code /test/resources}.
     *
     * @return the resources directory, from {@value #RESOURCES_DIR_PROPERTY} or {@code test/resources}
     *         relative to the working directory
     */
    public static Path resourcesDirectory() {
        String override = System.getProperty(RESOURCES_DIR_PROPERTY);
        return override != null ? Path.of(override) : Path.of("test", "resources");
    }

    /**
     * Returns a committed test resource.
     *
     * @param name a file name such as {@link #RESOURCE_SAMPLE_SMALL}
     * @return the path
     * @throws IllegalStateException if the file does not exist (wrong working directory)
     */
    public static Path resource(String name) {
        Path path = resourcesDirectory().resolve(name);
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("Test resource not found: " + path.toAbsolutePath()
                    + " (run tests from the project root or set -D" + RESOURCES_DIR_PROPERTY + ")");
        }
        return path;
    }

    /**
     * Reads a committed test resource as ASCII text.
     *
     * @param name a file name such as {@link #RESOURCE_SAMPLE_SMALL}
     * @return its content
     * @throws IOException if reading fails
     */
    public static String readResource(String name) throws IOException {
        return new String(Files.readAllBytes(resource(name)), StandardCharsets.US_ASCII);
    }
}
