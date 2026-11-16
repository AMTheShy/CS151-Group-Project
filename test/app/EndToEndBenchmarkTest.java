package app;

import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import testsupport.ImmediateUiDispatcher;

/**
 * Tests for {@code LocalSearchEngineApp.wire (real A + B + C)}. Owner: Member C.
 *
 * <p>Cases I-10..I-11 of {@code design/test-plan.md}. Every test is {@code @Disabled} until
 * implemented: remove the annotation, implement Arrange-Act-Assert as described in the method
 * Javadoc, and keep the method name and {@code @DisplayName} unchanged (they are referenced by
 * the test plan and the traceability table).</p>
 */
class EndToEndBenchmarkTest {

    @TempDir
    Path tempDir;

    private LocalSearchEngineApp.Components components;

    /** Creates fresh fixtures for every test (no shared state between tests). */
    @BeforeEach
    void setUp() {
        components = LocalSearchEngineApp.wire(tempDir.resolve("documents"), new ImmediateUiDispatcher());
    }

    /**
     * I-10: 100 MB search &lt;= 1 s (3.4) [benchmark].
     *
     * <p>Arrange: 20 x 5 MB generated documents imported; 2 warm-up searches; query 'computer
     * algorithm'.</p>
     * <p>Act: search.</p>
     * <p>Assert: time from startSearch to onSearchCompleted &lt;= 1000 ms (median of 3).</p>
     */
    @Test
    @Tag("benchmark")
    @Disabled("TODO: Member C")
    @DisplayName("I-10 100 MB search <= 1 s (3.4)")
    void search_hundredMegabyteCorpus_completesWithinOneSecond() {
        fail("TODO: Member C - I-10");
    }

    /**
     * I-11: 5 MB import &lt;= 1 s (3.4) [benchmark].
     *
     * <p>Arrange: 5 MB generated file.</p>
     * <p>Act: import.</p>
     * <p>Assert: time from importDocument to COMPLETED &lt;= 1000 ms.</p>
     */
    @Test
    @Tag("benchmark")
    @Disabled("TODO: Member C")
    @DisplayName("I-11 5 MB import <= 1 s (3.4)")
    void import_fiveMegabyteFile_completesWithinOneSecond() {
        fail("TODO: Member C - I-11");
    }
}
