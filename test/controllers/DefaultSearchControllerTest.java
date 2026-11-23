package controllers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import controllers.concurrency.ManualTaskScheduler;
import models.DocumentSearchResult;
import models.SearchQuery;
import testsupport.FakeDocumentRepository;
import testsupport.FakeSearchEngine;
import testsupport.QueueingUiDispatcher;
import testsupport.RecordingSearchListener;
import testsupport.TestDocuments;

class DefaultSearchControllerTest {

    private FakeDocumentRepository repository;
    private FakeSearchEngine engine;
    private ManualTaskScheduler scheduler;
    private QueueingUiDispatcher uiDispatcher;
    private RecordingSearchListener listener;
    private DefaultSearchController controller;

    @BeforeEach
    void setUp() {
        repository = new FakeDocumentRepository();
        engine = new FakeSearchEngine();
        scheduler = new ManualTaskScheduler();
        uiDispatcher = new QueueingUiDispatcher();
        listener = new RecordingSearchListener();
        controller = new DefaultSearchController(repository, engine, scheduler, uiDispatcher);
    }

    @Test
    @DisplayName("C-18 non-blocking search (U3)")
    void startSearch_returnsImmediately_notDoneAndNoCallback() {
        put("Alpha", "needle");
        SearchHandle handle = start();
        assertFalse(handle.isDone());
        assertEquals(0, listener.callbackCount());
        assertEquals(1, scheduler.pendingCount());
    }

    @Test
    @DisplayName("C-19 snapshot at start (S5)")
    void startSearch_documentAddedAfterStart_notInResults() {
        put("Alpha", "needle");
        start();
        put("Beta", "needle");
        finish();
        assertEquals(List.of("Alpha"), titles(listener));
    }

    @Test
    @DisplayName("C-20 deleted during search stays (S5)")
    void startSearch_documentRemovedDuringSearch_stillInResults() {
        put("Alpha", "needle");
        start();
        assertDoesNotThrow(() -> repository.removeAll(List.of("Alpha")));
        finish();
        assertEquals(List.of("Alpha"), titles(listener));
    }

    @Test
    @DisplayName("C-21 results sorted, only matches (U4, D6)")
    void startSearch_mixedDocuments_onlyMatchesSortedInOneCallback() {
        put("Zulu", "needle");
        put("Alpha", "needle");
        put("Middle", "unrelated");
        start();
        finish();
        assertEquals(List.of("Alpha", "Zulu"), titles(listener));
        assertEquals(1, listener.callbackCount());
        assertThrows(UnsupportedOperationException.class,
                () -> listener.completions().get(0).clear());
    }

    @Test
    @DisplayName("C-22 zero results")
    void startSearch_noMatches_completesWithEmptyList() {
        put("Alpha", "unrelated");
        start();
        finish();
        assertEquals(1, listener.callbackCount());
        assertTrue(titles(listener).isEmpty());
    }

    @Test
    @DisplayName("C-23 close tab during search (U3)")
    void cancel_beforeTasksRun_neverCallsListener() {
        put("Alpha", "needle");
        start().cancel();
        finish();
        assertEquals(0, listener.callbackCount());
        assertTrue(engine.searchedTitles().isEmpty());
    }

    @Test
    @DisplayName("C-24 race R5: results queued for EDT, then tab closed")
    void cancel_afterResultsComputedBeforeDispatch_neverCallsListener() {
        put("Alpha", "needle");
        SearchHandle handle = start();
        scheduler.runAll();
        assertEquals(1, uiDispatcher.pendingCount());
        handle.cancel();
        uiDispatcher.flush();
        assertEquals(0, listener.callbackCount());
    }

    @Test
    @DisplayName("C-25 engine failure")
    void startSearch_engineThrows_reportsFailureOnce() {
        put("Alpha", "needle");
        put("Beta", "needle");
        engine.failOn("Beta", new IllegalStateException("broken engine"));
        start();
        finish();
        assertEquals(List.of("Search failed: broken engine"), listener.failures());
        assertTrue(listener.completions().isEmpty());
    }

    @Test
    @DisplayName("C-26 per-document parallelism (4.4)")
    void startSearch_threeDocuments_submitsOneSearchTaskPerDocument() {
        put("A", "needle");
        put("B", "needle");
        put("C", "needle");
        start();
        assertEquals(3, scheduler.searchTaskCount());
    }

    @Test
    @DisplayName("C-27 cancellation reaches the engine")
    void cancel_runningSearch_setsTokenObservedByEngine() {
        put("Alpha", "needle");
        SearchHandle handle = start();
        engine.setBeforeSearchHook(document -> handle.cancel());
        finish();
        assertTrue(engine.tokensSeen().get(0).isCancelled());
        assertEquals(0, listener.callbackCount());
    }

    @Test
    @DisplayName("C-28 handle state")
    void isDone_afterCompletionOrFailure_isTrue() {
        put("Alpha", "needle");
        SearchHandle success = start();
        finish();
        engine.failOn("Alpha", new IllegalStateException("bad"));
        SearchHandle failure = start();
        finish();
        assertTrue(success.isDone());
        assertTrue(failure.isDone());
        assertFalse(failure.isCancelled());
    }

    @Test
    @DisplayName("C-29 independent tabs")
    void startSearch_twoConcurrentSearches_resultsGoToOwnListeners() {
        put("Alpha", "needle");
        put("Beta", "other");
        RecordingSearchListener second = new RecordingSearchListener();
        start();
        controller.startSearch(SearchQuery.of("other"), second);
        finish();
        assertEquals(List.of("Alpha"), titles(listener));
        assertEquals(List.of("Beta"), titles(second));
    }

    @Test
    @DisplayName("C-30 shutdown cancels searches (R8)")
    void cancelAll_activeSearches_allCancelled() {
        put("Alpha", "needle");
        SearchHandle first = start();
        SearchHandle second = start();
        controller.cancelAll();
        finish();
        assertTrue(first.isCancelled());
        assertTrue(second.isCancelled());
        assertEquals(0, listener.callbackCount());
        assertThrows(IllegalStateException.class, this::start);
    }

    private void put(String title, String text) {
        repository.putDirectly(TestDocuments.document(title, text));
    }

    private SearchHandle start() {
        return controller.startSearch(SearchQuery.of("needle"), listener);
    }

    private void finish() {
        scheduler.runAll();
        uiDispatcher.flush();
    }

    private static List<String> titles(RecordingSearchListener target) {
        return target.completions().get(0).stream()
                .map(DocumentSearchResult::title)
                .toList();
    }
}