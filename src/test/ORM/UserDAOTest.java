package ORM;

import DomainModel.Collection;
import DomainModel.Document;
import DomainModel.DocumentFormat;
import DomainModel.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserDAOTest {

    private Connection conn;
    private UserDAO userDAO;
    private DocumentDAO documentDAO;
    private CollectionDAO collectionDAO;
    private User testUser;
    private int testDocId;
    private int testColId;

    @BeforeEach
    void setUp() {
        try {
            DBConnection.setEnableTesting(true);
            DBConnection.resetInstance();
            conn = DBConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            userDAO = new UserDAO();
            documentDAO = new DocumentDAO();
            collectionDAO = new CollectionDAO();

            String email = "usertest+" + System.currentTimeMillis() + "@example.com";
            userDAO.addUser("User", "Tester", email, "pwd", false, false);
            testUser = userDAO.getUserByEmail(email);
            assertNotNull(testUser, "Impossibile creare user di test");

            // crea un documento per l'utente
            String title = "DocUser-" + System.currentTimeMillis();
            boolean created = documentDAO.addDocument(testUser, title, "desc", "1900", DocumentFormat.TXT, "document/" + testUser.getId() + "/", "file_test", List.of());
            assertTrue(created, "Impossibile creare documento di test");
            testDocId = documentDAO.getDocumentsByAuthor(testUser.getId()).get(0).getId();

            // crea una collection per l'utente
            collectionDAO.addCollection("ColUser", "descCol", testUser);
            List<Collection> cols = collectionDAO.getCollectionsByUser(testUser.getId());
            assertFalse(cols.isEmpty(), "Impossibile creare collection di test");
            testColId = cols.get(0).getId();

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
    void addAndGetUser() {
        assertNotNull(testUser);
        assertTrue(testUser.getEmail().contains("@example.com"));
    }

    @Test
    void removeUser() {
        try {
            // crea un user temporaneo
            String email = "toremove+" + System.currentTimeMillis() + "@example.com";
            userDAO.addUser("To", "Remove", email, "pwd", false, false);
            User u = userDAO.getUserByEmail(email);
            assertNotNull(u);
            int id = u.getId();

            userDAO.removeUser(id);
            User after = userDAO.getUserById(id);
            assertNull(after, "User non rimosso correttamente");
        } catch (Exception e) {
            fail("Eccezione in removeUser: " + e.getMessage());
        }
    }

    @Test
    void updateNextFileName() {
        boolean updated = userDAO.updateNextFileName(testUser.getId(), 12345);
        assertTrue(updated, "updateNextFileName dovrebbe ritornare true");
        User u = userDAO.getUserById(testUser.getId());
        assertNotNull(u);
        assertEquals(12345, u.getNextFileName());
    }

    @Test
    void setModeratorAndGetModerators() {
        boolean ok = userDAO.setModerator(testUser.getId(), true);
        assertTrue(ok, "setModerator dovrebbe ritornare true");
        User u = userDAO.getUserById(testUser.getId());
        assertTrue(u.isModerator(), "User dovrebbe essere moderatore dopo setModerator(true)");

        List<User> mods = userDAO.getModerators();
        assertNotNull(mods);
        assertTrue(mods.stream().anyMatch(x -> x.getId() == testUser.getId()), "getModerators non contiene il moderatore appena impostato");
    }

    @Test
    void addAndRemoveFavouriteDocument() {
        // aggiungi
        userDAO.addFavouriteDocuments(testUser.getId(), testDocId);
        List<Document> favs = userDAO.getFavouriteDocument(testUser.getId());
        assertNotNull(favs);
        assertTrue(favs.stream().anyMatch(d -> d.getId() == testDocId), "Documento non presente nei preferiti dopo addFavouriteDocuments");

        // rimuovi
        userDAO.removeFavouriteDocuments(testUser.getId(), testDocId);
        List<Document> favsAfter = userDAO.getFavouriteDocument(testUser.getId());
        assertFalse(favsAfter.stream().anyMatch(d -> d.getId() == testDocId), "Documento ancora presente dopo removeFavouriteDocuments");
    }

    @Test
    void addAndRemoveFavouriteCollection() {
        try {
            // aggiungi
            userDAO.addFavouriteCollection(testUser.getId(), testColId);

            PreparedStatement ps = conn.prepareStatement("SELECT * FROM favourite_collection WHERE user_id = ? AND collection_id = ?");
            ps.setInt(1, testUser.getId());
            ps.setInt(2, testColId);
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next(), "Record non trovato in favourite_collection dopo addFavouriteCollection");
            rs.close();
            ps.close();

            // rimuovi
            userDAO.removeFavouriteCollection(testUser.getId(), testColId);
            PreparedStatement ps2 = conn.prepareStatement("SELECT * FROM favourite_collection WHERE user_id = ? AND collection_id = ?");
            ps2.setInt(1, testUser.getId());
            ps2.setInt(2, testColId);
            ResultSet rs2 = ps2.executeQuery();
            assertFalse(rs2.next(), "Record ancora presente in favourite_collection dopo removeFavouriteCollection");
            rs2.close();
            ps2.close();

        } catch (SQLException e) {
            fail("Eccezione in addAndRemoveFavouriteCollection: " + e.getMessage());
        }
    }

    @Test
    void getAllUsers() {
        List<User> all = userDAO.getAllUsers();
        assertNotNull(all);
        assertTrue(all.size() >= 1, "getAllUsers dovrebbe restituire almeno 1 utente");
    }

    @Test
    void getUserByEmail() {
        User u = userDAO.getUserByEmail(testUser.getEmail());
        assertNotNull(u);
        assertEquals(testUser.getId(), u.getId());
    }

}