package models;

import java.nio.file.Path;
import java.util.Comparator;

/**
 * Application-wide constants and the single, shared title ordering.
 *
 * <p>Every "magic number" used by more than one class lives here so that all modules agree on
 * the same limits. Module code must reference these constants instead of re-declaring literals
 * (see {@code design/CONVENTIONS.md}, rule "Magic numbers").</p>
 *
 * <p>Owner: Eric (contract layer, frozen v1.0).</p>
 *
 * <p>Threading: Immutable, thread-safe (only static final fields and pure static methods).</p>
 */
public final class AppConstants {

    /** Maximum number of characters shown in the Preview column of the library table (D7, UC3). */
    public static final int PREVIEW_LENGTH = 500;

    /** Number of characters of context taken before and after the first match in a snippet (S4). */
    public static final int SNIPPET_CONTEXT = 40;

    /** Largest accepted document file size in bytes: 20 MiB (D3). Files larger than this are rejected. */
    public static final long MAX_FILE_SIZE_BYTES = 20L * 1024L * 1024L;

    /** Maximum title length in characters after trimming (D1). */
    public static final int MAX_TITLE_LENGTH = 200;

    /** Marker appended/prepended to text that has been truncated (S4, U3). */
    public static final String ELLIPSIS = "...";

    /** Maximum number of query characters shown in a search tab title before truncation (U3). */
    public static final int TAB_TITLE_MAX_LENGTH = 30;

    /** Name of the application data directory created under {@code user.home} (D4). */
    public static final String DATA_DIRECTORY_NAME = ".local-search-engine";

    /** Name of the sub-directory of the data directory holding the read-only document copies (D4). */
    public static final String DOCUMENTS_DIRECTORY_NAME = "documents";

    /** File extension used for the default file name offered by the Download dialog (D8). */
    public static final String EXPORT_FILE_EXTENSION = ".txt";

    /** Number of worker threads that execute imports concurrently (4.4: bounded import pool). */
    public static final int IMPORT_THREAD_COUNT = 2;

    /** Number of worker threads for other background work: startup load, delete, export. */
    public static final int BACKGROUND_THREAD_COUNT = 2;

    /** Buffer size used when streaming a file from or to disk. */
    public static final int IO_BUFFER_SIZE_BYTES = 64 * 1024;

    /**
     * Long loops over document text must call {@link CancellationToken#throwIfCancelled()} at
     * least once every this many characters (or bytes). 64 Ki characters take well under 1 ms to
     * scan, so cancellation latency stays far below the 100 ms UI budget.
     */
    public static final int CANCELLATION_CHECK_INTERVAL = 64 * 1024;

    /** Number of highlights applied per EDT batch in the reader (U6). */
    public static final int HIGHLIGHT_BATCH_SIZE = 500;

    /**
     * If a document has more matches than this, the reader highlights only the matches inside a
     * window around the visible region instead of all matches at once (decision N8 in the design
     * doc). Swing's {@code DefaultHighlighter} cost grows linearly with the number of highlights.
     */
    public static final int MAX_EAGER_HIGHLIGHTS = 10_000;

    /** Number of characters inserted into the reader's text model per EDT batch (U6). */
    public static final int TEXT_RENDER_CHUNK_CHARS = 256 * 1024;

    /** How long a COMPLETED import row stays visible in the progress area before removal (U2). */
    public static final int COMPLETED_IMPORT_ROW_LINGER_MILLIS = 2_000;

    /** Maximum time the application waits for worker threads to stop when exiting. */
    public static final long SHUTDOWN_GRACE_MILLIS = 2_000L;

    /**
     * The one and only ordering of document titles (D6): case-insensitive lexicographic order,
     * with case-sensitive natural order as tie breaker.
     *
     * <p>The tie breaker makes the comparator a <em>total</em> order that is consistent with
     * {@link String#equals(Object)}: {@code compare(a, b) == 0} if and only if
     * {@code a.equals(b)}. It is therefore safe to use as the comparator of a
     * {@code ConcurrentSkipListMap} keyed by title, where "Foo" and "foo" are distinct keys (D1).</p>
     *
     * <p>Nobody may write their own title comparator; always use this constant.</p>
     */
    public static final Comparator<String> TITLE_ORDER =
            String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.<String>naturalOrder());

    /** Orders documents by {@link Document#title()} using {@link #TITLE_ORDER}. */
    public static final Comparator<Document> DOCUMENT_TITLE_ORDER =
            Comparator.comparing(Document::title, TITLE_ORDER);

    private AppConstants() {
        throw new AssertionError("AppConstants is a constants holder and must not be instantiated");
    }

    /**
     * Returns the number of threads of the search pool: the number of available processors,
     * but at least 1 (4.4).
     *
     * @return the search pool size, always {@code >= 1}
     */
    public static int searchThreadCount() {
        return Math.max(1, Runtime.getRuntime().availableProcessors());
    }

    /**
     * Returns the production directory holding the read-only document copies:
     * {@code ${user.home}/.local-search-engine/documents} (D4).
     *
     * <p>Only the composition root ({@code app.LocalSearchEngineApp}) may call this method.
     * Every other class receives its directory through its constructor so that tests can use a
     * temporary directory instead.</p>
     *
     * @return the absolute default documents directory; the directory is not created
     */
    public static Path defaultDocumentsDirectory() {
        return Path.of(System.getProperty("user.home"), DATA_DIRECTORY_NAME, DOCUMENTS_DIRECTORY_NAME)
                .toAbsolutePath();
    }
}
