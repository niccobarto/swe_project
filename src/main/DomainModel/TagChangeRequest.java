package DomainModel;

import java.util.Date;
public class TagChangeRequest {

    private Integer id;
    private RequestStatus status;
    private Date dateRequest;
    private Date dateResult;
    private Document document;
    private TagChangeOperation operation;
    private String existingTagLabel; //tag già esistente, può essere null
    private String proposedLabel; //i tag saranno creati dopo l'accettazione per evitare tag non assegnati a nessuno
    private User moderator;

    public TagChangeRequest() {}

    public TagChangeRequest(Integer id,
                            RequestStatus status,
                            Date dateRequest,
                            Date dateResult,
                            Document document,
                            TagChangeOperation operation,
                            String existingTagLabel,
                            String proposedLabel) {
        this.id = id;
        this.status = status;
        this.dateRequest = dateRequest;
        this.dateResult = dateResult;
        this.document = document;
        this.operation = operation;
        this.existingTagLabel = existingTagLabel;
        this.proposedLabel = proposedLabel;
    }

    public boolean isForExistingTag() {
        return existingTagLabel != null && (proposedLabel == null || proposedLabel.isBlank());
    }

    public boolean isForNewLabel() {
        return existingTagLabel == null && proposedLabel != null && !proposedLabel.isBlank();
    }

    public static TagChangeRequest forExistingTag(Document doc, String proposedLabel, TagChangeOperation op) {
        TagChangeRequest r = new TagChangeRequest();
        r.status = RequestStatus.PENDING;
        r.dateRequest = new Date();
        r.document = doc;
        r.operation = op;
        r.existingTagLabel = proposedLabel;
        r.proposedLabel = null;
        return r;
    }
    //factory method
    public static TagChangeRequest forNewLabel(Document doc, String proposedLabel) {
        TagChangeRequest r = new TagChangeRequest();
        r.status = RequestStatus.PENDING;
        r.dateRequest = new Date();
        r.document = doc;
        r.operation = TagChangeOperation.ADD;
        r.existingTagLabel = null;
        r.proposedLabel = proposedLabel;
        return r;
    }


    //invarianti
    public void validateOrThrow() { //per garantire correttezza delle request
        boolean existing = isForExistingTag();
        boolean proposing = isForNewLabel();

        if (operation == null)
            throw new IllegalArgumentException("operation is required");
        if (document == null)
            throw new IllegalArgumentException("document is required");
        if ( status == null)
            throw new IllegalArgumentException("status is required");
        if (existing == proposing)
            throw new IllegalArgumentException("need only one of tagId or proposedLabel");
        if (operation == TagChangeOperation.REMOVE && !existing)
            throw new IllegalArgumentException("tagId is required by operation REMOVE");

    }

    public int getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    public Date getDateRequest() { return dateRequest; }
    public void setDateRequest(Date dateRequest) { this.dateRequest = dateRequest; }

    public Date getDateResult() { return dateResult; }
    public void setDateResult(Date dateResult) { this.dateResult = dateResult; }

    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }

    public TagChangeOperation getOperation() { return operation; }
    public void setOperation(TagChangeOperation operation) { this.operation = operation; }

    public String getProposedLabel() { return proposedLabel; }
    public void setProposedLabel(String proposedLabel) { this.proposedLabel = proposedLabel; }

    public String getExistingTagLabel() { return existingTagLabel; }
    public void setExistingTagLabel(String existingTagLabel) { this.existingTagLabel = existingTagLabel; }

    public User getModerator(){ return moderator; }
    public void setModerator(User moderator){ this.moderator = moderator; }
}
