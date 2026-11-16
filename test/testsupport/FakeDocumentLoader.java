package testsupport;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import models.CancellationToken;
import models.DocumentException;
import models.DocumentStorageException;
import models.InvalidDocumentException;
import models.OperationCancelledException;
import models.ProgressListener;
import models.storage.DocumentLoader;

/**
 * Scriptable {@link DocumentLoader}: returns registered contents for registered paths, throws
 * registered failures, and can block until a latch opens (to hold an import in READING while the
 * test does something else). Used by Member C.
 *
 * <p>Behavior of {@link #load}: records the path; if a blocker latch is set, waits for it (at most
 * {@value #BLOCK_TIMEOUT_SECONDS} s); checks the token; throws the registered failure if any;
 * reports progress {@code (length / 2, length)} and {@code (length, length)}, checking the token
 * in between; returns the content. Unregistered paths fail with
 * {@link InvalidDocumentException.Reason#NOT_A_REGULAR_FILE}.</p>
 *
 * <p>Owner: Eric (contract layer test support, frozen v1.0).</p>
 *
 * <p>Threading: Thread-safe.</p>
 */
public final class FakeDocumentLoader implements DocumentLoader {

    /** Maximum time {@link #load} waits for the blocker latch. */
    public static final long BLOCK_TIMEOUT_SECONDS = 10;

    private final Map<Path, String> contents = new ConcurrentHashMap<>();
    private final Map<Path, DocumentException> failures = new ConcurrentHashMap<>();
    private final Map<Path, RuntimeException> runtimeFailures = new ConcurrentHashMap<>();
    private final List<Path> loadedPaths = new CopyOnWriteArrayList<>();
    private volatile CountDownLatch blocker;

    /** Creates a loader with nothing registered. */
    public FakeDocumentLoader() {
        // empty
    }

    /**
     * Registers the content returned for a path.
     *
     * @param path    the path
     * @param content the content
     */
    public void register(Path path, String content) {
        contents.put(path, content);
    }

    /**
     * Registers a checked failure for a path.
     *
     * @param path    the path
     * @param failure an {@link InvalidDocumentException} or {@link DocumentStorageException}
     */
    public void registerFailure(Path path, DocumentException failure) {
        if (!(failure instanceof InvalidDocumentException) && !(failure instanceof DocumentStorageException)) {
            throw new IllegalArgumentException("unsupported failure type " + failure.getClass());
        }
        failures.put(path, failure);
    }

    /**
     * Registers an unexpected runtime failure (a "bug") for a path.
     *
     * @param path    the path
     * @param failure the exception
     */
    public void registerRuntimeFailure(Path path, RuntimeException failure) {
        runtimeFailures.put(path, failure);
    }

    /**
     * Makes every subsequent {@link #load} wait until {@code latch} reaches zero.
     *
     * @param latch the latch, or {@code null} to stop blocking
     */
    public void blockUntil(CountDownLatch latch) {
        blocker = latch;
    }

    /**
     * Returns the paths passed to {@link #load}, in call order.
     *
     * @return immutable copy
     */
    public List<Path> loadedPaths() {
        return List.copyOf(loadedPaths);
    }

    @Override
    public String load(Path source, ProgressListener progress, CancellationToken token)
            throws InvalidDocumentException, DocumentStorageException {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(progress, "progress");
        Objects.requireNonNull(token, "token");
        loadedPaths.add(source);
        CountDownLatch latch = blocker;
        if (latch != null) {
            awaitLatch(latch);
        }
        token.throwIfCancelled();
        RuntimeException runtimeFailure = runtimeFailures.get(source);
        if (runtimeFailure != null) {
            throw runtimeFailure;
        }
        DocumentException failure = failures.get(source);
        if (failure instanceof InvalidDocumentException) {
            throw (InvalidDocumentException) failure;
        }
        if (failure instanceof DocumentStorageException) {
            throw (DocumentStorageException) failure;
        }
        String content = contents.get(source);
        if (content == null) {
            throw new InvalidDocumentException(InvalidDocumentException.Reason.NOT_A_REGULAR_FILE,
                    "The file \"" + source + "\" does not exist.");
        }
        long length = content.length();
        progress.onProgress(length / 2, length);
        token.throwIfCancelled();
        progress.onProgress(length, length);
        return content;
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            if (!latch.await(BLOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("FakeDocumentLoader blocker was never released");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new OperationCancelledException("Interrupted while blocked");
        }
    }
}
