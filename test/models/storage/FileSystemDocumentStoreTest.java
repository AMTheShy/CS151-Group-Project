package models.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import models.Document;
import models.DocumentException;
import models.DocumentNotFoundException;
import models.DocumentStorageException;
import testsupport.TestDocuments;

/**
 * Tests for {@code FileSystemDocumentStore}. Owner: Member A.
 *
 * <p>Cases A-20..A-30 of {@code design/test-plan.md}.</p>
 */
class FileSystemDocumentStoreTest {

    /** Pattern of a valid entry id: 64 lower-case hex characters. */
    private static final String ENTRY_ID_PATTERN = "[0-9a-f]{64}";

    @TempDir
    Path tempDir;

    private Path directory;

    private FileSystemDocumentStore store;

    /** Creates fresh fixtures for every test (no shared state between tests). */
    @BeforeEach
    void setUp() {
        directory = tempDir.resolve("documents");
        store = new FileSystemDocumentStore(directory);
    }

    /**
     * A-20: round trip preserves bytes and metadata (D3, D4) [unit].
     *
     * <p>Arrange: document with CRLF/LF/CR content, fixed importedAt.</p>
     * <p>Act: save / load.</p>
     * <p>Assert: load returns equal document with same importedAt; content file bytes identical.</p>
     */
    @Test
    @DisplayName("A-20 round trip preserves bytes and metadata (D3, D4)")
    void saveThenLoad_documentWithMixedLineBreaks_roundTripsExactly() throws IOException, DocumentException {
        // Arrange
        Instant importedAt = Instant.parse("2026-11-09T12:34:56.789Z");
        Document document = new Document("Notes", "one\r\ntwo\nthree\rfour", importedAt);

        // Act
        store.save(document);
        Document loaded = store.load("Notes");

        // Assert
        assertEquals(document, loaded);
        assertEquals(importedAt, loaded.importedAt());
        assertArrayEquals(document.content().getBytes(StandardCharsets.US_ASCII),
                Files.readAllBytes(contentFile("Notes")));
    }

    /**
     * A-21: read-only copy (D4) [unit].
     *
     * <p>Arrange: save('Notes').</p>
     * <p>Act: save.</p>
     * <p>Assert: &lt;id&gt;.txt exists and Files.isWritable is false.</p>
     */
    @Test
    @DisplayName("A-21 read-only copy (D4)")
    void save_newDocument_contentFileIsReadOnly() throws DocumentStorageException {
        // Act
        store.save(TestDocuments.document("Notes"));

        // Assert
        Path contentFile = contentFile("Notes");
        assertTrue(Files.isRegularFile(contentFile), "content file must exist: " + contentFile);
        assertFalse(Files.isWritable(contentFile), "content file must be read-only");
    }

    /**
     * A-22: replace a read-only entry (Windows pitfall) [unit].
     *
     * <p>Arrange: save v1, then save v2 with same title.</p>
     * <p>Act: save.</p>
     * <p>Assert: load returns v2; still read-only; no .tmp files left.</p>
     */
    @Test
    @DisplayName("A-22 replace a read-only entry (Windows pitfall)")
    void save_existingReadOnlyEntry_replacesContent() throws IOException, DocumentException {
        // Arrange
        store.save(TestDocuments.document("Notes", "version one, a bit longer"));
        Document second = TestDocuments.document("Notes", "version two");

        // Act
        store.save(second);

        // Assert
        assertEquals("version two", store.load("Notes").content());
        assertFalse(Files.isWritable(contentFile("Notes")), "replaced content file must be read-only");
        assertEquals(List.of(), fileNamesEndingWith(".tmp"));
    }

    /**
     * A-23: case-insensitive file systems [unit].
     *
     * <p>Arrange: save 'Foo' and 'foo'.</p>
     * <p>Act: save / entryId.</p>
     * <p>Assert: both load correctly; entryId('Foo') != entryId('foo').</p>
     */
    @Test
    @DisplayName("A-23 case-insensitive file systems")
    void save_titlesDifferingOnlyInCase_storedSeparately() throws DocumentException {
        // Arrange
        store.save(TestDocuments.document("Foo", "upper"));

        // Act
        store.save(TestDocuments.document("foo", "lower"));

        // Assert
        assertEquals("upper", store.load("Foo").content());
        assertEquals("lower", store.load("foo").content());
        assertNotEquals(FileSystemDocumentStore.entryId("Foo"), FileSystemDocumentStore.entryId("foo"));
    }

    /**
     * A-24: titles with / \ : * ? " &lt; &gt; | [unit].
     *
     * <p>Arrange: save title 'a/b\c:d*e?f"g&lt;h&gt;i|j'.</p>
     * <p>Act: save / entryId.</p>
     * <p>Assert: load returns it; entryId is 64 lower-case hex chars.</p>
     */
    @Test
    @DisplayName("A-24 titles with / \\ : * ? \" < > |")
    void save_titleWithIllegalFileNameCharacters_succeeds() throws DocumentException {
        // Arrange
        String title = "a/b\\c:d*e?f\"g<h>i|j";
        Document document = TestDocuments.document(title, "special");

        // Act
        store.save(document);

        // Assert
        Document loaded = store.load(title);
        assertEquals(title, loaded.title());
        assertEquals("special", loaded.content());
        assertTrue(FileSystemDocumentStore.entryId(title).matches(ENTRY_ID_PATTERN),
                "entry id must be 64 lower-case hex characters");
    }

    /**
     * A-25: delete read-only entry [unit].
     *
     * <p>Arrange: save 'A'; delete('A'); delete('A'); delete('Missing').</p>
     * <p>Act: delete.</p>
     * <p>Assert: no files left for A; no exception.</p>
     */
    @Test
    @DisplayName("A-25 delete read-only entry")
    void delete_existingAndMissingEntries_removesFilesIdempotently() throws IOException, DocumentException {
        // Arrange
        store.save(TestDocuments.document("A"));

        // Act
        store.delete("A");
        store.delete("A");
        store.delete("Missing");

        // Assert
        assertEquals(List.of(), fileNamesStartingWith(FileSystemDocumentStore.entryId("A")));
        assertEquals(List.of(), store.listStoredTitles());
    }

    /**
     * A-26: crash recovery [unit].
     *
     * <p>Arrange: save 'b','A'; create stray x.tmp and orphan &lt;id&gt;.txt without metadata.</p>
     * <p>Act: listStoredTitles.</p>
     * <p>Assert: returns [A, b].</p>
     */
    @Test
    @DisplayName("A-26 crash recovery")
    void listStoredTitles_withTempAndOrphanFiles_returnsCompleteEntriesSorted() throws IOException, DocumentException {
        // Arrange
        store.save(TestDocuments.document("b"));
        store.save(TestDocuments.document("A"));
        TestDocuments.writeAsciiFile(directory, "x.tmp", "half-written");
        String orphanId = FileSystemDocumentStore.entryId("Orphan");
        TestDocuments.writeAsciiFile(directory, orphanId + FileSystemDocumentStore.CONTENT_SUFFIX, "no metadata");

        // Act
        List<String> titles = store.listStoredTitles();

        // Assert
        assertEquals(List.of("A", "b"), titles);
    }

    /**
     * A-27: missing entry [unit].
     *
     * <p>Arrange: empty store; load('X').</p>
     * <p>Act: load.</p>
     * <p>Assert: DocumentNotFoundException.</p>
     */
    @Test
    @DisplayName("A-27 missing entry")
    void load_missingTitle_throwsDocumentNotFoundException() {
        // Act
        DocumentNotFoundException thrown = assertThrows(DocumentNotFoundException.class, () -> store.load("X"));

        // Assert
        assertEquals("X", thrown.title());
    }

    /**
     * A-28: corrupt entry [unit].
     *
     * <p>Arrange: save 'A'; overwrite content file with fewer bytes (clear read-only first).</p>
     * <p>Act: load.</p>
     * <p>Assert: DocumentStorageException; listStoredTitles excludes 'A'.</p>
     */
    @Test
    @DisplayName("A-28 corrupt entry")
    void load_lengthMismatch_throwsDocumentStorageException() throws IOException, DocumentException {
        // Arrange
        store.save(TestDocuments.document("A", "the full original content"));
        Path contentFile = contentFile("A");
        assertTrue(contentFile.toFile().setWritable(true), "could not clear the read-only flag");
        Files.write(contentFile, "trunc".getBytes(StandardCharsets.US_ASCII));

        // Act
        DocumentStorageException thrown = assertThrows(DocumentStorageException.class, () -> store.load("A"));

        // Assert
        assertTrue(thrown.getMessage().contains("\"A\""), "message must name the title: " + thrown.getMessage());
        assertEquals(List.of(), store.listStoredTitles());
    }

    /**
     * A-29: lazy directory creation and unusable directory [unit].
     *
     * <p>Arrange: directory does not exist; then a regular FILE at the directory path.</p>
     * <p>Act: save / listStoredTitles.</p>
     * <p>Assert: first: directory created, empty list; second: DocumentStorageException.</p>
     */
    @Test
    @DisplayName("A-29 lazy directory creation and unusable directory")
    void listStoredTitles_missingDirectory_createsIt() throws IOException, DocumentStorageException {
        // Arrange
        assertFalse(Files.exists(directory), "precondition: directory must not exist yet");
        Path fileInsteadOfDirectory = TestDocuments.writeAsciiFile(tempDir, "not-a-folder", "x");
        FileSystemDocumentStore unusableStore = new FileSystemDocumentStore(fileInsteadOfDirectory);

        // Act
        List<String> titles = store.listStoredTitles();

        // Assert
        assertTrue(Files.isDirectory(directory), "directory must be created lazily");
        assertEquals(List.of(), titles);
        assertThrows(DocumentStorageException.class, unusableStore::listStoredTitles);
        assertThrows(DocumentStorageException.class, () -> unusableStore.save(TestDocuments.document("A")));
    }

    /**
     * A-30: persistence across restarts (D5) [unit].
     *
     * <p>Arrange: store1.save(A, B); store2 = new store on same dir.</p>
     * <p>Act: listStoredTitles / load.</p>
     * <p>Assert: store2.listStoredTitles() = [A, B]; loads equal docs.</p>
     */
    @Test
    @DisplayName("A-30 persistence across restarts (D5)")
    void newInstance_sameDirectory_seesPreviouslySavedDocuments() throws DocumentException {
        // Arrange
        Document first = TestDocuments.document("A", "first");
        Document second = TestDocuments.document("B", "second");
        store.save(first);
        store.save(second);
        FileSystemDocumentStore restarted = new FileSystemDocumentStore(directory);

        // Act
        List<String> titles = restarted.listStoredTitles();
        Document loadedFirst = restarted.load("A");
        Document loadedSecond = restarted.load("B");

        // Assert
        assertEquals(List.of("A", "B"), titles);
        assertEquals(first, loadedFirst);
        assertEquals(second, loadedSecond);
        assertEquals(first.importedAt(), loadedFirst.importedAt());
    }

    /**
     * Returns the content file of a stored title.
     *
     * @param title the title
     * @return {@code <directory>/<id>.txt}
     */
    private Path contentFile(String title) {
        return directory.resolve(FileSystemDocumentStore.entryId(title) + FileSystemDocumentStore.CONTENT_SUFFIX);
    }

    /**
     * Lists file names in the storage directory that end with a suffix.
     *
     * @param suffix the suffix
     * @return the matching names, empty if none
     * @throws IOException if listing fails
     */
    private List<String> fileNamesEndingWith(String suffix) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            return files.map(path -> path.getFileName().toString()).filter(name -> name.endsWith(suffix)).toList();
        }
    }

    /**
     * Lists file names in the storage directory that start with a prefix.
     *
     * @param prefix the prefix
     * @return the matching names, empty if none
     * @throws IOException if listing fails
     */
    private List<String> fileNamesStartingWith(String prefix) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            return files.map(path -> path.getFileName().toString()).filter(name -> name.startsWith(prefix)).toList();
        }
    }
}
