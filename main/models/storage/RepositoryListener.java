package models.storage;

import java.util.List;

import models.Document;

/**
 * Observer of {@link DocumentRepository} changes (Observer pattern).
 *
 * <p>Called on the thread that performed the mutation (a worker thread), <em>not</em> on the EDT,
 * while the repository still serializes mutations. Implementations must therefore be fast, must
 * not block, must not call mutating repository methods, and must hand any UI work to the
 * {@code controllers.UiDispatcher}. The controller is the only production implementation.</p>
 *
 * <p>Owner: Eric (interface, frozen v1.0). Implemented by Member C's controller; invoked by
 * Member A's repository.</p>
 *
 * <p>Threading: Called from worker threads; never concurrently for the same repository.</p>
 */
@FunctionalInterface
public interface RepositoryListener {

    /**
     * Reports that the set of documents changed.
     *
     * @param sortedSnapshot the complete, immutable, title-sorted list of documents after the change
     */
    void onDocumentsChanged(List<Document> sortedSnapshot);
}
