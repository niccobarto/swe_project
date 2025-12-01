package DomainModel;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DocumentSearchCriteriaBuilderTest {

    @Test
    void build_withAllFields_shouldPopulateCriteria() {
        Date after = new Date(1000L);
        Date before = new Date(2000L);
        List<String> tags = new ArrayList<>(Arrays.asList("tag1", "tag2"));

        DocumentSearchCriteria criteria = DocumentSearchCriteriaBuilder.getInstance()
                .setDocumentTitle("My doc")
                .setAuthorId(5)
                .setFormat(DocumentFormat.PDF)
                .setCreatedAfter(after)
                .setCreatedBefore(before)
                .setTags(tags)
                .build();

        assertTrue(criteria.getDocumentTitle().isPresent());
        assertEquals("My doc", criteria.getDocumentTitle().get());

        assertTrue(criteria.getAuthorId().isPresent());
        assertEquals(5, criteria.getAuthorId().get());

        assertTrue(criteria.getFormat().isPresent());
        assertEquals(DocumentFormat.PDF, criteria.getFormat().get());

        assertTrue(criteria.getCreatedAfter().isPresent());
        assertEquals(after, criteria.getCreatedAfter().get());

        assertTrue(criteria.getCreatedBefore().isPresent());
        assertEquals(before, criteria.getCreatedBefore().get());

        assertTrue(criteria.getTags().isPresent());
        assertEquals(2, criteria.getTags().get().size());
        assertEquals(Arrays.asList("tag1", "tag2"), criteria.getTags().get());

        // ensure defensive copy: modifying original list does not affect criteria
        tags.add("newtag");
        assertEquals(2, criteria.getTags().get().size());
    }

    @Test
    void build_withNoFields_shouldReturnEmptyOptionals() {
        DocumentSearchCriteria criteria = DocumentSearchCriteriaBuilder.getInstance()
                .build();

        assertFalse(criteria.getDocumentTitle().isPresent());
        assertFalse(criteria.getAuthorId().isPresent());
        assertFalse(criteria.getFormat().isPresent());
        assertFalse(criteria.getCreatedAfter().isPresent());
        assertFalse(criteria.getCreatedBefore().isPresent());
        assertFalse(criteria.getTags().isPresent());
    }

    @Test
    void setTags_withNull_shouldKeepTagsNull() {
        DocumentSearchCriteria criteria = DocumentSearchCriteriaBuilder.getInstance()
                .setTags(null)
                .build();

        assertFalse(criteria.getTags().isPresent());
    }
}

