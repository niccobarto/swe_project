package BusinessLogic;

import ORM.*;
import DomainModel.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class UserController {

    private final User currentUser;

    public UserController(User currentUser) {
        this.currentUser = Objects.requireNonNull(currentUser);
    }

    //TODO: dopo aver deciso la search col prof creare il metodo searchDocuments

    public void writeComment(int documentId, String text) {
        CommentDAO commentDAO = new CommentDAO();
        DocumentDAO documentDAO = new DocumentDAO();
        try{
            if (text == null || text.isBlank())
                throw new IllegalArgumentException("Empty comment text");
            Document doc = documentDAO.getDocumentById(documentId);
            if (doc == null)
                throw new IllegalArgumentException("Document not found");
            commentDAO.addComment(text, currentUser, doc);
        } catch (Exception e){
            System.err.println(e.getMessage()); //segnala errore a console
        }
    }

    public void createDocument(String description, String documentPeriod,
                               DocumentFormat format, String fileName,String filePath,
                               String instrument, String tonality,
                               String compositor, String documentType){ //TODO eliminare tutti questi attrubuti... lasciamo spazio ai tag per questi

        DocumentDAO documentDAO = new DocumentDAO();
        try{
            documentDAO.addDocument(currentUser, description, documentPeriod, format, filePath, fileName, documentType);
        }catch (Exception e){
            System.err.println(e.getMessage());
        }
    }

    //TODO capire funzionamento assegnazioine tag per funzioni assignTag e removeTag

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


}
