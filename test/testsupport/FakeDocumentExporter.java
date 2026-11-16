package testsupport;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import models.CancellationToken;
import models.Document;
import models.DocumentStorageException;
import models.storage.DocumentExporter;

/**
 * Recording {@link DocumentExporter} that never touches the disk, with failure injection. Used by
 * Member C.
 *
 * <p>Owner: Eric (contract layer test support, frozen v1.0).</p>
 *
 * <p>Threading: Thread-safe.</p>
 */
public final class FakeDocumentExporter implements DocumentExporter {

    /** One recorded export. */
    public record ExportCall(Document document, Path target) {
    }

    private final List<ExportCall> exports = new CopyOnWriteArrayList<>();
    private volatile DocumentStorageException failure;

    /** Creates an exporter that succeeds. */
    public FakeDocumentExporter() {
        // empty
    }

    /**
     * Makes every subsequent export fail.
     *
     * @param failure the exception, or {@code null} to succeed again
     */
    public void failWith(DocumentStorageException failure) {
        this.failure = failure;
    }

    /**
     * Returns the recorded exports (including failed ones).
     *
     * @return immutable copy in call order
     */
    public List<ExportCall> exports() {
        return List.copyOf(exports);
    }

    @Override
    public void export(Document document, Path target, CancellationToken token) throws DocumentStorageException {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(token, "token");
        exports.add(new ExportCall(document, target));
        token.throwIfCancelled();
        DocumentStorageException current = failure;
        if (current != null) {
            throw current;
        }
    }
}
