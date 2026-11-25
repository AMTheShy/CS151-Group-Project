package controllers;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import controllers.concurrency.ManualTaskScheduler;
import models.DocumentStorageException;
import models.DocumentSummary;
import models.ImportProgress;
import models.ImportState;
import models.InvalidDocumentException;
import testsupport.FakeDocumentExporter;
import testsupport.FakeDocumentLoader;
import testsupport.FakeDocumentRepository;
import testsupport.FakeSnippetExtractor;
import testsupport.QueueingUiDispatcher;
import testsupport.RecordingLibraryListener;
import testsupport.TestDocuments;

class DefaultDocumentLibraryControllerTest {

    private static final Path SOURCE = Path.of("source.txt");

    private FakeDocumentRepository repository;
    private FakeDocumentLoader loader;
    private FakeDocumentExporter exporter;
    private ManualTaskScheduler scheduler;
    private QueueingUiDispatcher uiDispatcher;
    private RecordingLibraryListener listener;
    private DefaultDocumentLibraryController controller;

    @BeforeEach
    void setUp() {
        repository = new FakeDocumentRepository();
        loader = new FakeDocumentLoader();
        exporter = new FakeDocumentExporter();
        scheduler = new ManualTaskScheduler();
        uiDispatcher = new QueueingUiDispatcher();
        listener = new RecordingLibraryListener();

        controller = new DefaultDocumentLibraryController(
                repository, loader, exporter, new FakeSnippetExtractor(),
                scheduler, uiDispatcher);
        controller.addLibraryListener(listener);
    }

    @Test
    @DisplayName("C-01 asynchronous import returns at once (U2)")
    void importDocument_validRequest_returnsImmediatelyWithQueuedProgress() {
        long id = submit("A");
        uiDispatcher.flush();
        assertTrue(id > 0);
        assertTrue(loader.loadedPaths().isEmpty());
        assertEquals(List.of(ImportState.QUEUED), states(id));
    }

    @Test
    @DisplayName("C-02 happy path (UC1)")
    void importDocument_successfulLoad_reportsReadingSavingCompletedAndAddsDocument() {
        long id = submit("A");
        finish();
        assertEquals(List.of(ImportState.QUEUED, ImportState.READING,
                ImportState.SAVING, ImportState.COMPLETED),
                states(id).stream().distinct().toList());
        assertEquals("text", repository.findByTitle("A").orElseThrow().content());
    }

    @Test
    @DisplayName("C-03 invalid file (D3)")
    void importDocument_invalidFile_reportsFailedAndImportFailed() {
        loader.registerFailure(SOURCE, new InvalidDocumentException(
                InvalidDocumentException.Reason.NON_ASCII_CONTENT, "bad ASCII"));
        long id = controller.importDocument(SOURCE, "A", false);
        finish();
        List<ImportState> progress = states(id);
        assertEquals(ImportState.FAILED, progress.get(progress.size() - 1));
        assertEquals("bad ASCII",
                listener.importFailures().get(0).errorMessage().orElseThrow());
    }

    @Test
    @DisplayName("C-04 D2 race at commit")
    void importDocument_titleTakenAtCommit_failsWithoutOverwriting() {
        repository.setBeforeAddHook(document ->
                repository.putDirectly(TestDocuments.document("A", "original")));
        submit("A");
        finish();
        assertEquals("original", repository.findByTitle("A").orElseThrow().content());
        assertEquals(1, listener.importFailures().size());
    }

    @Test
    @DisplayName("C-05 overwrite confirmed (D2)")
    void importDocument_replaceConfirmed_passesFlagToRepository() {
        repository.putDirectly(TestDocuments.document("A", "original"));
        loader.register(SOURCE, "updated");
        controller.importDocument(SOURCE, "A", true);
        finish();
        assertTrue(repository.addCalls().get(0).replaceExisting());
        assertEquals("updated", repository.findByTitle("A").orElseThrow().content());
    }

    @Test
    @DisplayName("C-06 precondition")
    void importDocument_invalidTitle_throwsIllegalArgumentExceptionAndSchedulesNothing() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.importDocument(SOURCE, "", false));
        assertEquals(0, scheduler.pendingCount());
    }

    @Test
    @DisplayName("C-07 multiple imports (U2)")
    void importDocument_twoConcurrentImports_trackedIndependentlyById() {
        long first = submit("A");
        long second = submit("B");
        finish();
        assertNotEquals(first, second);
        assertTrue(listener.isTerminal(first));
        assertTrue(listener.isTerminal(second));
    }

    @Test
    @DisplayName("C-08 EDT hand-off via UiDispatcher (4.3)")
    void callbacks_beforeFlush_nothingDelivered() {
        submit("A");
        scheduler.runAll();
        assertEquals(0, listener.callbackCount());
        uiDispatcher.flush();
        assertTrue(listener.callbackCount() > 0);
    }

    @Test
    @DisplayName("C-09 delete (UC3)")
    void deleteDocuments_runsInBackgroundAndReportsFailure() {
        repository.putDirectly(TestDocuments.document("A"));
        repository.failRemovalOf("A");
        controller.deleteDocuments(List.of("A"));
        assertTrue(repository.removeCalls().isEmpty());
        finish();
        assertEquals(List.of(List.of("A")), repository.removeCalls());
        assertEquals(1, listener.deleteFailures().size());
    }

    @Test
    @DisplayName("C-10 download (UC5, D8)")
    void exportDocument_successAndFailure_reportedWithSnapshot() {
        var snapshot = TestDocuments.document("A");
        controller.exportDocument(snapshot, SOURCE);
        assertTrue(exporter.exports().isEmpty());
        finish();
        assertSame(snapshot, exporter.exports().get(0).document());
        assertTrue(listener.exportEvents().get(0).succeeded());

        exporter.failWith(new DocumentStorageException("cannot export"));
        controller.exportDocument(snapshot, SOURCE);
        finish();
        assertEquals("cannot export", listener.exportEvents().get(1).message());
    }

    @Test
    @DisplayName("C-11 startup load (D5)")
    void loadPersistedDocuments_calledTwice_loadsOnceAndReportsProgress() {
        repository.setPersistedDocuments(List.of(TestDocuments.document("A")));
        controller.loadPersistedDocuments();
        controller.loadPersistedDocuments();
        finish();
        assertEquals(1, repository.loadPersistedCalls());
        assertFalse(listener.startupProgress().isEmpty());
        assertEquals(1, listener.startupCompletions().size());
    }

    @Test
    @DisplayName("C-12 startup load failure")
    void loadPersistedDocuments_storageFailure_reportsStartupLoadFailed() {
        repository.failLoad(new DocumentStorageException("cannot load"));
        controller.loadPersistedDocuments();
        finish();
        assertEquals(List.of("cannot load"), listener.startupFailures());
    }

    @Test
    @DisplayName("C-13 table rows (D6, D7)")
    void currentDocuments_afterRepositoryChange_returnsSortedSummariesWithPreviews() {
        add("Z", "z");
        add("A", "x".repeat(600));
        var rows = controller.currentDocuments();
        assertEquals(List.of("A", "Z"), rows.stream().map(DocumentSummary::title).toList());
        assertEquals(500, rows.get(0).preview().length());
        assertThrows(UnsupportedOperationException.class, rows::clear);
    }

    @Test
    @DisplayName("C-14 changes not caused by the view")
    void repositoryChange_fromOtherSource_dispatchesDocumentsChanged() {
        add("A", "text");
        uiDispatcher.flush();
        assertEquals("A", listener.latestDocuments().get(0).title());
    }

    @Test
    @DisplayName("C-15 exceptions are never swallowed")
    void importDocument_unexpectedRuntimeException_reportsFailedNotSwallowed() {
        loader.registerRuntimeFailure(SOURCE, new IllegalStateException("unexpected"));
        controller.importDocument(SOURCE, "A", false);
        finish();
        assertEquals("Unexpected error while importing: unexpected",
                listener.importFailures().get(0).errorMessage().orElseThrow());
    }

    @Test
    @DisplayName("C-16 listener management")
    void removeLibraryListener_afterRemoval_receivesNothing() {
        controller.removeLibraryListener(listener);
        submit("A");
        finish();
        assertEquals(0, listener.callbackCount());
    }

    @Test
    @DisplayName("C-17 shutdown during import (R8)")
    void cancelAll_withImportInFlight_cancelsAndSilencesCallbacks() {
        submit("A");
        controller.cancelAll();
        finish();
        assertEquals(0, listener.callbackCount());
        assertTrue(repository.addCalls().isEmpty());
        assertThrows(IllegalStateException.class, () -> submit("B"));
    }

    private long submit(String title) {
        loader.register(SOURCE, "text");
        return controller.importDocument(SOURCE, title, false);
    }

    private void add(String title, String text) {
        assertDoesNotThrow(() -> repository.add(TestDocuments.document(title, text), false));
    }

    private void finish() {
        scheduler.runAll();
        uiDispatcher.flush();
    }

    private List<ImportState> states(long id) {
        return listener.progressOf(id).stream().map(ImportProgress::state).toList();
    }
}