package controllers;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import controllers.concurrency.ManualTaskScheduler;
import models.SearchQuery;
import testsupport.FakeDocumentExporter;
import testsupport.FakeDocumentLoader;
import testsupport.FakeDocumentRepository;
import testsupport.FakeSearchEngine;
import testsupport.FakeSnippetExtractor;
import testsupport.QueueingUiDispatcher;
import testsupport.RecordingLibraryListener;
import testsupport.RecordingSearchListener;
import testsupport.TestDocuments;

class DefaultLifecycleControllerTest {

    private ManualTaskScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new ManualTaskScheduler();
    }

    @Test
    @DisplayName("C-38 application exit (R8)")
    void shutdown_calledTwice_cancelsWorkStopsSchedulerOnce() {
        var repository = new FakeDocumentRepository();
        repository.putDirectly(TestDocuments.document("A"));

        var dispatcher = new QueueingUiDispatcher();
        var loader = new FakeDocumentLoader();
        var library = new DefaultDocumentLibraryController(
                repository, loader, new FakeDocumentExporter(),
                new FakeSnippetExtractor(), scheduler, dispatcher);
        var search = new DefaultSearchController(
                repository, new FakeSearchEngine(), scheduler, dispatcher);

        var calls = new RecordingLibraryListener();
        library.addLibraryListener(calls);
        library.importDocument(Path.of("pending.txt"), "B", false);
        SearchHandle handle = search.startSearch(
                SearchQuery.of("needle"), new RecordingSearchListener());

        var lifecycle = new DefaultLifecycleController(library, search, scheduler);
        assertTimeout(Duration.ofMillis(100), lifecycle::shutdown);
        assertTimeout(Duration.ofMillis(100), lifecycle::shutdown);

        assertTrue(handle.isCancelled());
        assertTrue(scheduler.isShutdown());
        assertEquals(0, scheduler.pendingCount());

        dispatcher.flush();
        assertEquals(0, calls.callbackCount());
        assertTrue(loader.loadedPaths().isEmpty());
    }
}