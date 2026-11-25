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

/**
 * Composition root and entry point of the Local Search Engine.
 *
 * <p><b>Owner:</b> Member C (Integration).</p>
 *
 * <p>This is the <b>only</b> class allowed to {@code new} implementation classes of other modules
 * (dependency rule 4.3). Everyone else receives interfaces through constructors (Dependency
 * Injection). Keeping all wiring here means swapping an implementation (e.g. a different
 * {@code SearchEngine}) is a one-line change.</p>
 *
 * <p><b>{@link #wire(Path, UiDispatcher)} - required object graph:</b></p>
 * <pre>
 * store      = new FileSystemDocumentStore(documentsDirectory)            // Member A
 * repository = new DefaultDocumentRepository(store)                       // Member A
 * loader     = new StreamingDocumentLoader()                              // Member A
 * exporter   = new FileSystemDocumentExporter()                           // Member A
 * snippets   = new DefaultSnippetExtractor()                              // Member B
 * engine     = new LinearScanSearchEngine(snippets)                       // Member B
 * scheduler  = ExecutorTaskScheduler.createDefault()                      // Member C
 * library    = new DefaultDocumentLibraryController(repository, loader, exporter, snippets, scheduler, dispatcher)
 * search     = new DefaultSearchController(repository, engine, scheduler, dispatcher)
 * lifecycle  = new DefaultLifecycleController(library, search, scheduler)
 * </pre>
 * <p>{@code wire} has no Swing dependency, so the integration tests call it with a temporary
 * directory and an {@code ImmediateUiDispatcher}.</p>
 *
 * <p><b>{@link #main(String[])} - required startup sequence:</b> (1) configure
 * {@code java.util.logging} (console handler, level INFO); (2)
 * {@code components = wire(AppConstants.defaultDocumentsDirectory(), new SwingUiDispatcher())};
 * (3) on the EDT ({@code SwingUtilities.invokeLater}): create {@code new SwingDialogService()},
 * {@code new MainFrame(components.library(), components.search(), components.lifecycle(), dialogs)},
 * show it; (4) still on the EDT after the frame is visible:
 * {@code components.library().loadPersistedDocuments()} so the progress appears on the left (D5).
 * {@code main} itself returns immediately.</p>
 *
 * <p><b>Threading:</b> {@code main} runs on the main thread; GUI creation on the EDT.</p>
 *
 * <p><b>Implementation notes:</b> Initialization failures shut down the scheduler or
 * lifecycle before propagating to the caller or EDT.</p>
 */
public final class LocalSearchEngineApp {

    /**
     * The controllers produced by {@link #wire}, i.e. everything a view needs.
     *
     * @param library   the document library facade
     * @param search    the search facade
     * @param lifecycle shutdown
     */
    public record Components(
            DocumentLibraryController library,
            SearchController search,
            LifecycleController lifecycle) {

        /**
         * Validates the components.
         *
         * @throws NullPointerException if a component is {@code null}
         */
        public Components {
            Objects.requireNonNull(library, "library");
            Objects.requireNonNull(search, "search");
            Objects.requireNonNull(lifecycle, "lifecycle");
        }
    }

    private LocalSearchEngineApp() {
        throw new AssertionError("LocalSearchEngineApp is not instantiable");
    }

    /**
     * Starts the application (see class description).
     *
     * @param args ignored
     */
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

    /**
     * Builds the complete object graph without any GUI (see class description). Used by
     * {@link #main} and by integration tests (I-01..I-13).
     *
     * @param documentsDirectory directory of the read-only copies, not {@code null}
     * @param uiDispatcher       how callbacks reach the UI thread, not {@code null}
     * @return the wired controllers
     * @throws NullPointerException if an argument is {@code null}
     */
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