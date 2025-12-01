package BusinessLogic;
import ORM.*;
import DomainModel.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AdminController {
    private final User currentUser;
    private UserDAO userDAO;

    public AdminController(User currentUser) {
        Objects.requireNonNull(currentUser);
        this.userDAO = new UserDAO();
        // Non blocchiamo più la creazione del controller; i controlli di autorizzazione
        // vengono effettuati all'inizio di ogni operazione tramite ensureAdmin().
        this.currentUser = currentUser;
    }
    public AdminController(User currentUser, UserDAO userDAO) {
        Objects.requireNonNull(currentUser);
        this.userDAO = userDAO;
        // Non blocchiamo più la creazione del controller; i controlli di autorizzazione
        // vengono effettuati all'inizio di ogni operazione tramite ensureAdmin().
        this.currentUser = currentUser;
    }

    // Metodo centralizzato per verificare i permessi admin
    private void ensureAdmin() {
        if (!this.currentUser.isAdmin()) {
            throw new IllegalArgumentException("Accesso negato: operazione riservata agli admin");
        }
    }

    public User searchUserById(int userId){
        ensureAdmin();
        try{
            UserDAO userDAO = new UserDAO();
            User u = userDAO.getUserById(userId);
            return u; // può essere null
        }catch(Exception e){
            System.err.println("searchUserById failed: " + e);
            return null;
        }
    }

    public void removeUser(int userId){
        ensureAdmin();
        try{
            UserDAO userDAO = new UserDAO();
            userDAO.removeUser(userId);
        }catch(Exception e){
            System.err.println("removeUser failed: " + e);
        }
    }

    public User searchUserByEmail(String email){
        ensureAdmin();
        try{
            UserDAO userDAO = new UserDAO();
            User u = userDAO.getUserByEmail(email);
            return u;
        }catch(Exception e){
            System.err.println("searchUserByEmail failed: " + e);
            return null;
        }
    }

    public List<User> allUsers(){
        ensureAdmin();
        try{
            UserDAO userDAO = new UserDAO();
            List<User> users = userDAO.getAllUsers();
            return users != null ? users : new ArrayList<>();
        }catch(Exception e){
            System.err.println("allUsers failed: " + e);
            return new ArrayList<>();
        }
    }

    public List<Document> allDocuments(){
        ensureAdmin();
        try{
            DocumentDAO documentDAO = new DocumentDAO();
            List<Document> docs = documentDAO.getAllDocuments();
            return docs != null ? docs : new ArrayList<>();
        }catch(Exception e){
            System.err.println("allDocuments failed: " + e);
            return new ArrayList<>();
        }
    }

    public List<Document> documentsByAuthor(int userId){
        ensureAdmin();
        try{
            DocumentDAO documentDAO = new DocumentDAO();
            List<Document> docs = documentDAO.getDocumentsByAuthor(userId);
            return docs != null ? docs : new ArrayList<>();
        }catch(Exception e){
            System.err.println("documentsByAuthor failed: " + e);
            return new ArrayList<>();
        }
    }

    public void setModerator(int userId, boolean isModerator){
        ensureAdmin();
        try {
            UserDAO userDAO = new UserDAO();
            userDAO.setModerator(userId, isModerator);
        } catch (Exception e) {
            System.err.println("setModerator failed: " + e);
        }
    }

    public void removeComment(int commentId){
        //Todo
    }
    public List<Document> documentsApprovedByModerator(int moderatorId){
        ensureAdmin();
        try{
            UserDAO userDAO = new UserDAO();
            User moderatorUser = userDAO.getUserById(moderatorId);
            if(moderatorUser == null) return new ArrayList<>();
            List<PublishRequest> moderatorHistory = new ModeratorController(moderatorUser).viewRequestHistory();
            List<Document> documents = new ArrayList<>();
            if(moderatorHistory != null){
                for(PublishRequest pr : moderatorHistory){
                    if(pr != null && pr.getDocument() != null) documents.add(pr.getDocument());
                }
            }
            return documents;
        }catch(Exception e){
            System.err.println("documentsApprovedByModerator failed: " + e);
            return new ArrayList<>();
        }
    }

    public List<Document> documentsByStatus(DocumentStatus status) {
        ensureAdmin();
        try{
            DocumentDAO documentDAO = new DocumentDAO();
            List<Document> docs = documentDAO.getDocumentsByStatus(status);
            return docs != null ? docs : new ArrayList<>();
        }catch(Exception e){
            System.err.println("documentsByStatus failed: " + e);
            return new ArrayList<>();
        }
    }

    public List<Collection> allCollections(){
        ensureAdmin();
        try{
            CollectionDAO collectionDAO = new CollectionDAO();
            List<Collection> cols = collectionDAO.getAllCollections();
            return cols != null ? cols : new ArrayList<>();
        }catch(Exception e){
            System.err.println("allCollections failed: " + e);
            return new ArrayList<>();
        }
    }

    public void deleteCollection(int collectionId){
        ensureAdmin();
        try{
            CollectionDAO collectionDAO = new CollectionDAO();
            collectionDAO.deleteCollection(collectionId);
        }catch(Exception e){
            System.err.println("deleteCollection failed: " + e);
        }
    }

    public List<Comment> getCommentByAuthor(int authorId){
        ensureAdmin();
        try{
            CommentDAO commentDAO = new CommentDAO();
            List<Comment> comments = commentDAO.getCommentsByAuthor(authorId);
            return comments != null ? comments : new ArrayList<>();
        }catch(Exception e){
            System.err.println("getCommentByAuthor failed: " + e);
            return new ArrayList<>();
        }
    }

    public List<Comment> getCommentsByDocument(int documentId){
        ensureAdmin();
        try{
            CommentDAO commentDAO = new CommentDAO();
            List<Comment> comments = commentDAO.getCommentsByDocument(documentId);
            return comments != null ? comments : new ArrayList<>();
        }catch(Exception e){
            System.err.println("getCommentsByDocument failed: " + e);
            return new ArrayList<>();
        }
    }
}
