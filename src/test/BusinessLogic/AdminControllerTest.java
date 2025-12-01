package BusinessLogic;

import DomainModel.*;
import ORM.*;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdminControllerTest {

    private Connection conn;
    private UserDAO userDAO;
    private DocumentDAO documentDAO;
    private CommentDAO commentDAO;
    private CollectionDAO collectionDAO;
    private PublishRequestDAO publishRequestDAO;

    private User adminUser;
    private User normalUser;
    private AdminController adminController;

    @BeforeEach
    void setUp() {
        try {
            // ATTIVA la modalità TEST e forza la ricreazione dell'istanza singleton
            DBConnection.setEnableTesting(true);
            DBConnection.resetInstance();

            conn = DBConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            userDAO = new UserDAO();
            documentDAO = new DocumentDAO();
            commentDAO = new CommentDAO();
            collectionDAO = new CollectionDAO();
            publishRequestDAO = new PublishRequestDAO();

            // Crea utenti (direttamente via DAO)
            String adminEmail = "admin+" + System.currentTimeMillis() + "@example.com";
            userDAO.addUser("Admin", "User", adminEmail, "pwd", true, true);
            adminUser = userDAO.getUserByEmail(adminEmail);
            assertNotNull(adminUser);

            String userEmail = "user+" + System.currentTimeMillis() + "@example.com";
            userDAO.addUser("Normal", "User", userEmail, "pwd", false, false);
            normalUser = userDAO.getUserByEmail(userEmail);
            assertNotNull(normalUser);

            adminController = new AdminController(adminUser);
        } catch (Exception e) {
            fail("setUp failed: " + e.getMessage());
        }
    }

    @AfterEach
    void tearDown() {
        try {
            if (conn != null) {
                conn.rollback();
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            fail("tearDown failed: " + e.getMessage());
        }
    }

    @Test
    void searchUserById_positive_and_negative_and_auth() {
        // positivo
        User found = adminController.searchUserById(normalUser.getId());
        assertNotNull(found);
        assertEquals(normalUser.getEmail(), found.getEmail());
        // negativo: id inesistente
        assertNull(adminController.searchUserById(Integer.MAX_VALUE));
        // accesso negato
        AdminController nonAdmin = new AdminController(normalUser);
        assertThrows(IllegalArgumentException.class, () -> nonAdmin.searchUserById(normalUser.getId()));
    }

    @Test
    void removeUser_positive_and_negative_and_auth() {
        // positivo
        adminController.removeUser(normalUser.getId());
        assertNull(userDAO.getUserById(normalUser.getId()));
        // negativo: id inesistente
        assertDoesNotThrow(() -> adminController.removeUser(Integer.MAX_VALUE));
        // accesso negato
        AdminController nonAdmin = new AdminController(normalUser);
        assertThrows(IllegalArgumentException.class, () -> nonAdmin.removeUser(adminUser.getId()));
    }

    @Test
    void searchUserByEmail_positive_and_negative_and_auth() {
        // positivo
        User u = adminController.searchUserByEmail(normalUser.getEmail());
        assertNotNull(u);
        assertEquals(normalUser.getId(), u.getId());
        // negativo: email inesistente
        assertNull(adminController.searchUserByEmail("nope+" + System.currentTimeMillis() + "@x"));
        // accesso negato
        AdminController nonAdmin = new AdminController(normalUser);
        assertThrows(IllegalArgumentException.class, () -> nonAdmin.searchUserByEmail(normalUser.getEmail()));
    }

    @Test
    void allUsers_positive_and_auth() {
        List<User> users = adminController.allUsers();
        assertNotNull(users);
        assertTrue(users.size() >= 2);
        // accesso negato
        AdminController nonAdmin = new AdminController(normalUser);
        assertThrows(IllegalArgumentException.class, nonAdmin::allUsers);
    }

    @Test
    void allDocuments_positive_and_auth() {
        // setup: crea un documento per normalUser direttamente via DAO
        documentDAO.addDocument(normalUser, "A", "d", "1900", DocumentFormat.PDF, "fp", "fname", List.of("t"));
        List<Document> docs = adminController.allDocuments();
        assertNotNull(docs);
        assertFalse(docs.isEmpty());
        // accesso negato
        AdminController nonAdmin = new AdminController(normalUser);
        assertThrows(IllegalArgumentException.class, nonAdmin::allDocuments);
    }

    @Test
    void documentsByAuthor_positive_and_negative_and_auth() {
        // setup: crea due documenti per normalUser
        documentDAO.addDocument(normalUser, "A1", "d", "1900", DocumentFormat.TXT, "fp", "f1", List.of("t"));
        documentDAO.addDocument(normalUser, "A2", "d", "1901", DocumentFormat.PDF, "fp", "f2", List.of("t"));
        // positivo
        List<Document> docs = adminController.documentsByAuthor(normalUser.getId());
        assertNotNull(docs);
        assertTrue(docs.size() >= 2);
        // negativo: autore inesistente
        List<Document> none = adminController.documentsByAuthor(Integer.MAX_VALUE);
        assertNotNull(none);
        // accesso negato
        AdminController nonAdmin = new AdminController(normalUser);
        assertThrows(IllegalArgumentException.class, () -> nonAdmin.documentsByAuthor(normalUser.getId()));
    }

    @Test
    void setModerator_positive_and_negative_and_auth() {
        // positivo
        adminController.setModerator(normalUser.getId(), true);
        User updated = userDAO.getUserById(normalUser.getId());
        assertTrue(updated.isModerator());
        // negativo: id inesistente
        assertDoesNotThrow(() -> adminController.setModerator(Integer.MAX_VALUE, true));
        // accesso negato
        AdminController nonAdmin = new AdminController(normalUser);
        assertThrows(IllegalArgumentException.class, () -> nonAdmin.setModerator(adminUser.getId(), false));
    }

    @Test
    void removeComment_positive_and_negative_and_auth() {
        // setup: inserisci doc+comment direttamente via DAO
        documentDAO.addDocument(normalUser, "C", "d", "2000", DocumentFormat.PDF, "fp", "fn", List.of("t"));
        int docId = documentDAO.getDocumentsByAuthor(normalUser.getId()).get(0).getId();
        commentDAO.addComment("Hi", normalUser.getId(), docId);
        int commentId = commentDAO.getCommentsByDocument(docId).get(0).getId();
        // positivo: rimuovi commento
        adminController.removeComment(commentId);
        assertTrue(commentDAO.getCommentsByDocument(docId).isEmpty());
        // negativo: id inesistente
        assertDoesNotThrow(() -> adminController.removeComment(Integer.MAX_VALUE));
        // accesso negato
        AdminController nonAdmin = new AdminController(normalUser);
        // ricrea un commento per testare accesso negato
        commentDAO.addComment("Hi2", normalUser.getId(), docId);
        int anotherCommentId = commentDAO.getCommentsByDocument(docId).get(0).getId();
        assertThrows(IllegalArgumentException.class, () -> nonAdmin.removeComment(anotherCommentId));
    }

    @Test
    void documentsApprovedByModerator_positive_and_negative_and_auth() {
        // setup: crea doc e publish request APPROVED direttamente via DAO
        documentDAO.addDocument(normalUser, "P", "d", "2000", DocumentFormat.PDF, "fp", "fn", List.of("t"));
        int docId = documentDAO.getDocumentsByAuthor(normalUser.getId()).get(0).getId();
        publishRequestDAO.addRequest(documentDAO.getDocumentById(docId));
        publishRequestDAO.updateRequestStatus(docId, adminUser.getId(), RequestStatus.APPROVED);
        // positivo: lista contiene il doc approvato del moderator specificato (adminUser)
        List<Document> approved = adminController.documentsApprovedByModerator(adminUser.getId());
        assertTrue(approved.stream().anyMatch(d -> d.getId() == docId));
        // negativo: moderator inesistente
        List<Document> none = adminController.documentsApprovedByModerator(Integer.MAX_VALUE);
        assertNotNull(none);
        // accesso negato
        AdminController nonAdmin = new AdminController(normalUser);
        assertThrows(IllegalArgumentException.class, () -> nonAdmin.documentsApprovedByModerator(adminUser.getId()));
    }

    @Test
    void documentsByStatus_positive_and_negative_and_auth() {
        // setup: due doc, uno PENDING e uno DRAFT
        documentDAO.addDocument(normalUser, "S1", "d", "2000", DocumentFormat.PDF, "fp", "f1", List.of("t"));
        int d1 = documentDAO.getDocumentsByAuthor(normalUser.getId()).get(0).getId();
        documentDAO.updateDocumentStatus(d1, DocumentStatus.PENDING);
        documentDAO.addDocument(normalUser, "S2", "d2", "2001", DocumentFormat.TXT, "fp", "f2", List.of("t"));
        // positivo: trova almeno S1
        List<Document> pending = adminController.documentsByStatus(DocumentStatus.PENDING);
        assertTrue(pending.stream().anyMatch(d -> d.getId() == d1));
        // negativo: status senza doc (es. REJECTED)
        List<Document> rejected = adminController.documentsByStatus(DocumentStatus.REJECTED);
        assertNotNull(rejected);
        // accesso negato
        AdminController nonAdmin = new AdminController(normalUser);
        assertThrows(IllegalArgumentException.class, () -> nonAdmin.documentsByStatus(DocumentStatus.PENDING));
    }

    @Test
    void allCollections_positive_and_auth() {
        // setup: crea una collection direttamente via DAO
        collectionDAO.addCollection("CAll", "desc", normalUser);
        List<Collection> cols = adminController.allCollections();
        assertNotNull(cols);
        assertFalse(cols.isEmpty());
        // accesso negato
        AdminController nonAdmin = new AdminController(normalUser);
        assertThrows(IllegalArgumentException.class, nonAdmin::allCollections);
    }

    @Test
    void deleteCollection_positive_and_negative_and_auth() {
        // setup: crea collection
        collectionDAO.addCollection("CDel", "desc", normalUser);
        int colId = collectionDAO.getCollectionsByUser(normalUser.getId()).get(0).getId();
        // positivo: elimina
        adminController.deleteCollection(colId);
        assertTrue(collectionDAO.getCollectionsByUser(normalUser.getId()).isEmpty());
        // negativo: id inesistente
        assertDoesNotThrow(() -> adminController.deleteCollection(Integer.MAX_VALUE));
        // accesso negato
        collectionDAO.addCollection("CDel2", "desc", normalUser);
        int colId2 = collectionDAO.getCollectionsByUser(normalUser.getId()).get(0).getId();
        AdminController nonAdmin = new AdminController(normalUser);
        assertThrows(IllegalArgumentException.class, () -> nonAdmin.deleteCollection(colId2));
    }

    @Test
    void getCommentByAuthor_positive_and_negative_and_auth() {
        // setup: commento su doc
        documentDAO.addDocument(normalUser, "GC", "d", "2000", DocumentFormat.PDF, "fp", "fn", List.of("t"));
        int docId = documentDAO.getDocumentsByAuthor(normalUser.getId()).get(0).getId();
        commentDAO.addComment("Hi", normalUser.getId(), docId);
        // positivo
        List<Comment> comments = adminController.getCommentByAuthor(normalUser.getId());
        assertFalse(comments.isEmpty());
        // negativo: autore inesistente
        List<Comment> none = adminController.getCommentByAuthor(Integer.MAX_VALUE);
        assertNotNull(none);
        // accesso negato
        AdminController nonAdmin = new AdminController(normalUser);
        assertThrows(IllegalArgumentException.class, () -> nonAdmin.getCommentByAuthor(normalUser.getId()));
    }

    @Test
    void getCommentsByDocument_positive_and_negative_and_auth() {
        // setup: commento su doc
        documentDAO.addDocument(normalUser, "GCD", "d", "2000", DocumentFormat.PDF, "fp", "fn", List.of("t"));
        int docId = documentDAO.getDocumentsByAuthor(normalUser.getId()).get(0).getId();
        commentDAO.addComment("Hi", normalUser.getId(), docId);
        // positivo
        List<Comment> comments = adminController.getCommentsByDocument(docId);
        assertFalse(comments.isEmpty());
        // negativo: doc inesistente
        List<Comment> none = adminController.getCommentsByDocument(Integer.MAX_VALUE);
        assertNotNull(none);
        // accesso negato
        AdminController nonAdmin = new AdminController(normalUser);
        assertThrows(IllegalArgumentException.class, () -> nonAdmin.getCommentsByDocument(docId));
    }
}