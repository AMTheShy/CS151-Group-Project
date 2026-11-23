package models.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import models.AppConstants;
import models.CancellationToken;
import models.Document;
import models.DocumentException;
import models.DocumentStorageException;
import models.DuplicateTitleException;
import models.LoadSummary;
import models.OperationCancelledException;
import models.ProgressListener;
import models.SimpleCancellationToken;
import testsupport.FakeDocumentStore;
import testsupport.TestDocuments;

/**
 * Tests for {@code DefaultDocumentRepository}. Owner: Member A.
 *
 * <p>Cases A-01..A-19 of {@code design/test-plan.md}. The repository is tested against
 * {@link FakeDocumentStore}, whose operation log proves the write-ahead order.</p>
 */
class DefaultDocumentRepositoryTest {

    /** Timeout for every wait in the concurrency tests (CONVENTIONS Q7). */
    private static final long TIMEOUT_SECONDS = 5;

    /** Rounds of the racy scenario in A-15 (CONVENTIONS Q7: at least 50). */
    private static final int RACE_ROUNDS = 50;

    /** Writer threads in A-16 and A-18. */
    private static final int WRITER_THREADS = 8;

    /** Distinct titles per writer thread in A-16. */
    private static final int TITLES_PER_THREAD = 25;

    /** Writer iterations in A-17. */
    private static final int WRITE_ITERATIONS = 1000;

    /** Reader threads in A-17. */
    private static final int READER_THREADS = 2;

    /** Writer threads in A-18. */
    private static final int ORDERING_THREADS = 4;

    private FakeDocumentStore store;

    private DefaultDocumentRepository repository;

    /** Snapshots received by the listener of the tests that register {@link #recorder}. */
    private List<List<Document>> notifications;

    /** Listener that records every snapshot into {@link #notifications}. */
    private RepositoryListener recorder;

    /** Creates fresh fixtures for every test (no shared state between tests). */
    @BeforeEach
    void setUp() {
        store = new FakeDocumentStore();
        repository = new DefaultDocumentRepository(store);
        notifications = new CopyOnWriteArrayList<>();
        recorder = notifications::add;
    }

    /**
     * A-01: add a new document [unit].
     *
     * <p>Arrange: empty repository; add(doc 'Notes', false).</p>
     * <p>Act: add / findByTitle.</p>
     * <p>Assert: findByTitle('Notes') returns doc; store log = [save:Notes]; returns Optional.empty().</p>
     */
    @Test
    @DisplayName("A-01 add a new document")
    void add_newDocument_isStoredAndFindable() throws DocumentException {
        // Arrange
        Document document = TestDocuments.document("Notes");

        // Act
        Optional<Document> previous = repository.add(document, false);

        // Assert
        assertEquals(Optional.empty(), previous);
        assertEquals(Optional.of(document), repository.findByTitle("Notes"));
        assertEquals(List.of("save:Notes"), store.operations());
    }

    /**
     * A-02: duplicate title without replace (D2) [unit].
     *
     * <p>Arrange: 'Notes' present; add(other 'Notes', false).</p>
     * <p>Act: add.</p>
     * <p>Assert: DuplicateTitleException; original unchanged; no second save in store log.</p>
     */
    @Test
    @DisplayName("A-02 duplicate title without replace (D2)")
    void add_duplicateWithoutReplace_throwsDuplicateTitleException() throws DocumentException {
        // Arrange
        Document original = TestDocuments.document("Notes", "original");
        repository.add(original, false);
        Document other = TestDocuments.document("Notes", "other");

        // Act
        DuplicateTitleException thrown = assertThrows(DuplicateTitleException.class,
                () -> repository.add(other, false));

        // Assert
        assertEquals("Notes", thrown.title());
        assertEquals("original", repository.findByTitle("Notes").orElseThrow().content());
        assertEquals(List.of("save:Notes"), store.operations());
    }

    /**
     * A-03: duplicate title with replace (D2) [unit].
     *
     * <p>Arrange: 'Notes' v1 present; add('Notes' v2, true).</p>
     * <p>Act: add.</p>
     * <p>Assert: returns Optional(v1); findByTitle returns v2; store has v2.</p>
     */
    @Test
    @DisplayName("A-03 duplicate title with replace (D2)")
    void add_duplicateWithReplace_replacesAndReturnsPrevious() throws DocumentException {
        // Arrange
        Document first = TestDocuments.document("Notes", "v1");
        Document second = TestDocuments.document("Notes", "v2");
        repository.add(first, false);

        // Act
        Optional<Document> previous = repository.add(second, true);

        // Assert
        assertEquals("v1", previous.orElseThrow().content());
        assertEquals("v2", repository.findByTitle("Notes").orElseThrow().content());
        assertEquals("v2", store.stored("Notes").orElseThrow().content());
    }

    /**
     * A-04: titles are case-sensitive keys (D1) [unit].
     *
     * <p>Arrange: add 'Foo' then 'foo'.</p>
     * <p>Act: add / containsTitle.</p>
     * <p>Assert: size 2; containsTitle('FOO') false.</p>
     */
    @Test
    @DisplayName("A-04 titles are case-sensitive keys (D1)")
    void add_titlesDifferingOnlyInCase_coexist() throws DocumentException {
        // Act
        repository.add(TestDocuments.document("Foo"), false);
        repository.add(TestDocuments.document("foo"), false);

        // Assert
        assertEquals(2, repository.size());
        assertTrue(repository.containsTitle("Foo"));
        assertTrue(repository.containsTitle("foo"));
        assertFalse(repository.containsTitle("FOO"));
    }

    /**
     * A-05: sorted immutable snapshot (D6, S5) [unit].
     *
     * <p>Arrange: add 'b','A','a'; take snapshot; add 'c'.</p>
     * <p>Act: listAllSortedByTitle.</p>
     * <p>Assert: snapshot = [A, a, b]; snapshot unchanged after add; snapshot.add throws
     * UnsupportedOperationException.</p>
     */
    @Test
    @DisplayName("A-05 sorted immutable snapshot (D6, S5)")
    void listAllSortedByTitle_afterLaterAdd_snapshotUnchanged() throws DocumentException {
        // Arrange
        repository.add(TestDocuments.document("b"), false);
        repository.add(TestDocuments.document("A"), false);
        repository.add(TestDocuments.document("a"), false);

        // Act
        List<Document> snapshot = repository.listAllSortedByTitle();
        repository.add(TestDocuments.document("c"), false);

        // Assert
        assertEquals(List.of("A", "a", "b"), titles(snapshot));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(TestDocuments.document("d")));
        assertEquals(List.of("A", "a", "b", "c"), titles(repository.listAllSortedByTitle()));
    }

    /**
     * A-06: write-ahead: store failure [unit].
     *
     * <p>Arrange: store.failSave('Notes'); add('Notes').</p>
     * <p>Act: add.</p>
     * <p>Assert: DocumentStorageException; findByTitle empty; no listener call.</p>
     */
    @Test
    @DisplayName("A-06 write-ahead: store failure")
    void add_storeFailure_leavesRepositoryUnchanged() {
        // Arrange
        store.failSave("Notes", new DocumentStorageException("Could not save the document \"Notes\": disk full."));
        repository.addRepositoryListener(recorder);

        // Act
        assertThrows(DocumentStorageException.class,
                () -> repository.add(TestDocuments.document("Notes"), false));

        // Assert
        assertEquals(Optional.empty(), repository.findByTitle("Notes"));
        assertEquals(0, repository.size());
        assertEquals(List.of(), notifications);
    }

    /**
     * A-07: delete selected documents (UC3) [unit].
     *
     * <p>Arrange: 'A','B' present; removeAll([A, X]).</p>
     * <p>Act: removeAll.</p>
     * <p>Assert: returns [A]; store log has delete:A; B still present; exactly one notification.</p>
     */
    @Test
    @DisplayName("A-07 delete selected documents (UC3)")
    void removeAll_existingAndMissingTitles_removesExistingOnly() throws DocumentException {
        // Arrange
        repository.add(TestDocuments.document("A"), false);
        repository.add(TestDocuments.document("B"), false);
        repository.addRepositoryListener(recorder);

        // Act
        List<String> removed = repository.removeAll(List.of("A", "X"));

        // Assert
        assertEquals(List.of("A"), removed);
        assertTrue(store.operations().contains("delete:A"));
        assertFalse(store.operations().contains("delete:X"), "missing titles must not reach the store");
        assertTrue(repository.containsTitle("B"));
        assertFalse(repository.containsTitle("A"));
        assertEquals(1, notifications.size());
    }

    /**
     * A-08: partial delete failure [unit].
     *
     * <p>Arrange: 'A','B' present; store.failDelete('A'); removeAll([A, B]).</p>
     * <p>Act: removeAll.</p>
     * <p>Assert: DocumentStorageException naming A; B removed; A still present; listener notified once.</p>
     */
    @Test
    @DisplayName("A-08 partial delete failure")
    void removeAll_partialStoreFailure_removesOthersAndThrows() throws DocumentException {
        // Arrange
        repository.add(TestDocuments.document("A"), false);
        repository.add(TestDocuments.document("B"), false);
        store.failDelete("A", new DocumentStorageException("Could not delete the document \"A\": access denied."));
        repository.addRepositoryListener(recorder);

        // Act
        DocumentStorageException thrown = assertThrows(DocumentStorageException.class,
                () -> repository.removeAll(List.of("A", "B")));

        // Assert
        assertTrue(thrown.getMessage().startsWith("Could not delete \"A\":"),
                "message must name the failed title: " + thrown.getMessage());
        assertTrue(repository.containsTitle("A"));
        assertFalse(repository.containsTitle("B"));
        assertEquals(1, notifications.size());
        assertEquals(List.of("A"), titles(notifications.get(0)));
    }

    /**
     * A-09: Observer notifications [unit].
     *
     * <p>Arrange: register listener; add, replace, remove.</p>
     * <p>Act: listeners.</p>
     * <p>Assert: 3 notifications, each the full sorted snapshot; failed add sends none.</p>
     */
    @Test
    @DisplayName("A-09 Observer notifications")
    void mutations_successfulChanges_notifyWithSortedSnapshot() throws DocumentException {
        // Arrange
        repository.add(TestDocuments.document("b", "b v1"), false);
        repository.addRepositoryListener(recorder);

        // Act
        repository.add(TestDocuments.document("A"), false);
        repository.add(TestDocuments.document("b", "b v2"), true);
        repository.removeAll(List.of("A"));
        assertThrows(DuplicateTitleException.class, () -> repository.add(TestDocuments.document("b"), false));
        store.failSave("C", new DocumentStorageException("Could not save the document \"C\": disk full."));
        assertThrows(DocumentStorageException.class, () -> repository.add(TestDocuments.document("C"), false));

        // Assert
        assertEquals(3, notifications.size());
        assertEquals(List.of("A", "b"), titles(notifications.get(0)));
        assertEquals("b v1", notifications.get(0).get(1).content());
        assertEquals(List.of("A", "b"), titles(notifications.get(1)));
        assertEquals("b v2", notifications.get(1).get(1).content());
        assertEquals(List.of("b"), titles(notifications.get(2)));
    }

    /**
     * A-10: listener unregistration [unit].
     *
     * <p>Arrange: register, remove, then add.</p>
     * <p>Act: removeRepositoryListener.</p>
     * <p>Assert: listener not called; duplicate registration notifies only once.</p>
     */
    @Test
    @DisplayName("A-10 listener unregistration")
    void removeRepositoryListener_afterRemoval_noMoreNotifications() throws DocumentException {
        // Arrange
        List<List<Document>> removedListenerCalls = new ArrayList<>();
        RepositoryListener removedListener = removedListenerCalls::add;
        repository.addRepositoryListener(removedListener);
        repository.removeRepositoryListener(removedListener);
        repository.addRepositoryListener(recorder);
        repository.addRepositoryListener(recorder);

        // Act
        repository.add(TestDocuments.document("Notes"), false);

        // Assert
        assertEquals(List.of(), removedListenerCalls);
        assertEquals(1, notifications.size());
    }

    /**
     * A-11: startup load (D5) [unit].
     *
     * <p>Arrange: store pre-populated with 3 docs.</p>
     * <p>Act: loadPersisted.</p>
     * <p>Assert: LoadSummary(3,0,[]); progress (1,3),(2,3),(3,3); listAll has 3 docs with original importedAt.</p>
     */
    @Test
    @DisplayName("A-11 startup load (D5)")
    void loadPersisted_storedDocuments_loadsAllAndReportsProgress() throws DocumentStorageException {
        // Arrange
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        store.putDirectly(new Document("A", "a", base));
        store.putDirectly(new Document("B", "b", base.plusSeconds(1)));
        store.putDirectly(new Document("C", "c", base.plusSeconds(2)));
        List<String> progressCalls = new ArrayList<>();
        ProgressListener progress = (completed, total) -> progressCalls.add(completed + "/" + total);

        // Act
        LoadSummary summary = repository.loadPersisted(progress, CancellationToken.NONE);

        // Assert
        assertEquals(new LoadSummary(3, 0, List.of()), summary);
        assertEquals(List.of("1/3", "2/3", "3/3"), progressCalls);
        List<Document> documents = repository.listAllSortedByTitle();
        assertEquals(List.of("A", "B", "C"), titles(documents));
        assertEquals(base, documents.get(0).importedAt());
        assertEquals(base.plusSeconds(2), documents.get(2).importedAt());
    }

    /**
     * A-12: user import wins over startup load (R6) [unit].
     *
     * <p>Arrange: 'A' added by user; store also has older 'A'.</p>
     * <p>Act: loadPersisted.</p>
     * <p>Assert: skippedCount 1; in-memory 'A' is the user's version.</p>
     */
    @Test
    @DisplayName("A-12 user import wins over startup load (R6)")
    void loadPersisted_titleAlreadyInMemory_skipsPersistedCopy() throws DocumentException {
        // Arrange
        store.putDirectly(new Document("A", "older persisted copy", Instant.parse("2026-01-01T00:00:00Z")));
        Document userVersion = TestDocuments.document("A", "user's new import");
        repository.add(userVersion, false);

        // Act
        LoadSummary summary = repository.loadPersisted(ProgressListener.NONE, CancellationToken.NONE);

        // Assert
        assertEquals(1, summary.skippedCount());
        assertEquals(0, summary.loadedCount());
        assertEquals("user's new import", repository.findByTitle("A").orElseThrow().content());
        assertFalse(store.operations().contains("load:A"), "a title already in memory must not be loaded");
    }

    /**
     * A-13: corrupt persisted entry [unit].
     *
     * <p>Arrange: store.failLoad('B', DocumentStorageException); store has A, B, C.</p>
     * <p>Act: loadPersisted.</p>
     * <p>Assert: loaded 2; problems has 1 entry mentioning 'B'.</p>
     */
    @Test
    @DisplayName("A-13 corrupt persisted entry")
    void loadPersisted_corruptEntry_recordsProblemAndContinues() throws DocumentStorageException {
        // Arrange
        store.putDirectly(TestDocuments.document("A"));
        store.putDirectly(TestDocuments.document("B"));
        store.putDirectly(TestDocuments.document("C"));
        store.failLoad("B", new DocumentStorageException(
                "Could not load the document \"B\": the stored copy is corrupt (expected 9 bytes but found 3)."));

        // Act
        LoadSummary summary = repository.loadPersisted(ProgressListener.NONE, CancellationToken.NONE);

        // Assert
        assertEquals(2, summary.loadedCount());
        assertEquals(1, summary.problems().size());
        assertTrue(summary.problems().get(0).contains("B"), "problem must name the entry: " + summary.problems());
        assertEquals(List.of("A", "C"), titles(repository.listAllSortedByTitle()));
    }

    /**
     * A-14: cancellation of startup load [unit].
     *
     * <p>Arrange: token cancelled from progress listener after first entry.</p>
     * <p>Act: loadPersisted.</p>
     * <p>Assert: OperationCancelledException; first document stays in repository.</p>
     */
    @Test
    @DisplayName("A-14 cancellation of startup load")
    void loadPersisted_cancelled_throwsAndKeepsLoadedDocuments() {
        // Arrange
        store.putDirectly(TestDocuments.document("A"));
        store.putDirectly(TestDocuments.document("B"));
        store.putDirectly(TestDocuments.document("C"));
        SimpleCancellationToken token = new SimpleCancellationToken();
        ProgressListener cancelAfterFirst = (completed, total) -> {
            if (completed == 1) {
                token.cancel();
            }
        };

        // Act
        assertThrows(OperationCancelledException.class, () -> repository.loadPersisted(cancelAfterFirst, token));

        // Assert
        assertEquals(List.of("A"), titles(repository.listAllSortedByTitle()));
    }

    /**
     * A-15: D2 race: two concurrent imports of the same title [concurrency].
     *
     * <p>Arrange: 2 threads, start latch, both add('Same', false); repeat 50 times.</p>
     * <p>Act: add.</p>
     * <p>Assert: each round: exactly 1 success and 1 DuplicateTitleException.</p>
     */
    @Test
    @Tag("concurrency")
    @DisplayName("A-15 D2 race: two concurrent imports of the same title")
    void add_concurrentSameTitleWithoutReplace_exactlyOneSucceeds() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < RACE_ROUNDS; round++) {
                // Arrange
                // new repository every round so the rounds do not affect each other
                FakeDocumentStore roundStore = new FakeDocumentStore();
                DefaultDocumentRepository roundRepository = new DefaultDocumentRepository(roundStore);
                CountDownLatch start = new CountDownLatch(1);
                List<Future<Boolean>> attempts = new ArrayList<>();
                for (int thread = 0; thread < 2; thread++) {
                    Document document = TestDocuments.document("Same", "from thread " + thread);
                    attempts.add(executor.submit(() -> {
                        // both threads wait here, then hit add() at the same moment
                        assertTrue(start.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
                        try {
                            roundRepository.add(document, false);
                            return true;
                        } catch (DuplicateTitleException duplicate) {
                            return false;
                        }
                    }));
                }

                // Act
                // release both threads at once
                start.countDown();
                int successes = 0;
                for (Future<Boolean> attempt : attempts) {
                    if (attempt.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                        successes++;
                    }
                }

                // Assert
                assertEquals(1, successes, "round " + round + ": exactly one add must succeed");
                // the loser must not have written anything to the store either
                assertEquals(List.of("save:Same"), roundStore.operations(), "round " + round);
            }
        } finally {
            shutDown(executor);
        }
    }

    /**
     * A-16: concurrent adds of distinct titles [concurrency].
     *
     * <p>Arrange: 8 threads x 25 distinct titles, start latch, await with timeout.</p>
     * <p>Act: add / size.</p>
     * <p>Assert: size 200; every title findable.</p>
     */
    @Test
    @Tag("concurrency")
    @DisplayName("A-16 concurrent adds of distinct titles")
    void add_concurrentDistinctTitles_allPresent() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(WRITER_THREADS);
        try {
            // Arrange
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> writers = new ArrayList<>();
            for (int thread = 0; thread < WRITER_THREADS; thread++) {
                int threadIndex = thread;
                writers.add(executor.submit(() -> {
                    assertTrue(start.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
                    for (int i = 0; i < TITLES_PER_THREAD; i++) {
                        repository.add(TestDocuments.document(distinctTitle(threadIndex, i)), false);
                    }
                    return null;
                }));
            }

            // Act
            start.countDown();
            awaitAll(writers);

            // Assert
            assertEquals(WRITER_THREADS * TITLES_PER_THREAD, repository.size());
            for (int thread = 0; thread < WRITER_THREADS; thread++) {
                for (int i = 0; i < TITLES_PER_THREAD; i++) {
                    assertTrue(repository.containsTitle(distinctTitle(thread, i)), distinctTitle(thread, i));
                }
            }
        } finally {
            shutDown(executor);
        }
    }

    /**
     * A-17: lock-free reads during writes [concurrency].
     *
     * <p>Arrange: 1 writer adds/removes 1000 times; 2 readers call listAll continuously.</p>
     * <p>Act: listAllSortedByTitle.</p>
     * <p>Assert: no exception; every snapshot sorted by TITLE_ORDER.</p>
     */
    @Test
    @Tag("concurrency")
    @DisplayName("A-17 lock-free reads during writes")
    void listAllSortedByTitle_duringConcurrentWrites_alwaysSortedAndNeverThrows() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(1 + READER_THREADS);
        try {
            // Arrange
            CountDownLatch start = new CountDownLatch(1);
            AtomicBoolean writerDone = new AtomicBoolean(false);
            List<Future<Integer>> readers = new ArrayList<>();
            for (int reader = 0; reader < READER_THREADS; reader++) {
                readers.add(executor.submit(() -> {
                    assertTrue(start.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
                    int snapshotsChecked = 0;
                    // do-while so every reader checks at least one snapshot
                    do {
                        assertSorted(repository.listAllSortedByTitle());
                        snapshotsChecked++;
                    } while (!writerDone.get());
                    return snapshotsChecked;
                }));
            }
            Future<?> writer = executor.submit(() -> {
                try {
                    assertTrue(start.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
                    for (int i = 0; i < WRITE_ITERATIONS; i++) {
                        // Alternate the case of the first letter to exercise both levels of TITLE_ORDER.
                        String title = (i % 2 == 0 ? "doc " : "Doc ") + (i % 100);
                        repository.add(TestDocuments.document(title), true);
                        if (i % 3 == 0) {
                            repository.removeAll(List.of(title));
                        }
                    }
                    return null;
                // in finally so the readers stop even if the writer crashes
                } finally {
                    writerDone.set(true);
                }
            });

            // Act
            start.countDown();
            writer.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            // Assert
            for (Future<Integer> reader : readers) {
                assertTrue(reader.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) > 0, "each reader must check snapshots");
            }
            assertSorted(repository.listAllSortedByTitle());
        } finally {
            shutDown(executor);
        }
    }

    /**
     * A-18: notification ordering (R9) [concurrency].
     *
     * <p>Arrange: 4 threads add distinct titles; listener records snapshot sizes.</p>
     * <p>Act: listeners.</p>
     * <p>Assert: sizes strictly increasing 1..N; final snapshot has N docs.</p>
     */
    @Test
    @Tag("concurrency")
    @DisplayName("A-18 notification ordering (R9)")
    void notifications_concurrentWriters_deliveredInMutationOrder() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(ORDERING_THREADS);
        try {
            // Arrange
            List<Integer> sizes = new CopyOnWriteArrayList<>();
            repository.addRepositoryListener(snapshot -> sizes.add(snapshot.size()));
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> writers = new ArrayList<>();
            for (int thread = 0; thread < ORDERING_THREADS; thread++) {
                int threadIndex = thread;
                writers.add(executor.submit(() -> {
                    assertTrue(start.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
                    for (int i = 0; i < TITLES_PER_THREAD; i++) {
                        repository.add(TestDocuments.document(distinctTitle(threadIndex, i)), false);
                    }
                    return null;
                }));
            }

            // Act
            start.countDown();
            awaitAll(writers);

            // Assert
            int expectedCount = ORDERING_THREADS * TITLES_PER_THREAD;
            List<Integer> expectedSizes = new ArrayList<>();
            for (int size = 1; size <= expectedCount; size++) {
                expectedSizes.add(size);
            }
            // if notifications were sent after unlocking, the sizes could arrive out of order
            assertEquals(expectedSizes, sizes);
            assertEquals(expectedCount, repository.listAllSortedByTitle().size());
        } finally {
            shutDown(executor);
        }
    }

    /**
     * A-19: null checks [unit].
     *
     * <p>Arrange: null document/title/titles/listener/progress/token.</p>
     * <p>Act: all.</p>
     * <p>Assert: NullPointerException for each.</p>
     */
    @Test
    @DisplayName("A-19 null checks")
    void publicMethods_nullArguments_throwNullPointerException() {
        assertThrows(NullPointerException.class, () -> new DefaultDocumentRepository(null));
        assertThrows(NullPointerException.class, () -> repository.add(null, false));
        assertThrows(NullPointerException.class, () -> repository.findByTitle(null));
        assertThrows(NullPointerException.class, () -> repository.containsTitle(null));
        assertThrows(NullPointerException.class, () -> repository.removeAll(null));
        assertThrows(NullPointerException.class, () -> repository.removeAll(Arrays.asList("A", null)));
        assertThrows(NullPointerException.class, () -> repository.addRepositoryListener(null));
        assertThrows(NullPointerException.class, () -> repository.removeRepositoryListener(null));
        assertThrows(NullPointerException.class, () -> repository.loadPersisted(null, CancellationToken.NONE));
        assertThrows(NullPointerException.class, () -> repository.loadPersisted(ProgressListener.NONE, null));
    }

    /**
     * Returns the titles of documents, in order.
     *
     * @param documents the documents
     * @return their titles
     */
    private static List<String> titles(List<Document> documents) {
        return documents.stream().map(Document::title).toList();
    }

    /**
     * Returns the title written by one writer thread in A-16 and A-18.
     *
     * @param thread the thread index
     * @param index  the title index within the thread
     * @return a title unique across all threads
     */
    private static String distinctTitle(int thread, int index) {
        return "Thread " + thread + " title " + index;
    }

    /**
     * Asserts that consecutive titles are strictly ordered by {@code TITLE_ORDER}.
     *
     * @param snapshot the snapshot to check
     */
    private static void assertSorted(List<Document> snapshot) {
        for (int i = 1; i < snapshot.size(); i++) {
            String previous = snapshot.get(i - 1).title();
            String current = snapshot.get(i).title();
            assertTrue(AppConstants.TITLE_ORDER.compare(previous, current) < 0,
                    "snapshot not sorted at " + i + ": " + previous + " / " + current);
        }
    }

    /**
     * Waits for every task, rethrowing the first failure.
     *
     * @param tasks the tasks
     * @throws ExecutionException   if a task failed
     * @throws InterruptedException if interrupted while waiting
     * @throws TimeoutException     if a task did not finish within the timeout
     */
    private static void awaitAll(List<? extends Future<?>> tasks)
            throws ExecutionException, InterruptedException, TimeoutException {
        for (Future<?> task : tasks) {
            task.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }

    /**
     * Shuts an executor down and waits for its threads to finish (CONVENTIONS Q7).
     *
     * @param executor the executor
     * @throws InterruptedException if interrupted while waiting
     */
    private static void shutDown(ExecutorService executor) throws InterruptedException {
        // always stop the threads, even when a test fails
        executor.shutdownNow();
        assertTrue(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS), "worker threads did not stop");
    }
}
