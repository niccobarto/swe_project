package ORM;

import DomainModel.Document;
import DomainModel.PublishRequest;
import DomainModel.RequestStatus;
import DomainModel.User;

import java.util.List;
import java.util.concurrent.Flow;

public class PublishRequestDAO extends BaseDAO {

    public PublishRequestDAO() {}
    public void addRequest(Document doc) {}
    public void removeRequest(int docId) {}
    public void updateRequestStatus(int docId,RequestStatus status){}
    public List<PublishRequest> getRequestByModerator(int userId){return null;}
    public List<PublishRequest> getRequestsByAuthor(int userId){return null;}
    public List<PublishRequest> getRequestsByStatus(RequestStatus status){return null;}
}
