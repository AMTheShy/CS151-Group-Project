package app;

import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import controllers.DefaultDocumentLibraryController;
import controllers.DefaultLifecycleController;
import controllers.DefaultSearchController;
import controllers.DocumentLibraryController;
import controllers.LifecycleController;
import controllers.SearchController;
import controllers.UiDispatcher;
import controllers.concurrency.ExecutorTaskScheduler;
import controllers.concurrency.SwingUiDispatcher;
import models.AppConstants;
import models.search.DefaultSnippetExtractor;
import models.search.LinearScanSearchEngine;
import models.storage.DefaultDocumentRepository;
import models.storage.FileSystemDocumentExporter;
import models.storage.FileSystemDocumentStore;
import models.storage.StreamingDocumentLoader;
import views.MainFrame;
import views.SwingDialogService;

public final class LocalSearchEngineApp {

    public record Components(
            DocumentLibraryController library,
            SearchController search,
            LifecycleController lifecycle) {

        public Components {
            Objects.requireNonNull(library, "library");
            Objects.requireNonNull(search, "search");
            Objects.requireNonNull(lifecycle, "lifecycle");
        }
    }

    private LocalSearchEngineApp() {
        throw new AssertionError("LocalSearchEngineApp is not instantiable");
    }

    public static void main(String[] args) {
        Logger.getLogger("").setLevel(Level.INFO);
        Components components = wire(
                AppConstants.defaultDocumentsDirectory(), new SwingUiDispatcher());

        SwingUtilities.invokeLater(() -> {
            try {
                MainFrame frame = new MainFrame(components.library(), components.search(),
                        components.lifecycle(), new SwingDialogService());
                frame.setVisible(true);
                components.library().loadPersistedDocuments();
            } catch (RuntimeException failure) {
                components.lifecycle().shutdown();
                throw failure;
            }
        });
    }

    public static Components wire(Path documentsDirectory, UiDispatcher uiDispatcher) {
        Objects.requireNonNull(documentsDirectory, "documentsDirectory");
        Objects.requireNonNull(uiDispatcher, "uiDispatcher");

        var repository = new DefaultDocumentRepository(
                new FileSystemDocumentStore(documentsDirectory));
        var snippets = new DefaultSnippetExtractor();
        var scheduler = ExecutorTaskScheduler.createDefault();

        try {
            var library = new DefaultDocumentLibraryController(
                    repository, new StreamingDocumentLoader(),
                    new FileSystemDocumentExporter(), snippets, scheduler, uiDispatcher);
            var search = new DefaultSearchController(
                    repository, new LinearScanSearchEngine(snippets), scheduler, uiDispatcher);
            return new Components(
                    library, search, new DefaultLifecycleController(library, search, scheduler));
        } catch (RuntimeException failure) {
            scheduler.shutdownNow();
            throw failure;
        }
    }
}