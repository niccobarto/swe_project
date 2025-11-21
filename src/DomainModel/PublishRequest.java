package DomainModel;

import java.util.Date;

public class PublishRequest {
    private final int id;
    private RequestStatus status;
    private String motivation;
    private Date dateRequest;
    private Date dateResult;
    private Document document;

    public PublishRequest(int id,String motivation, Date date_request, Date date_result, Document document) {
        this.id=id;
        this.status = RequestStatus.PENDING;
        this.motivation = motivation;
        this.dateRequest = date_request;
        this.dateResult = date_result;
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

    public Date getDateRequest() {
        return dateRequest;
    }

    public void setDateRequest(Date date_request) {
        this.dateRequest = date_request;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public int getId() {
        return id;
     }
    public Date getDate_result() {
        return dateResult;
    }

    public void setDate_result(Date date_result) {
        this.dateResult = date_result;
    }
}

