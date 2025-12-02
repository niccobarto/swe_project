package BusinessLogic;

import DomainModel.*;
import ORM.*;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModeratorControllerTest {

    private Connection conn;
    private UserDAO userDAO;
    private DocumentDAO documentDAO;
    private PublishRequestDAO publishRequestDAO;

    private User adminUser;
    private User normalUser;
    private ModeratorController moderatorController;

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
            publishRequestDAO = new PublishRequestDAO();

            // Crea utenti (direttamente via DAO)
            String adminEmail = "mod+" + System.currentTimeMillis() + "@example.com";
            userDAO.addUser("Mod", "User", adminEmail, "pwd", true, true); // moderator AND admin (come richiede il controller attuale)
            adminUser = userDAO.getUserByEmail(adminEmail);
            assertNotNull(adminUser);

            String userEmail = "user+" + System.currentTimeMillis() + "@example.com";
            userDAO.addUser("Normal", "User", userEmail, "pwd", false, false);
            normalUser = userDAO.getUserByEmail(userEmail);
            assertNotNull(normalUser);

            moderatorController = new ModeratorController(adminUser);
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
    void updateDocumentStatus_positive_and_negative_and_auth() {
        // POSITIVO: crea doc + richiesta pendente, approva tramite moderatorController
        documentDAO.addDocument(normalUser, "P", "d", "2000", DocumentFormat.PDF, "fp", "fn", List.of("t"));
        int docId = documentDAO.getDocumentsByAuthor(normalUser.getId()).get(0).getId();
        publishRequestDAO.addRequest(documentDAO.getDocumentById(docId));

        moderatorController.updateDocumentStatus(docId, RequestStatus.APPROVED);

        // il documento deve essere pubblicato
        Document published = documentDAO.getDocumentById(docId);
        assertNotNull(published);
        assertEquals(DocumentStatus.PUBLISHED, published.getStatus());

        // la richiesta deve risultare APPROVED
        List<PublishRequest> approvedReqs = publishRequestDAO.getRequestsByStatus(RequestStatus.APPROVED);
        assertTrue(approvedReqs.stream().anyMatch(r -> r.getDocument() != null && r.getDocument().getId() == docId));

        // NEGATIVO: documento senza richiesta pendente -> non cambia lo status
        documentDAO.addDocument(normalUser, "NoPending", "d", "2001", DocumentFormat.TXT, "fp", "fn2", List.of("t"));
        int docNoPending = documentDAO.getDocumentsByAuthor(normalUser.getId()).get(0).getId();
        // non aggiungiamo publishRequest
        moderatorController.updateDocumentStatus(docNoPending, RequestStatus.APPROVED);
        Document notChanged = documentDAO.getDocumentById(docNoPending);
        assertNotNull(notChanged);
        assertEquals(DocumentStatus.DRAFT, notChanged.getStatus());

        // AUTH: utente non moderatore prova ad approvare -> non deve cambiare lo stato
        ModeratorController nonMod = new ModeratorController(normalUser);
        // crea doc+request per testare l'accesso negato
        documentDAO.addDocument(normalUser, "AuthTest", "d", "2002", DocumentFormat.PDF, "fp", "fn3", List.of("t"));
        int docAuth = documentDAO.getDocumentsByAuthor(normalUser.getId()).get(0).getId();
        publishRequestDAO.addRequest(documentDAO.getDocumentById(docAuth));
        nonMod.updateDocumentStatus(docAuth, RequestStatus.REJECTED);

        // essendo non moderatore lo status della richiesta rimane PENDING e il documento resta DRAFT
        List<PublishRequest> pending = publishRequestDAO.getRequestsByStatus(RequestStatus.PENDING);
        assertTrue(pending.stream().anyMatch(r -> r.getDocument() != null && r.getDocument().getId() == docAuth));
        assertEquals(DocumentStatus.PENDING, documentDAO.getDocumentById(docAuth).getStatus());
    }

    @Test
    void viewPendingDocumentRequests_positive_and_auth() {
        // setup: crea 2 documenti con richieste pendenti
        documentDAO.addDocument(normalUser, "VP1", "d", "1999", DocumentFormat.PDF, "fp", "f1", List.of("t"));
        int d1 = documentDAO.getDocumentsByAuthor(normalUser.getId()).get(0).getId();
        publishRequestDAO.addRequest(documentDAO.getDocumentById(d1));

        documentDAO.addDocument(normalUser, "VP2", "d", "2000", DocumentFormat.TXT, "fp", "f2", List.of("t"));
        int d2 = documentDAO.getDocumentsByAuthor(normalUser.getId()).get(0).getId();
        publishRequestDAO.addRequest(documentDAO.getDocumentById(d2));

        List<PublishRequest> pendings = moderatorController.viewPendingDocumentRequests();
        assertNotNull(pendings);
        assertTrue(pendings.size() >= 2);
        assertTrue(pendings.stream().anyMatch(r -> r.getDocument() != null && (r.getDocument().getId() == d1 || r.getDocument().getId() == d2)));

        // accesso negato: utente non moderatore ottiene lista vuota
        ModeratorController nonMod = new ModeratorController(normalUser);
        List<PublishRequest> none = nonMod.viewPendingDocumentRequests();
        assertNotNull(none);
        assertTrue(none.isEmpty());
    }

    @Test
    void decideTagRequest() {
        //Todo implementare
    }

    @Test
    void viewPendingTagRequests() {
        //Todo implementare
    }

    @Test
    void viewRequestHistory() {
        //Todo implementare
    }

    @Test
    void viewTagRequestHistory() {
        //Todo implementare
    }
}