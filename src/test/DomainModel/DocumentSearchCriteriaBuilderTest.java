package DomainModel;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DocumentSearchCriteriaBuilderTest {

    @Test
    // Verifica che, quando si impostano tutti i campi, il criterio risultante contenga
    // i valori corrispondenti e che venga fatta una copia difensiva della lista dei tag.
    void buildAllFields() {
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
    // Verifica che, se non si impostano campi, il criterio restituisca Optionals vuoti
    void buildNoFields() {
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
    // Verifica che impostare tags a null mantenga l'optional dei tag vuoto
    void setTagsNull() {
        DocumentSearchCriteria criteria = DocumentSearchCriteriaBuilder.getInstance()
                .setTags(null)
                .build();

        assertFalse(criteria.getTags().isPresent());
    }
}
