package testsupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import models.CancellationToken;
import models.Document;
import models.DuplicateTitleException;
import models.LoadSummary;
import models.ProgressListener;

/** Tests the repository fake, so that Member C can trust it. Owner: Eric. */
class FakeDocumentRepositoryTest {

    /** E-51: fake follows the add/duplicate/replace and notification contract. */
    @Test
    @DisplayName("E-51 fake repository enforces duplicates, replaces on request and notifies once per change")
    void add_duplicateAndReplace_followsContract() throws Exception {
        FakeDocumentRepository repository = new FakeDocumentRepository();
        List<List<Document>> snapshots = new ArrayList<>();
        repository.addRepositoryListener(snapshots::add);
        Document first = TestDocuments.document("A", "one");
        Document second = TestDocuments.document("A", "two");

        repository.add(first, false);
        assertThrows(DuplicateTitleException.class, () -> repository.add(second, false));
        assertEquals(first, repository.add(second, true).orElseThrow());

        assertEquals(second, repository.findByTitle("A").orElseThrow());
        assertEquals(2, snapshots.size());
        assertEquals(3, repository.addCalls().size());
    }

    /** E-52: in-memory documents win over persisted ones during loadPersisted. */
    @Test
    @DisplayName("E-52 fake loadPersisted skips titles already in memory and reports progress")
    void loadPersisted_titleAlreadyInMemory_isSkipped() throws Exception {
        FakeDocumentRepository repository = new FakeDocumentRepository();
        repository.putDirectly(TestDocuments.document("B", "user import"));
        repository.setPersistedDocuments(List.of(TestDocuments.document("A"), TestDocuments.document("B", "old")));
        List<long[]> progress = new ArrayList<>();
        ProgressListener listener = (completed, total) -> progress.add(new long[] {completed, total});

        LoadSummary summary = repository.loadPersisted(listener, CancellationToken.NONE);

        assertEquals(1, summary.loadedCount());
        assertEquals(1, summary.skippedCount());
        assertEquals("user import", repository.findByTitle("B").orElseThrow().content());
        assertEquals(2, progress.size());
        assertTrue(progress.get(1)[0] == 2 && progress.get(1)[1] == 2);
    }
}
