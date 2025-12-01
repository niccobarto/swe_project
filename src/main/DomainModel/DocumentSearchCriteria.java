package DomainModel;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Represents the optional filters that can be used to search documents.
 */
public class DocumentSearchCriteria {

    private String documentTitle;
    private Integer authorId;
    private DocumentFormat format;
    private Date createdAfter;
    private Date createdBefore;
    private List<String> tags;

    public Optional<String> getDocumentTitle() {
        return Optional.ofNullable(documentTitle);
    }

    public void setDocumentTitle(String documentName) {
        this.documentTitle = documentName;
    }

    public Optional<Integer> getAuthorId() {
        return Optional.ofNullable(authorId);
    }

    public void setAuthorId(Integer authorId) {
        this.authorId = authorId;
    }

    public Optional<DocumentFormat> getFormat() {
        return Optional.ofNullable(format);
    }

    public void setFormat(DocumentFormat format) {
        this.format = format;
    }

    public Optional<Date> getCreatedAfter() {
        return Optional.ofNullable(createdAfter);
    }

    public void setCreatedAfter(Date createdAfter) {
        this.createdAfter = createdAfter;
    }

    public Optional<Date> getCreatedBefore() {
        return Optional.ofNullable(createdBefore);
    }

    public void setCreatedBefore(Date createdBefore) {
        this.createdBefore = createdBefore;
    }

    public Optional<List<String>> getTags() {
        return Optional.ofNullable(tags);
    }

    public void setTags(List<String> tags) {
        if (tags == null) {
            this.tags = null;
        } else {
            this.tags = new ArrayList<>(tags);
        }
    }
}