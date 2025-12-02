package BusinessLogic;

import DomainModel.*;
import ORM.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.print.Doc;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
class UserControllerTest {

    private Connection conn;
    private UserDAO userDAO;
    private DocumentDAO documentDAO;
    private CommentDAO commentDAO;
    private CollectionDAO collectionDAO;
    private PublishRequestDAO publishRequestDAO;
    private User currentUser;
    private UserController controller;

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
            String email = "uctest+" + System.currentTimeMillis() + "@example.com";
            userDAO.addUser("Unit", "Tester", email, "pwd", false, false);
            currentUser = userDAO.getUserByEmail(email);
            assertNotNull(currentUser, "User di test non creato");
            controller = new UserController(currentUser);
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

    /*
     * searchDocuments
     * - caso: nessuna corrispondenza sul titolo
     * - caso: trova documento per titolo
     * - caso: trova documento per formato
     */
    @Test
    void searchDocuments() {
        // caso: nessuna corrispondenza sul titolo
        DocumentSearchCriteria criteria = new DocumentSearchCriteria();
        criteria.setDocumentTitle("no-match-title");
        ArrayList<Document> results = controller.searchDocuments(criteria);
        assertNotNull(results);
        assertTrue(results.isEmpty(), "La ricerca senza corrispondenze deve restituire lista vuota");

        // caso: trova documento per titolo
        String title = "Unique-" + System.currentTimeMillis();
        controller.createDocument(title, "desc", "1800", DocumentFormat.TXT, List.of("t1"));
        DocumentSearchCriteria c1 = new DocumentSearchCriteria();
        c1.setDocumentTitle(title);
        ArrayList<Document> results1 = controller.searchDocuments(c1);
        assertTrue(results1.stream().anyMatch(d -> title.equals(d.getTitle())));

        // caso: trova documento per formato
        controller.createDocument("PDFDoc", "desc", "1800", DocumentFormat.PDF, List.of("t1"));
        DocumentSearchCriteria c2 = new DocumentSearchCriteria();
        c2.setFormat(DocumentFormat.PDF);
        ArrayList<Document> results2 = controller.searchDocuments(c2);
        assertTrue(results2.stream().anyMatch(d -> d.getFormat() == DocumentFormat.PDF));
    }

    /*
     * writeComment
     * - caso: aggiunge commento su documento esistente
     * - caso: testo vuoto viene ignorato
     * - caso: documento inesistente viene ignorato
     */
    @Test
    void writeComment() {
        // aggiunge commento su documento esistente
        controller.createDocument("DocC", "desc", "2000", DocumentFormat.PDF, List.of("t"));
        List<Document> docs = documentDAO.getDocumentsByAuthor(currentUser.getId());
        assertFalse(docs.isEmpty());
        int docId = docs.get(0).getId();
        controller.writeComment(docId, "Hello");
        assertFalse(commentDAO.getCommentsByDocument(docId).isEmpty(), "Commento non aggiunto");

        // testo vuoto viene ignorato
        controller.createDocument("DocC2", "desc", "2000", DocumentFormat.PDF, List.of("t"));
        List<Document> doc= documentDAO.getDocumentsByAuthor(currentUser.getId());
        int docId2 = documentDAO.getDocumentsByAuthor(currentUser.getId()).get(0).getId();
        controller.writeComment(docId2, " ");
        assertTrue(commentDAO.getCommentsByDocument(docId2).isEmpty());

        // documento inesistente viene ignorato (nessuna eccezione)
        int nonExistingDocId = Integer.MAX_VALUE; // improbabile esista
        controller.writeComment(nonExistingDocId, "Hello");
        assertTrue(commentDAO.getCommentsByDocument(nonExistingDocId).isEmpty());
    }

    /*
     * createDocument
     * - caso: crea documento con titolo
     * - caso: incrementa nextFileName dell'utente
     * - caso: input non validi gestiti senza eccezioni
     */
    @Test
    void createDocument() {
        // crea documento con titolo
        controller.createDocument("MyDoc", "desc", "1900-1950", DocumentFormat.PDF, List.of("a", "b"));
        List<Document> docs = documentDAO.getDocumentsByAuthor(currentUser.getId());
        assertFalse(docs.isEmpty());
        assertEquals("MyDoc", docs.get(0).getTitle());

        // incrementa nextFileName
        int prev = currentUser.getNextFileName();
        controller.createDocument("T", "d", "1900", DocumentFormat.PDF, List.of("t"));
        assertEquals(prev + 1, currentUser.getNextFileName());

        // input non validi non devono lanciare eccezioni
        assertDoesNotThrow(() -> controller.createDocument(null, "d", "1900", DocumentFormat.PDF, List.of("t")));
        assertDoesNotThrow(() -> controller.createDocument("Doc", null, "1900", DocumentFormat.PDF, List.of("t")));
        assertDoesNotThrow(() -> controller.createDocument("Doc", "d", null, DocumentFormat.PDF, List.of("t")));
        assertDoesNotThrow(() -> controller.createDocument("Doc", "d", "1900", null, List.of("t")));
    }

    /*
     * viewOwnDocuments
     * - caso: inizialmente vuoto
     * - caso: dopo creazione contiene il documento
     */
    @Test
    void viewOwnDocuments() {
        ArrayList<Document> docs = controller.viewOwnDocuments();
        assertNotNull(docs);
        assertTrue(docs.isEmpty());

        controller.createDocument("Mine", "d", "1900", DocumentFormat.TXT, List.of("t"));
        ArrayList<Document> docs2 = controller.viewOwnDocuments();
        assertEquals(1, docs2.size());
        assertEquals("Mine", docs2.get(0).getTitle());
    }

    /*
     * addDocumentToCollection
     * - caso: aggiunge documento a collection posseduta
     * - caso: non aggiunge se la collection è di un altro utente
     * - caso: input invalidi gestiti senza eccezioni
     */
    @Test
    void addDocumentToCollection() {
        controller.createDocument("Dcol", "d", "1900", DocumentFormat.PDF, List.of("t"));
        int docId = documentDAO.getDocumentsByAuthor(currentUser.getId()).get(0).getId();
        collectionDAO.addCollection("C1", "desc", currentUser);
        int colId = collectionDAO.getCollectionsByUser(currentUser.getId()).get(0).getId();
        controller.addDocumentToCollection(docId, colId);
        boolean present = collectionDAO.getDocumentsByCollection(colId).stream().anyMatch(d -> d.getId() == docId);
        assertTrue(present, "Documento non inserito in collection posseduta");

        // not owned
        controller.createDocument("Dcol2", "d", "1900", DocumentFormat.PDF, List.of("t"));
        int docId2 = documentDAO.getDocumentsByAuthor(currentUser.getId()).get(0).getId();
        String otherEmail = "other+" + System.currentTimeMillis() + "@example.com";
        userDAO.addUser("Other", "User", otherEmail, "pwd", false, false);
        User other = userDAO.getUserByEmail(otherEmail);
        collectionDAO.addCollection("OtherC", "desc", other);
        int otherColId = collectionDAO.getCollectionsByUser(other.getId()).get(0).getId();
        controller.addDocumentToCollection(docId2, otherColId);
        boolean present2 = collectionDAO.getDocumentsByCollection(otherColId).stream().anyMatch(d -> d.getId() == docId2);
        assertFalse(present2);

        // invalid ids
        int invalidDocId = Integer.MAX_VALUE;
        int invalidColId = Integer.MAX_VALUE;
        assertDoesNotThrow(() -> controller.addDocumentToCollection(invalidDocId, invalidColId));
    }

    /*
     * removeDocumentToCollection
     * - caso: rimuove da collection posseduta
     * - caso: non rimuove da collection di un altro utente
     * - caso: input invalidi gestiti senza eccezioni
     */
    @Test
    void removeDocumentToCollection() {
        controller.createDocument("Dcol2", "d", "1900", DocumentFormat.PDF, List.of("t"));
        int docId = documentDAO.getDocumentsByAuthor(currentUser.getId()).get(0).getId();
        collectionDAO.addCollection("C2", "desc", currentUser);
        int colId = collectionDAO.getCollectionsByUser(currentUser.getId()).get(0).getId();
        collectionDAO.addDocumentToCollection(docId, colId);
        controller.removeDocumentToCollection(docId, colId);
        boolean present = collectionDAO.getDocumentsByCollection(colId).stream().anyMatch(d -> d.getId() == docId);
        assertFalse(present, "Documento non rimosso dalla collection");

        // not owned
        controller.createDocument("Dcol4", "d", "1900", DocumentFormat.PDF, List.of("t"));
        int docId2 = documentDAO.getDocumentsByAuthor(currentUser.getId()).get(0).getId();
        String otherEmail = "other+" + System.currentTimeMillis() + "@example.com";
        userDAO.addUser("Other", "User", otherEmail, "pwd", false, false);
        User other = userDAO.getUserByEmail(otherEmail);
        collectionDAO.addCollection("OtherC2", "desc", other);
        int otherColId = collectionDAO.getCollectionsByUser(other.getId()).get(0).getId();
        collectionDAO.addDocumentToCollection(docId2, otherColId);
        controller.removeDocumentToCollection(docId2, otherColId);
        boolean present3 = collectionDAO.getDocumentsByCollection(otherColId).stream().anyMatch(d -> d.getId() == docId2);
        assertTrue(present3);

        // invalid ids
        assertDoesNotThrow(() -> controller.removeDocumentToCollection(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    /*
     * askForPublication
     * - caso: crea richiesta per documento proprio
     * - caso: setta lo status del documento a PENDING
     * - caso: non crea duplicati
     * - caso: non crea richiesta per documenti non propri
     * - caso: input non esistenti gestiti senza eccezioni
     */
    @Test
    void askForPublication() {
        controller.createDocument("Pub1", "d", "1900", DocumentFormat.PDF, List.of("t"));
        int docId = documentDAO.getDocumentsByAuthor(currentUser.getId()).get(0).getId();
        controller.askForPublication(docId);
        boolean exists = publishRequestDAO.getRequestsByStatus(RequestStatus.PENDING)
                .stream().anyMatch(r -> r.getDocument() != null && r.getDocument().getId() == docId);
        assertTrue(exists, "Publish request non creata");

        // setta PENDING
        controller.createDocument("Pub2", "d", "1900", DocumentFormat.PDF, List.of("t"));
        int docId2 = documentDAO.getDocumentsByAuthor(currentUser.getId()).get(0).getId();
        controller.askForPublication(docId2);
        Document d = documentDAO.getDocumentById(docId2);
        assertEquals(DocumentStatus.PENDING, d.getStatus(), "Documento non settato a PENDING");

        // duplicato
        controller.createDocument("Pub3", "d", "1900", DocumentFormat.PDF, List.of("t"));
        int docId3 = documentDAO.getDocumentsByAuthor(currentUser.getId()).get(0).getId();
        controller.askForPublication(docId3);
        controller.askForPublication(docId3);
        long count = publishRequestDAO.getRequestsByStatus(RequestStatus.PENDING)
                .stream().filter(r -> r.getDocument() != null && r.getDocument().getId() == docId3).count();
        assertEquals(1, count);

        // non proprio
        String otherEmail = "other+" + System.currentTimeMillis() + "@example.com";
        userDAO.addUser("Other", "User", otherEmail, "pwd", false, false);
        User other = userDAO.getUserByEmail(otherEmail);
        UserController otherController = new UserController(other);
        otherController.createDocument("OtherDoc", "d", "1900", DocumentFormat.PDF, List.of("t"));
        int otherDocId = documentDAO.getDocumentsByAuthor(other.getId()).get(0).getId();
        controller.askForPublication(otherDocId);
        boolean existsOther = publishRequestDAO.getRequestsByStatus(RequestStatus.PENDING)
                .stream().anyMatch(r -> r.getDocument() != null && r.getDocument().getId() == otherDocId);
        assertFalse(existsOther);

        // non esistente
        assertDoesNotThrow(() -> controller.askForPublication(Integer.MAX_VALUE));
    }

    /*
     * favourites: documents
     * - caso: aggiunge e rimuove documenti dai preferiti
     * - caso: input invalidi gestiti senza eccezioni
     */
    @Test
    void favouritesDocuments() {
        controller.createDocument("FavD1", "d", "1900", DocumentFormat.PDF, List.of("t"));
        int docId = documentDAO.getDocumentsByAuthor(currentUser.getId()).get(0).getId();
        controller.addDocumentToFavourites(docId);
        boolean present = userDAO.getFavouriteDocument(currentUser.getId()).stream().anyMatch(d -> d.getId() == docId);
        assertTrue(present, "Documento non aggiunto ai preferiti");

        // invalid add
        assertDoesNotThrow(() -> controller.addDocumentToFavourites(Integer.MAX_VALUE));

        // remove
        controller.createDocument("FavD2", "d", "1900", DocumentFormat.PDF, List.of("t"));
        int docId2 = documentDAO.getDocumentsByAuthor(currentUser.getId()).get(0).getId();
        controller.addDocumentToFavourites(docId2);
        controller.removeDocumentFromFavourites(docId2);
        boolean present2 = userDAO.getFavouriteDocument(currentUser.getId()).stream().anyMatch(d -> d.getId() == docId2);
        assertFalse(present2, "Documento non rimosso dai preferiti");

        // invalid remove
        assertDoesNotThrow(() -> controller.removeDocumentFromFavourites(Integer.MAX_VALUE));
    }

    /*
     * favourites: collections
     * - caso: aggiunge e rimuove collection dai preferiti (verifiche dirette sul DB)
     * - caso: input invalidi gestiti senza eccezioni
     */
    @Test
    void favouritesCollections() {
        collectionDAO.addCollection("Cfav1", "desc", currentUser);
        int colId = collectionDAO.getCollectionsByUser(currentUser.getId()).get(0).getId();
        controller.addCollectionToFavourites(colId);
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS cnt FROM favourite_collection WHERE user_id=? AND collection_id=?");
            ps.setInt(1, currentUser.getId());
            ps.setInt(2, colId);
            ResultSet rs = ps.executeQuery();
            int cnt = rs.next() ? rs.getInt("cnt") : 0;
            rs.close();
            ps.close();
            assertTrue(cnt > 0, "Collection non aggiunta ai preferiti");
        } catch (SQLException e) {
            fail("Verifica favourite_collection fallita: " + e.getMessage());
        }

        // invalid add
        assertDoesNotThrow(() -> controller.addCollectionToFavourites(Integer.MAX_VALUE));

        // remove
        collectionDAO.addCollection("Cfav2", "desc", currentUser);
        int colId2 = collectionDAO.getCollectionsByUser(currentUser.getId()).get(0).getId();
        controller.addCollectionToFavourites(colId2);
        controller.removeCollectionFromFavourites(colId2);
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS cnt FROM favourite_collection WHERE user_id=? AND collection_id=?");
            ps.setInt(1, currentUser.getId());
            ps.setInt(2, colId2);
            ResultSet rs = ps.executeQuery();
            int cnt = rs.next() ? rs.getInt("cnt") : 0;
            rs.close();
            ps.close();
            assertEquals(0, cnt, "Collection non rimossa dai preferiti");
        } catch (SQLException e) {
            fail("Verifica favourite_collection fallita: " + e.getMessage());
        }

        // invalid remove
        assertDoesNotThrow(() -> controller.removeCollectionFromFavourites(Integer.MAX_VALUE));
    }

    @Test
    void requestAddExistingTag() {
        // Todo implementare
    }

    @Test
    void requestAddNewTag() {
        // Todo implementare
    }

    @Test
    void requestRemoveTag() {
        // Todo implementare
    }
}
