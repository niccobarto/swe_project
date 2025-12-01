package BusinessLogic;

import ORM.*;
import DomainModel.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UserController {

    private final User currentUser;

    public UserController(User currentUser) {
        this.currentUser = Objects.requireNonNull(currentUser);
    }

    public ArrayList<Document> searchDocuments(DocumentSearchCriteria criteria) {
        DocumentDAO documentDAO = new DocumentDAO();
        try {
            return new ArrayList<>(documentDAO.searchDocuments(criteria));
        } catch (Exception e) {
            System.err.println("searchDocuments failed for user=" + currentUser.getId());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }


    public void writeComment(int documentId, String text) {
        CommentDAO commentDAO = new CommentDAO();
        DocumentDAO documentDAO = new DocumentDAO();
        try{
            if (text == null || text.isBlank())
                throw new IllegalArgumentException("Empty comment text");
            Document doc = documentDAO.getDocumentById(documentId);
            if (doc == null)
                throw new IllegalArgumentException("Document not found");
            commentDAO.addComment(text, currentUser.getId(), doc.getId());
        } catch (Exception e){
            System.err.println(e.getMessage()); //segnala errore a console
        }
    }

    public void createDocument(String documentTitle,String description, String documentPeriod,
                               DocumentFormat format,List<String> tags){
        DocumentDAO documentDAO = new DocumentDAO();
        try{
            String filePath="document/"+currentUser.getId()+"/";
            String fileName="doc_"+currentUser.getNextFileName();
            boolean created=documentDAO.addDocument(currentUser, documentTitle, description, documentPeriod, format, filePath, fileName,tags);

            UserDAO userDAO = new UserDAO();

            boolean updated = userDAO.updateNextFileName(currentUser.getId(), currentUser.getNextFileName() + 1);
            if (updated && created) {
                currentUser.incrementNextFileName();
            }
        }catch (Exception e){
            System.err.println(e.getMessage());
        }
    }
    public void deleteDocument(int documentId){
        DocumentDAO documentDAO = new DocumentDAO();
        try{
            Document doc= documentDAO.getDocumentById(documentId);
            if( doc == null)
                throw new IllegalArgumentException("Document not found");
            if ( doc.getAuthor() == null || doc.getAuthor().getId() != currentUser.getId())
                throw new IllegalArgumentException("You are not the author of the document");
            documentDAO.deleteDocument(documentId);
        } catch (Exception e){
            System.err.println(e.getMessage());
        }
    }
    public ArrayList <Document> viewOwnDocuments() {
        DocumentDAO documentDAO = new DocumentDAO();
        try{
            return new ArrayList<>(documentDAO.getDocumentsByAuthor(currentUser.getId()));
        } catch (Exception e){
            System.err.println(e.getMessage());
            return new ArrayList<>();
        }
    }

    public void addDocumentToCollection(int docId, int collectionId) {
        CollectionDAO collectionDAO = new CollectionDAO();
        try{
            boolean ownedByCurrentUser = collectionDAO.getCollectionsByUser(currentUser.getId())
                    .stream()
                    .anyMatch(c -> c.getId() == collectionId);

            if (!ownedByCurrentUser)
                throw new IllegalArgumentException("You are not the author of the collection");

            collectionDAO.addDocumentToCollection(docId, collectionId);
        } catch (Exception e){
            System.err.println("addDocumentToCollection failed: docId=" + docId + ", collectionId=" + collectionId);
            e.printStackTrace();
        }
    }
    public void removeDocumentToCollection(int docId, int collectionId) {
        CollectionDAO collectionDAO = new CollectionDAO();
        try{
            boolean ownedByCurrentUser = collectionDAO.getCollectionsByUser(currentUser.getId())
                    .stream()
                    .anyMatch(c -> c.getId() == collectionId);

            if (!ownedByCurrentUser)
                throw new IllegalArgumentException("You are not the author of the collection");

            collectionDAO.removeDocumentFromCollection(docId, collectionId);
        } catch (Exception e){
            System.err.println("removeDocumentToCollection failed: docId=" + docId + ", collectionId=" + collectionId);
            e.printStackTrace();
        }
    }
    public void askForPublication(int docId) {
        DocumentDAO documentDAO = new DocumentDAO();
        PublishRequestDAO publishRequestDAO = new PublishRequestDAO();
        try {
            Document doc= documentDAO.getDocumentById(docId);
            if( doc == null)
                throw new IllegalArgumentException("Document not found");
            if ( doc.getAuthor() == null || doc.getAuthor().getId() != currentUser.getId())
                throw new IllegalArgumentException("You are not the author of the document");

            //blocca se c'è una richiesta pending sullo stesso documento
            List<PublishRequest> pendings = publishRequestDAO.getRequestsByStatus(RequestStatus.PENDING);
            boolean alreadyPending = pendings.stream().anyMatch(r ->r.getDocument() != null && r.getDocument().getId() == docId);
            if (alreadyPending){
                throw new IllegalArgumentException("Document is already pending");
            }
            publishRequestDAO.addRequest(doc);
            //aggiorna stato
            documentDAO.updateDocumentStatus(docId, DocumentStatus.PENDING);
        } catch (Exception e){
            System.err.println("askForPublication failed: docId=" + docId + ", user=" + currentUser.getId());
            e.printStackTrace();
        }
    }

    public void addDocumentToFavourites(int documentId){
        UserDAO userDAO = new UserDAO();
        try{
            userDAO.addFavouriteDocuments(currentUser.getId(), documentId);
        } catch (Exception e){
            System.err.println(e.getMessage());
        }
    }

    public void removeDocumentFromFavourites(int documentId) {
        UserDAO userDAO = new UserDAO();
        try {
            userDAO.removeFavouriteDocuments(currentUser.getId(), documentId);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public void addCollectionToFavourites(int collectionId) {
        UserDAO userDAO = new UserDAO();
        try {
            userDAO.addFavouriteCollection(currentUser.getId(), collectionId);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public void removeCollectionFromFavourites(int collectionId) {
        UserDAO userDAO = new UserDAO();
        try {
            userDAO.removeFavouriteCollection(currentUser.getId(), collectionId);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public void requestAddExistingTag(int docId, String tagLabel){
        DocumentDAO documentDAO = new DocumentDAO();
        TagDAO tagDAO = new TagDAO();
        TagChangeRequestDAO reqDAO = new TagChangeRequestDAO();

        try {
            if (tagLabel == null || tagLabel.isBlank())
                throw new IllegalArgumentException("Tag label cannot be empty");

            Document doc = documentDAO.getDocumentById(docId);
            if (doc == null)
                throw new IllegalArgumentException("Document not found");
            if (doc.getAuthor() == null || doc.getAuthor().getId() != currentUser.getId())
                throw new IllegalArgumentException("You are not the author of the document");

            //trova tag
            Tag tag = tagDAO.findByLabelNormalized(tagLabel);
            if (tag == null)
                throw new IllegalArgumentException("Tag does not exist. Use requestAddNewTag if you want to propose a new one.");

            //evita doppioni
            boolean duplicate = reqDAO.existsPendingDuplicate(
                    docId,
                    TagChangeOperation.ADD,
                    tag.getLabel(),
                    null
            );
            if (duplicate)
                throw new IllegalArgumentException("There is already a pending request for this tag on this document.");

            TagChangeRequest req = TagChangeRequest.forExistingTag(
                    doc,
                    tag.getLabel(),
                    TagChangeOperation.ADD
            );
            reqDAO.addRequestForExistingTag(req);
    }catch (Exception e) {
            System.err.println("requestAddExistingTag failed: docId=" + docId +
                    ", user=" + currentUser.getId() + ", label=" + tagLabel);
            e.printStackTrace();
        }
    }

    public void requestAddNewTag(int docId, String newLabel){
        DocumentDAO documentDAO = new DocumentDAO();
        TagDAO tagDAO = new TagDAO();
        TagChangeRequestDAO reqDAO = new TagChangeRequestDAO();

        try{
            if (newLabel == null || newLabel.isBlank())
                throw new IllegalArgumentException("Tag label cannot be empty");

            String trimmed= newLabel.trim();

            Document doc = documentDAO.getDocumentById(docId);
            if (doc == null)
                throw new IllegalArgumentException("Document not found");
            if (doc.getAuthor() == null || doc.getAuthor().getId() != currentUser.getId())
                throw new IllegalArgumentException("You are not the author of the document");
            //controllo che non ci sia tag con questa label
            Tag existing = tagDAO.findByLabelNormalized(trimmed);
            if (existing != null)
                throw new IllegalArgumentException("A tag with this label already exists. Use requestAddExistingTag instead.");
            //evita doppioni pending
            boolean duplicate = reqDAO.existsPendingDuplicate(
                    docId,
                    TagChangeOperation.ADD,
                    null,
                    trimmed
            );
            if (duplicate)
                throw new IllegalArgumentException("There is already a pending request for this new tag on this document.");

            TagChangeRequest req = TagChangeRequest.forNewLabel(doc, trimmed);
            reqDAO.addRequestForNewTag(req);
        }catch (Exception e) {
            System.err.println("requestAddNewTag failed: docId=" + docId +
                    ", user=" + currentUser.getId() + ", label=" + newLabel);
            e.printStackTrace();
        }
    }

    public void requestRemoveTag(int docId, String tagLabel) {
        DocumentDAO documentDAO = new DocumentDAO();
        TagDAO tagDAO = new TagDAO();
        TagChangeRequestDAO reqDAO = new TagChangeRequestDAO();

        try {
            if (tagLabel == null || tagLabel.isBlank())
                throw new IllegalArgumentException("Tag label cannot be empty.");

            Document doc = documentDAO.getDocumentById(docId);
            if (doc == null)
                throw new IllegalArgumentException("Document not found.");
            if (doc.getAuthor() == null || doc.getAuthor().getId() != currentUser.getId())
                throw new IllegalArgumentException("You are not the author of this document.");

            Tag tag = tagDAO.findByLabelNormalized(tagLabel);
            if (tag == null)
                throw new IllegalArgumentException("Tag does not exist.");

            // evito duplicati
            boolean duplicate = reqDAO.existsPendingDuplicate(
                    docId,
                    TagChangeOperation.REMOVE,
                    tag.getLabel(),
                    null
            );
            if (duplicate)
                throw new IllegalArgumentException(
                        "There is already a pending remove request for this tag on this document."
                );

            TagChangeRequest req = TagChangeRequest.forExistingTag(
                    doc,
                    tag.getLabel(),
                    TagChangeOperation.REMOVE
            );

            reqDAO.addRequestForExistingTag(req);

        } catch (Exception e) {
            System.err.println(
                    "requestRemoveTag failed: docId=" + docId +
                            ", user=" + currentUser.getId() +
                            ", tag='" + tagLabel + "'"
            );
            e.printStackTrace();
        }
    }
}
