package ORM;

import DomainModel.Collection;
import DomainModel.DocumentFormat;
import DomainModel.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CollectionDAOTest {

    private Connection conn;
    private UserDAO userDAO;
    private DocumentDAO documentDAO;
    private CollectionDAO collectionDAO;
    private User testUser;
    private int testDocId;

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

            String email = "coltest+" + System.currentTimeMillis() + "@example.com";
            userDAO.addUser("Col", "Tester", email, "pwd", false, false);
            testUser = userDAO.getUserByEmail(email);
            assertNotNull(testUser, "Impossibile creare user di test");

            // crea un documento per l'utente
            String title = "DocCol-" + System.currentTimeMillis();
            boolean created = documentDAO.addDocument(testUser, title, "desc", "1900", DocumentFormat.TXT, "document/" + testUser.getId() + "/", "file_test", List.of());
            assertTrue(created, "Impossibile creare documento di test");
            testDocId = documentDAO.getDocumentsByAuthor(testUser.getId()).get(0).getId();

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
    void addCollection() {
        collectionDAO.addCollection("C1", "desc1", testUser);
        List<Collection> cols = collectionDAO.getCollectionsByUser(testUser.getId());
        assertNotNull(cols);
        assertFalse(cols.isEmpty(), "Collections vuote dopo addCollection");
        assertEquals("C1", cols.get(0).getName());
    }

    @Test
    void addAndRemoveDocumentToCollection() {
        collectionDAO.addCollection("C2", "desc2", testUser);
        int colId = collectionDAO.getCollectionsByUser(testUser.getId()).get(0).getId();

        // aggiungi documento alla collection
        collectionDAO.addDocumentToCollection(testDocId, colId);
        List<DomainModel.Document> docs = collectionDAO.getDocumentsByCollection(colId);
        assertNotNull(docs);
        assertTrue(docs.stream().anyMatch(d -> d.getId() == testDocId), "Documento non presente in collection dopo addDocumentToCollection");

        // remove document
        collectionDAO.removeDocumentFromCollection(testDocId, colId);
        List<DomainModel.Document> docsAfter = collectionDAO.getDocumentsByCollection(colId);
        assertFalse(docsAfter.stream().anyMatch(d -> d.getId() == testDocId), "Documento non rimosso dalla collection");
    }

    @Test
    void deleteCollection() {
        collectionDAO.addCollection("C3", "desc3", testUser);
        int colId = collectionDAO.getCollectionsByUser(testUser.getId()).get(0).getId();

        // aggiungi documento e favourite per verificare cascades in delete
        collectionDAO.addDocumentToCollection(testDocId, colId);
        userDAO.addFavouriteCollection(testUser.getId(), colId);

        // delete
        collectionDAO.deleteCollection(colId);

        Collection c = collectionDAO.getCollectionById(colId);
        assertNull(c, "Collection non eliminata correttamente");

        List<DomainModel.Document> docs = collectionDAO.getDocumentsByCollection(colId);
        assertTrue(docs.isEmpty(), "document_collection non svuotata dopo deleteCollection");
    }

    @Test
    void getAllCollectionsAndGetById() {
        // crea due collection
        collectionDAO.addCollection("All1", "d1", testUser);
        collectionDAO.addCollection("All2", "d2", testUser);

        List<Collection> all = collectionDAO.getAllCollections();
        assertNotNull(all);
        assertTrue(all.size() >= 2, "getAllCollections dovrebbe restituire almeno 2 elementi");

        int someId = collectionDAO.getCollectionsByUser(testUser.getId()).get(0).getId();
        Collection byId = collectionDAO.getCollectionById(someId);
        assertNotNull(byId);
        assertEquals(someId, byId.getId());
    }

}