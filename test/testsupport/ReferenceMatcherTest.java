package testsupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import models.MatchRange;

/**
 * Tests the test oracle itself, so that Member B can trust it. Owner: Eric.
 */
class ReferenceMatcherTest {

    /** E-49: the oracle implements S1 and S3 exactly as specified. */
    @Test
    @DisplayName("E-49 oracle matches all S1 positives, no S1 negatives, and is non-overlapping")
    void findAll_s1ExamplesAndOverlap_matchSpecification() {
        for (String positive : TestDocuments.S1_POSITIVE_TEXTS) {
            assertEquals(1, ReferenceMatcher.findAll(positive, TestDocuments.S1_QUERY).size(), positive);
        }
        for (String negative : TestDocuments.S1_NEGATIVE_TEXTS) {
            assertTrue(ReferenceMatcher.findAll(negative, TestDocuments.S1_QUERY).isEmpty(), negative);
        }
        assertEquals(List.of(new MatchRange(0, 2), new MatchRange(2, 4)), ReferenceMatcher.findAll("aaaa", "aa"));
        assertEquals(List.of(new MatchRange(0, 2)), ReferenceMatcher.findAll("aaa", "aa"));
    }

    /** E-50: the committed S1 resource contains exactly the documented number of matches. */
    @Test
    @DisplayName("E-50 committed resources contain the documented number of matches")
    void findAll_committedResources_matchDocumentedCounts() throws IOException {
        String s1Cases = TestDocuments.readResource(TestDocuments.RESOURCE_S1_CASES);
        String sample = TestDocuments.readResource(TestDocuments.RESOURCE_SAMPLE_SMALL);

        assertEquals(TestDocuments.S1_CASES_EXPECTED_MATCHES,
                ReferenceMatcher.findAll(s1Cases, TestDocuments.S1_QUERY).size());
        assertEquals(TestDocuments.SAMPLE_SMALL_EXPECTED_MATCHES,
                ReferenceMatcher.findAll(sample, TestDocuments.S1_QUERY).size());
    }
}
