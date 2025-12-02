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
    void searchUserById() {
        // Caso positivo: utente esistente deve essere trovato
        User found = adminController.searchUserById(normalUser.getId());
        assertNotNull(found);
        assertEquals(normalUser.getEmail(), found.getEmail());
        // Caso negativo: id inesistente deve restituire null
        assertNull(adminController.searchUserById(Integer.MAX_VALUE));
        // Caso autorizzazione: se chi invoca non è admin viene sollevata IllegalArgumentException
        AdminController nonAdmin = new AdminController(normalUser);
        assertThrows(IllegalArgumentException.class, () -> nonAdmin.searchUserById(normalUser.getId()));
    }

    @Test
    void removeUser() {
        // Caso positivo: admin rimuove utente
        adminController.removeUser(normalUser.getId());
        assertNull(userDAO.getUserById(normalUser.getId()));
        // Caso negativo: id inesistente non deve lanciare eccezioni
        assertDoesNotThrow(() -> adminController.removeUser(Integer.MAX_VALUE));
        // Caso autorizzazione: utente non admin non può rimuovere admin
        AdminController nonAdmin = new AdminController(normalUser);
        assertThrows(IllegalArgumentException.class, () -> nonAdmin.removeUser(adminUser.getId()));
    }

    @Test
    void searchUserByEmail() {
        // Positivo: trova l'utente per email
        User u = adminController.searchUserByEmail(normalUser.getEmail());
        assertNotNull(u);
        assertEquals(normalUser.getId(), u.getId());
        // Negativo: email inesistente -> null
        assertNull(adminController.searchUserByEmail("nope+" + System.currentTimeMillis() + "@x"));
        // Accesso negato: non-admin non può eseguire la ricerca
        AdminController nonAdmin = new AdminController(normalUser);
        assertThrows(IllegalArgumentException.class, () -> nonAdmin.searchUserByEmail(normalUser.getEmail()));
    }

    @Test
    void allUsers() {
        // Positivo: l'admin ottiene la lista di utenti
        List<User> users = adminController.allUsers();
        assertNotNull(users);
        assertTrue(users.size() >= 2);
        // Accesso negato: non-admin non deve poter chiamare il metodo
        AdminController nonAdmin = new AdminController(normalUser);
        assertThrows(IllegalArgumentException.class, nonAdmin::allUsers);
    }

    @Test
    void allDocuments() {
        // Setup: crea un documento per normalUser direttamente via DAO
        documentDAO.addDocument(normalUser, "A", "d", "1900", DocumentFormat.PDF, "fp", "fname", List.of("t"));
        // Positivo: admin ottiene tutti i documenti
        List<Document> docs = adminController.allDocuments();
        assertNotNull(docs);
        assertFalse(docs.isEmpty());
        // Accesso negato: non-admin non può ottenere tutti i documenti
        AdminController nonAdmin = new AdminController(normalUser);
        assertThrows(IllegalArgumentException.class, nonAdmin::allDocuments);
    }

    @Test
    void documentsByAuthor() {
        // Setup: crea due documenti per normalUser
        documentDAO.addDocument(normalUser, "A1", "d", "1900", DocumentFormat.TXT, "fp", "f1", List.of("t"));
        documentDAO.addDocument(normalUser, "A2", "d", "1901", DocumentFormat.PDF, "fp", "f2", List.of("t"));
        // Positivo: trova i documenti dell'autore
        List<Document> docs = adminController.documentsByAuthor(normalUser.getId());
        assertNotNull(docs);
        assertTrue(docs.size() >= 2);
        // Negativo: autore inesistente -> lista vuota
        List<Document> none = adminController.documentsByAuthor(Integer.MAX_VALUE);
        assertNotNull(none);
        // Accesso negato
        AdminController nonAdmin = new AdminController(normalUser);
        assertThrows(IllegalArgumentException.class, () -> nonAdmin.documentsByAuthor(normalUser.getId()));
    }

    @Test
    void setModerator() {
        // Positivo: setta il flag moderator su true
        adminController.setModerator(normalUser.getId(), true);
        User updated = userDAO.getUserById(normalUser.getId());
        assertTrue(updated.isModerator());
        // Negativo: id inesistente non deve lanciare eccezioni
        assertDoesNotThrow(() -> adminController.setModerator(Integer.MAX_VALUE, true));
        // Accesso negato
        AdminController nonAdmin = new AdminController(normalUser);
        assertThrows(IllegalArgumentException.class, () -> nonAdmin.setModerator(adminUser.getId(), false));
    }

    @Test
    void removeComment() {
        // Setup: inserisci doc+comment direttamente via DAO
        documentDAO.addDocument(normalUser, "C", "d", "2000", DocumentFormat.PDF, "fp", "fn", List.of("t"));
        int docId = documentDAO.getDocumentsByAuthor(normalUser.getId()).get(0).getId();
        commentDAO.addComment("Hi", normalUser.getId(), docId);
        int commentId = commentDAO.getCommentsByDocument(docId).get(0).getId();
        // Positivo: admin rimuove commento
        adminController.removeComment(commentId);
        assertTrue(commentDAO.getCommentsByDocument(docId).isEmpty());
        // Negativo: id inesistente non deve lanciare eccezioni
        assertDoesNotThrow(() -> adminController.removeComment(Integer.MAX_VALUE));
        // Accesso negato: non-admin non può rimuovere commenti
        AdminController nonAdmin = new AdminController(normalUser);
        // ricrea un commento per testare accesso negato
        commentDAO.addComment("Hi2", normalUser.getId(), docId);
        int anotherCommentId = commentDAO.getCommentsByDocument(docId).get(0).getId();
        assertThrows(IllegalArgumentException.class, () -> nonAdmin.removeComment(anotherCommentId));
    }

    @Test
    void documentsApprovedByModerator() {
        // setup: crea doc e publish request APPROVED direttamente via DAO
        documentDAO.addDocument(normalUser, "P", "d", "2000", DocumentFormat.PDF, "fp", "fn", List.of("t"));
        int docId = documentDAO.getDocumentsByAuthor(normalUser.getId()).get(0).getId();
        publishRequestDAO.addRequest(documentDAO.getDocumentById(docId));
        publishRequestDAO.updateRequestStatus(docId, adminUser.getId(), RequestStatus.APPROVED);
        // Positivo: lista contiene il doc approvato del moderator specificato (adminUser)
        List<Document> approved = adminController.documentsApprovedByModerator(adminUser.getId());
        assertTrue(approved.stream().anyMatch(d -> d.getId() == docId));
        // Negativo: moderator inesistente -> lista vuota
        List<Document> none = adminController.documentsApprovedByModerator(Integer.MAX_VALUE);
        assertNotNull(none);
        // Accesso negato
        AdminController nonAdmin = new AdminController(normalUser);
        assertThrows(IllegalArgumentException.class, () -> nonAdmin.documentsApprovedByModerator(adminUser.getId()));
    }

    @Test
    void documentsByStatus() {
        // setup: due doc, uno PENDING e uno DRAFT
        documentDAO.addDocument(normalUser, "S1", "d", "2000", DocumentFormat.PDF, "fp", "f1", List.of("t"));
        int d1 = documentDAO.getDocumentsByAuthor(normalUser.getId()).get(0).getId();
        documentDAO.updateDocumentStatus(d1, DocumentStatus.PENDING);
        documentDAO.addDocument(normalUser, "S2", "d2", "2001", DocumentFormat.TXT, "fp", "f2", List.of("t"));
        // Positivo: trova almeno S1
        List<Document> pending = adminController.documentsByStatus(DocumentStatus.PENDING);
        assertTrue(pending.stream().anyMatch(d -> d.getId() == d1));
        // Negativo: status senza doc (es. REJECTED)
        List<Document> rejected = adminController.documentsByStatus(DocumentStatus.REJECTED);
        assertNotNull(rejected);
        // Accesso negato
        AdminController nonAdmin = new AdminController(normalUser);
        assertThrows(IllegalArgumentException.class, () -> nonAdmin.documentsByStatus(DocumentStatus.PENDING));
    }

    @Test
    void allCollections() {
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
    void deleteCollection() {
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
    void getCommentByAuthor() {
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
    void getCommentsByDocument() {
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