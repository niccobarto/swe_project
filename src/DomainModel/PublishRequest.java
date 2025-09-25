package DomainModel;

import java.util.Date;

public class PublishRequest {
    private RequestStatus status;
    private String motivation;
    private Date date;
    private Document document;

    public PublishRequest(String motivation, Date date, Document document) {
        this.status = RequestStatus.PENDING;
        this.motivation = motivation;
        this.date = date;
        this.document = document;
    }

    public String getMotivation() {
        return motivation;
    }

    public void setMotivation(String motivation) {
        this.motivation = motivation;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }
}
