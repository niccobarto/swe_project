package BusinessLogic;

import ORM.*;
import DomainModel.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ModeratorController {

    private final User currentUser;

    public ModeratorController(User currentUser) {
        this.currentUser = Objects.requireNonNull(currentUser, "currentUser cannot be null" );
    }

    public void updateStatus( int docId, RequestStatus decision){
        PublishRequestDAO publishRequestDAO = new PublishRequestDAO();
        DocumentDAO documentDAO = new DocumentDAO();
        try{
            if(!currentUser.isModerator())
                throw new IllegalArgumentException("Current user is not moderator");
            if( decision == null || decision == RequestStatus.PENDING)
                throw new IllegalArgumentException("Invalid decision");
            List<PublishRequest> pendings = publishRequestDAO.getRequestsByStatus(RequestStatus.PENDING);
            boolean hasPending = pendings.stream().anyMatch( r -> r.getDocument() != null && r.getDocument().getId() == docId);
            if (!hasPending)
                throw new IllegalArgumentException("No pending requests found for this document");
            publishRequestDAO.updateRequestStatus(docId, decision);
            if(decision == RequestStatus.APPROVED)
                documentDAO.updateDocumentStatus( docId, DocumentStatus.PUBLISHED);
            if(decision == RequestStatus.REJECTED)
                documentDAO.updateDocumentStatus(docId, DocumentStatus.DRAFT);
        } catch (Exception e){
            System.err.println("updateStatus failed: docId=" + docId + ", decision=" + decision + ", moderator=" + currentUser.getId());
        }
    }

    public ArrayList<PublishRequest> viewPendingRequests(){
        PublishRequestDAO publishRequestDAO = new PublishRequestDAO();
        try{
            if (!currentUser.isModerator())
                throw new IllegalArgumentException("Current user is not moderator");
            return new ArrayList<>(publishRequestDAO.getRequestsByStatus(RequestStatus.PENDING));
        } catch (Exception e){
            System.err.println("viewPendingRequests failed: docId=" + currentUser.getId());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public ArrayList<PublishRequest> viewRequestHistory(){
        PublishRequestDAO publishRequestDAO = new PublishRequestDAO();
        try{
            if(!currentUser.isModerator())
                throw new IllegalArgumentException("Current user is not moderator");
            return new ArrayList<>(publishRequestDAO.getRequestByModerator(currentUser.getId()));
        } catch (Exception e){
            System.err.println("viewRequestHistory failed: docId=" + currentUser.getId());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

}
