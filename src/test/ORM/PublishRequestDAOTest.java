package ORM;

import DomainModel.DocumentFormat;
import DomainModel.Document;
import DomainModel.User;
import DomainModel.PublishRequest;
import DomainModel.RequestStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PublishRequestDAOTest {

    private Connection conn;
    private UserDAO userDAO;
    private DocumentDAO documentDAO;
    private PublishRequestDAO publishRequestDAO;
    private User author;
    private int docId;

    @BeforeEach
    void setUp() {
        try {
            DBConnection.setEnableTesting(true);
            DBConnection.resetInstance();
            conn = DBConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            userDAO = new UserDAO();
            documentDAO = new DocumentDAO();
            publishRequestDAO = new PublishRequestDAO();

            String email = "pubreq+" + System.currentTimeMillis() + "@example.com";
            userDAO.addUser("Pub", "Author", email, "pwd", false, false);
            author = userDAO.getUserByEmail(email);
            assertNotNull(author, "Impossibile creare author di test");

            // crea documento
            boolean created = documentDAO.addDocument(author, "Title PR", "desc", "1900", DocumentFormat.TXT, "document/" + author.getId() + "/", "file_pr", List.of());
            assertTrue(created, "Impossibile creare documento di test");
            docId = documentDAO.getDocumentsByAuthor(author.getId()).get(0).getId();

        } catch (SQLException e) {
            fail("setUp fallito: " + e.getMessage());
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
            fail("tearDown fallito: " + e.getMessage());
        }
    }

    @Test
    void addRequestAndGetPending() {
        Document d = documentDAO.getDocumentById(docId);
        assertNotNull(d);

        publishRequestDAO.addRequest(d);

        PublishRequest pr = publishRequestDAO.getPendingRequestByDocument(docId);
        assertNotNull(pr, "La publish request dovrebbe esistere dopo addRequest");
        assertEquals(RequestStatus.PENDING, pr.getStatus());

        List<PublishRequest> pending = publishRequestDAO.getRequestsByStatus(RequestStatus.PENDING);
        assertTrue(pending.stream().anyMatch(x -> x.getDocument().getId() == docId), "getRequestsByStatus non contiene la richiesta pending");
    }

    @Test
    void updateRequestStatusAndQueries() {
        // crea una request
        Document d = documentDAO.getDocumentById(docId);
        publishRequestDAO.addRequest(d);

        // crea un moderatore
        String emailMod = "mod+" + System.currentTimeMillis() + "@example.com";
        userDAO.addUser("Mod", "User", emailMod, "pwd", true, false);
        User mod = userDAO.getUserByEmail(emailMod);
        assertNotNull(mod);

        // aggiorna lo status
        publishRequestDAO.updateRequestStatus(docId, mod.getId(), RequestStatus.APPROVED);

        // ora la request dovrebbe essere in ACCEPTED e non più PENDING
        List<PublishRequest> accepted = publishRequestDAO.getRequestsByStatus(RequestStatus.APPROVED);
        assertTrue(accepted.stream().anyMatch(x -> x.getDocument().getId() == docId), "Request non presente in ACCEPTED dopo updateRequestStatus");

        List<PublishRequest> byMod = publishRequestDAO.getRequestByModerator(mod.getId());
        assertTrue(byMod.stream().anyMatch(x -> x.getDocument().getId() == docId), "getRequestByModerator non ritorna la richiesta aggiornata");

        PublishRequest pending = publishRequestDAO.getPendingRequestByDocument(docId);
        assertNull(pending, "getPendingRequestByDocument dovrebbe ritornare null dopo l'aggiornamento dello status");
    }

    @Test
    void removeRequest() {
        Document d = documentDAO.getDocumentById(docId);
        publishRequestDAO.addRequest(d);

        PublishRequest pr = publishRequestDAO.getPendingRequestByDocument(docId);
        assertNotNull(pr);

        publishRequestDAO.removeRequest(docId);

        PublishRequest after = publishRequestDAO.getPendingRequestByDocument(docId);
        assertNull(after, "La publish request dovrebbe essere stata rimossa");
    }

}