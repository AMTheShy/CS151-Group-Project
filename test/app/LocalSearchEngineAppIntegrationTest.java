    @Test
    @DisplayName("I-03 D2 race end to end")
    void import_sameTitleConcurrently_exactlyOneSucceeds() throws Exception {
        Path first = TestDocuments.writeAsciiFile(tempDir, "one.txt", "one");
        Path second = TestDocuments.writeAsciiFile(tempDir, "two.txt", "two");

        long firstImportId = components.library().importDocument(first, "Same", false);
        long secondImportId = components.library().importDocument(second, "Same", false);

        assertTrue(libraryEvents.await(events -> events.isTerminal(firstImportId)
                && events.isTerminal(secondImportId) && events.importFailures().size() == 1, TIMEOUT));
        assertTrue(libraryEvents.importFailures().get(0).errorMessage()
                .orElseThrow().contains("already exists"));
        assertEquals(1, components.library().currentDocuments().size());
    }