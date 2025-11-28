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
            publishRequestDAO.updateRequestStatus(docId, currentUser.getId(), decision);
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
            System.err.println("viewPendingRequests failed: moderator=" + currentUser.getId());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void decideTagRequest(int requestId, RequestStatus decision) {
        TagChangeRequestDAO reqDAO = new TagChangeRequestDAO();
        TagDAO tagDAO = new TagDAO();
        DocumentDAO documentDAO = new DocumentDAO();

        try {
            if (!currentUser.isModerator())
                throw new IllegalArgumentException("Current user is not moderator");
            if (decision == null || decision == RequestStatus.PENDING)
                throw new IllegalArgumentException("Invalid decision");

            TagChangeRequest req = reqDAO.getById(requestId);
            if (req == null)
                throw new IllegalArgumentException("TagChangeRequest not found");
            if (req.getStatus() != RequestStatus.PENDING)
                throw new IllegalArgumentException("Request is not PENDING");

            Document doc = req.getDocument();
            if (doc == null)
                throw new IllegalStateException("Request has no associated document");

            //moderatore rifiuta
            if (decision == RequestStatus.REJECTED) {
                reqDAO.updateStatus(requestId, currentUser.getId(), RequestStatus.REJECTED);
                return;
            }

            //  moderatore approva
            if (decision == RequestStatus.APPROVED) {
                TagChangeOperation op = req.getOperation();

                if (op == TagChangeOperation.ADD) {

                    if (req.isForExistingTag()) {
                        // Aggiunta di un tag già esistente
                        String label = req.getExistingTagLabel();

                        Tag tag = tagDAO.findByLabelNormalized(label);
                        if (tag == null) {
                            throw new IllegalStateException("Existing tag not found in DB: " + label);
                        }

                        documentDAO.addTagToDocument(doc.getId(), tag.getLabel());

                    } else if (req.isForNewLabel()) {
                        // Creazione di un NUOVO tag + collegamento al documento
                        String rawLabel = req.getProposedLabel();
                        if (rawLabel == null || rawLabel.isBlank())
                            throw new IllegalStateException("Proposed label is empty");

                        String label = rawLabel.trim();
                        Tag newTag = new Tag(label, null);
                        tagDAO.addTag(newTag);
                        documentDAO.addTagToDocument(doc.getId(), label);

                    } else {
                        throw new IllegalStateException(
                                "ADD operation requires either existingTagLabel or proposedLabel");
                    }

                } else if (op == TagChangeOperation.REMOVE) {

                    if (!req.isForExistingTag())
                        throw new IllegalStateException("REMOVE operation requires existingTagLabel");

                    String label = req.getExistingTagLabel();

                    // Rimuovo SOLO il link documento-tag, NON cancello il tag dalla tabella Tag
                    documentDAO.removeTagFromDocument(doc.getId(), label);

                } else {
                    throw new IllegalStateException("Unsupported TagChangeOperation: " + op);
                }

                // Dopo aver applicato gli effetti sul dominio/DB,
                // aggiorno lo stato della richiesta e registro il moderatore
                reqDAO.updateStatus(requestId, currentUser.getId(), RequestStatus.APPROVED);
            }

        } catch (Exception e) {
            System.err.println(
                    "decideTagRequest failed: requestId=" + requestId +
                            ", decision=" + decision +
                            ", moderator=" + currentUser.getId()
            );
            e.printStackTrace();
        }
    }



    public ArrayList<TagChangeRequest> viewPendingTagRequests() {
        TagChangeRequestDAO requestDAO = new TagChangeRequestDAO();
        try {
            if (!currentUser.isModerator())
                throw new IllegalArgumentException("Current user is not a moderator");

            return new ArrayList<>(requestDAO.getPending());
        } catch (Exception e) {
            System.err.println("viewPendingTagRequests failed: moderator=" + currentUser.getId());
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

    public ArrayList<TagChangeRequest> viewTagRequestHistory() {
        TagChangeRequestDAO reqDAO = new TagChangeRequestDAO();

        try {
            if (!currentUser.isModerator())
                throw new IllegalArgumentException("Current user is not moderator");

            return new ArrayList<>(reqDAO.getRequestByModerator(currentUser.getId()));

        } catch (Exception e) {
            System.err.println("viewTagRequestHistory failed: moderator=" + currentUser.getId());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }


}
