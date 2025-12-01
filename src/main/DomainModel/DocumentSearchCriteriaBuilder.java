package DomainModel;

import java.util.Date;
import java.util.List;

public class DocumentSearchCriteriaBuilder {
    private String documentTitle;
    private Integer authorId;
    private DocumentFormat format;
    private Date createdAfter;
    private Date createdBefore;
    private List<String> tags;

    public DocumentSearchCriteriaBuilder setDocumentTitle(String documentTitle) {
        this.documentTitle = documentTitle;
        return this;
    }
    public DocumentSearchCriteriaBuilder setAuthorId(Integer authorId) {
        this.authorId = authorId;
        return this;
    }

    public DocumentSearchCriteriaBuilder setFormat(DocumentFormat format) {
        this.format = format;
        return this;
    }
    public DocumentSearchCriteriaBuilder setCreatedAfter(Date createdAfter) {
        this.createdAfter = createdAfter;
        return this;
    }
    public DocumentSearchCriteriaBuilder setCreatedBefore(Date createdBefore) {
        this.createdBefore = createdBefore;
        return this;
    }
    public DocumentSearchCriteriaBuilder setTags(List<String> tags) {
        this.tags = tags;
        return this;
    }

    private DocumentSearchCriteriaBuilder(){}

    public static DocumentSearchCriteriaBuilder getInstance(){
        return new DocumentSearchCriteriaBuilder();
    }

    public DocumentSearchCriteria build() {
        DocumentSearchCriteria criteria = new DocumentSearchCriteria();
        criteria.setDocumentTitle(this.documentTitle);
        criteria.setAuthorId(this.authorId);
        criteria.setFormat(this.format);
        criteria.setCreatedAfter(this.createdAfter);
        criteria.setCreatedBefore(this.createdBefore);
        criteria.setTags(this.tags);
        return criteria;
    }
}
