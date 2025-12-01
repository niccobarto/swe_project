package BusinessLogic;

import DomainModel.*;
import ORM.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    // searchDocuments
    @Test
    void searchDocuments_noMatchTitle_returnsEmpty() {
        DocumentSearchCriteria criteria = new DocumentSearchCriteria();
        criteria.setDocumentTitle("no-match-title");
        ArrayList<Document> results = controller.searchDocuments(criteria);
        assertNotNull(results);
        assertTrue(results.isEmpty(), "La ricerca senza corrispondenze deve restituire lista vuota");
    }

    @Test
    void searchDocuments_matchByTitle_findsDoc() {
        String title = "Unique-" + System.currentTimeMillis();
        controller.createDocument(title, "desc", "1800", DocumentFormat.TXT, List.of("t1"));
        DocumentSearchCriteria c = new DocumentSearchCriteria();
        c.setDocumentTitle(title);
        ArrayList<Document> results = controller.searchDocuments(c);
        assertTrue(results.stream().anyMatch(d -> title.equals(d.getTitle())));
    }

    @Test
    void searchDocuments_matchByFormat_findsDoc() {
        controller.createDocument("PDFDoc", "desc", "1800", DocumentFormat.PDF, List.of("t1"));
        DocumentSearchCriteria c = new DocumentSearchCriteria();
        c.setFormat(DocumentFormat.PDF);
        ArrayList<Document> results = controller.searchDocuments(c);
        assertTrue(results.stream().anyMatch(d -> d.getFormat() == DocumentFormat.PDF));
    }

    // writeComment
    @Test
    void writeComment_addsCommentOnExistingDoc() {
        controller.createDocument("DocC", "desc", "2000", DocumentFormat.PDF, List.of("t"));
        List<Document> docs = documentDAO.getDocumentsByAuthor(currentUser.getId());
        assertFalse(docs.isEmpty());
        int docId = docs.get(0).getId();
        controller.writeComment(docId, "Hello");
        assertFalse(commentDAO.getCommentsByDocument(docId).isEmpty(), "Commento non aggiunto");
    }

    @Test
    void writeComment_emptyText_isIgnored() {
        controller.createDocument("DocC2", "desc", "2000", DocumentFormat.PDF, List.of("t"));
        int docId = documentDAO.getDocumentsByAuthor(currentUser.getId()).get(0).getId();
        controller.writeComment(docId, " ");
        assertTrue(commentDAO.getCommentsByDocument(docId).isEmpty());
    }

    @Test
    void writeComment_nonExistingDoc_isIgnored() {
        int nonExistingDocId = Integer.MAX_VALUE; // improbabile esista
        controller.writeComment(nonExistingDocId, "Hello");
        // nessuna eccezione, nessun commento scritto ovviamente
        assertTrue(commentDAO.getCommentsByDocument(nonExistingDocId).isEmpty());
    }

    // createDocument
    @Test
    void createDocument_createsRecordWithTitle() {
        controller.createDocument("MyDoc", "desc", "1900-1950", DocumentFormat.PDF, List.of("a", "b"));
        List<Document> docs = documentDAO.getDocumentsByAuthor(currentUser.getId());
        assertFalse(docs.isEmpty());
        assertEquals("MyDoc", docs.get(0).getTitle());
    }

    @Test
    void createDocument_incrementsNextFileName() {
        int prev = currentUser.getNextFileName();
        controller.createDocument("T", "d", "1900", DocumentFormat.PDF, List.of("t"));
        assertEquals(prev + 1, currentUser.getNextFileName());
    }

    @Test
    void createDocument_invalidInputs_areHandledGracefully() {
        // titoli/descrizioni/periodi/format null o vuoti non dovrebbero lanciare eccezioni verso l'esterno
        assertDoesNotThrow(() -> controller.createDocument(null, "d", "1900", DocumentFormat.PDF, List.of("t")));
        assertDoesNotThrow(() -> controller.createDocument("Doc", null, "1900", DocumentFormat.PDF, List.of("t")));
        assertDoesNotThrow(() -> controller.createDocument("Doc", "d", null, DocumentFormat.PDF, List.of("t")));
        assertDoesNotThrow(() -> controller.createDocument("Doc", "d", "1900", null, List.of("t")));
    }

    // viewOwnDocuments
    @Test
    void viewOwnDocuments_initiallyEmpty() {
        ArrayList<Document> docs = controller.viewOwnDocuments();
        assertNotNull(docs);
        assertTrue(docs.isEmpty());
    }

    @Test
    void viewOwnDocuments_afterCreation_hasOneDoc() {
        controller.createDocument("Mine", "d", "1900", DocumentFormat.TXT, List.of("t"));
        ArrayList<Document> docs = controller.viewOwnDocuments();
        assertEquals(1, docs.size());
        assertEquals("Mine", docs.get(0).getTitle());
    }

    // addDocumentToCollection
    @Test
    void addDocumentToCollection_ownedCollection_addsDoc() {
        controller.createDocument("Dcol", "d", "1900", DocumentFormat.PDF, List.of("t"));
        int docId = documentDAO.getDocumentsByAuthor(currentUser.getId()).get(0).getId();
        collectionDAO.addCollection("C1", "desc", currentUser);
        int colId = collectionDAO.getCollectionsByUser(currentUser.getId()).get(0).getId();
        controller.addDocumentToCollection(docId, colId);
        boolean present = collectionDAO.getDocumentsByCollection(colId).stream().anyMatch(d -> d.getId() == docId);
        assertTrue(present, "Documento non inserito in collection posseduta");
    }

    @Test
    void addDocumentToCollection_notOwnedCollection_doesNothing() {
        controller.createDocument("Dcol2", "d", "1900", DocumentFormat.PDF, List.of("t"));
        int docId = documentDAO.getDocumentsByAuthor(currentUser.getId()).get(0).getId();
        String otherEmail = "other+" + System.currentTimeMillis() + "@example.com";
        userDAO.addUser("Other", "User", otherEmail, "pwd", false, false);
        User other = userDAO.getUserByEmail(otherEmail);
        collectionDAO.addCollection("OtherC", "desc", other);
        int otherColId = collectionDAO.getCollectionsByUser(other.getId()).get(0).getId();
        controller.addDocumentToCollection(docId, otherColId);
        boolean present = collectionDAO.getDocumentsByCollection(otherColId).stream().anyMatch(d -> d.getId() == docId);
        assertFalse(present);
    }

    @Test
    void addDocumentToCollection_invalidIds_areHandled() {
        int invalidDocId = Integer.MAX_VALUE;
        int invalidColId = Integer.MAX_VALUE;
        assertDoesNotThrow(() -> controller.addDocumentToCollection(invalidDocId, invalidColId));
    }

    // removeDocumentToCollection
    @Test
    void removeDocumentToCollection_ownedCollection_removesDoc() {
        controller.createDocument("Dcol2", "d", "1900", DocumentFormat.PDF, List.of("t"));
        int docId = documentDAO.getDocumentsByAuthor(currentUser.getId()).get(0).getId();
        collectionDAO.addCollection("C2", "desc", currentUser);
        int colId = collectionDAO.getCollectionsByUser(currentUser.getId()).get(0).getId();
        collectionDAO.addDocumentToCollection(docId, colId);
        controller.removeDocumentToCollection(docId, colId);
        boolean present = collectionDAO.getDocumentsByCollection(colId).stream().anyMatch(d -> d.getId() == docId);
        assertFalse(present, "Documento non rimosso dalla collection");
    }

    @Test
    void removeDocumentToCollection_notOwnedCollection_doesNothing() {
        controller.createDocument("Dcol4", "d", "1900", DocumentFormat.PDF, List.of("t"));
        int docId = documentDAO.getDocumentsByAuthor(currentUser.getId()).get(0).getId();
        String otherEmail = "other+" + System.currentTimeMillis() + "@example.com";
        userDAO.addUser("Other", "User", otherEmail, "pwd", false, false);
        User other = userDAO.getUserByEmail(otherEmail);
        collectionDAO.addCollection("OtherC2", "desc", other);
        int otherColId = collectionDAO.getCollectionsByUser(other.getId()).get(0).getId();
        collectionDAO.addDocumentToCollection(docId, otherColId);
        controller.removeDocumentToCollection(docId, otherColId);
        boolean present = collectionDAO.getDocumentsByCollection(otherColId).stream().anyMatch(d -> d.getId() == docId);
        assertTrue(present);
    }

    @Test
    void removeDocumentToCollection_invalidIds_areHandled() {
        assertDoesNotThrow(() -> controller.removeDocumentToCollection(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    // askForPublication
    @Test
    void askForPublication_ownDoc_createsRequest() {
        controller.createDocument("Pub1", "d", "1900", DocumentFormat.PDF, List.of("t"));
        int docId = documentDAO.getDocumentsByAuthor(currentUser.getId()).get(0).getId();
        controller.askForPublication(docId);
        boolean exists = publishRequestDAO.getRequestsByStatus(RequestStatus.PENDING)
                .stream().anyMatch(r -> r.getDocument() != null && r.getDocument().getId() == docId);
        assertTrue(exists, "Publish request non creata");
    }

    @Test
    void askForPublication_setsDocumentPending() {
        controller.createDocument("Pub2", "d", "1900", DocumentFormat.PDF, List.of("t"));
        int docId = documentDAO.getDocumentsByAuthor(currentUser.getId()).get(0).getId();
        controller.askForPublication(docId);
        Document d = documentDAO.getDocumentById(docId);
        assertEquals(DocumentStatus.PENDING, d.getStatus(), "Documento non settato a PENDING");
    }

    @Test
    void askForPublication_duplicateRequest_notCreated() {
        controller.createDocument("Pub3", "d", "1900", DocumentFormat.PDF, List.of("t"));
        int docId = documentDAO.getDocumentsByAuthor(currentUser.getId()).get(0).getId();
        controller.askForPublication(docId);
        controller.askForPublication(docId);
        long count = publishRequestDAO.getRequestsByStatus(RequestStatus.PENDING)
                .stream().filter(r -> r.getDocument() != null && r.getDocument().getId() == docId).count();
        assertEquals(1, count);
    }

    @Test
    void askForPublication_notOwnedDoc_isIgnored() {
        String otherEmail = "other+" + System.currentTimeMillis() + "@example.com";
        userDAO.addUser("Other", "User", otherEmail, "pwd", false, false);
        User other = userDAO.getUserByEmail(otherEmail);
        UserController otherController = new UserController(other);
        otherController.createDocument("OtherDoc", "d", "1900", DocumentFormat.PDF, List.of("t"));
        int otherDocId = documentDAO.getDocumentsByAuthor(other.getId()).get(0).getId();
        controller.askForPublication(otherDocId);
        boolean exists = publishRequestDAO.getRequestsByStatus(RequestStatus.PENDING)
                .stream().anyMatch(r -> r.getDocument() != null && r.getDocument().getId() == otherDocId);
        assertFalse(exists);
    }

    @Test
    void askForPublication_nonExistingDoc_isIgnored() {
        assertDoesNotThrow(() -> controller.askForPublication(Integer.MAX_VALUE));
    }

    // favourites: documents
    @Test
    void addDocumentToFavourites_insertsRow() {
        controller.createDocument("FavD1", "d", "1900", DocumentFormat.PDF, List.of("t"));
        int docId = documentDAO.getDocumentsByAuthor(currentUser.getId()).get(0).getId();
        controller.addDocumentToFavourites(docId);
        boolean present = userDAO.getFavouriteDocument(currentUser.getId()).stream().anyMatch(d -> d.getId() == docId);
        assertTrue(present, "Documento non aggiunto ai preferiti");
    }

    @Test
    void addDocumentToFavourites_invalidDocId_isHandled() {
        assertDoesNotThrow(() -> controller.addDocumentToFavourites(Integer.MAX_VALUE));
    }

    @Test
    void removeDocumentFromFavourites_deletesRow() {
        controller.createDocument("FavD2", "d", "1900", DocumentFormat.PDF, List.of("t"));
        int docId = documentDAO.getDocumentsByAuthor(currentUser.getId()).get(0).getId();
        controller.addDocumentToFavourites(docId);
        controller.removeDocumentFromFavourites(docId);
        boolean present = userDAO.getFavouriteDocument(currentUser.getId()).stream().anyMatch(d -> d.getId() == docId);
        assertFalse(present, "Documento non rimosso dai preferiti");
    }

    @Test
    void removeDocumentFromFavourites_invalidDocId_isHandled() {
        assertDoesNotThrow(() -> controller.removeDocumentFromFavourites(Integer.MAX_VALUE));
    }

    // favourites: collections
    @Test
    void addCollectionToFavourites_insertsRow() {
        collectionDAO.addCollection("Cfav1", "desc", currentUser);
        int colId = collectionDAO.getCollectionsByUser(currentUser.getId()).get(0).getId();
        controller.addCollectionToFavourites(colId);
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS cnt FROM favourite_collections WHERE user_id=? AND collection_id=?");
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
    }

    @Test
    void addCollectionToFavourites_invalidCollectionId_isHandled() {
        assertDoesNotThrow(() -> controller.addCollectionToFavourites(Integer.MAX_VALUE));
    }

    @Test
    void removeCollectionFromFavourites_deletesRow() {
        collectionDAO.addCollection("Cfav2", "desc", currentUser);
        int colId = collectionDAO.getCollectionsByUser(currentUser.getId()).get(0).getId();
        controller.addCollectionToFavourites(colId);
        controller.removeCollectionFromFavourites(colId);
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS cnt FROM favourite_collections WHERE user_id=? AND collection_id=?");
            ps.setInt(1, currentUser.getId());
            ps.setInt(2, colId);
            ResultSet rs = ps.executeQuery();
            int cnt = rs.next() ? rs.getInt("cnt") : 0;
            rs.close();
            ps.close();
            assertEquals(0, cnt, "Collection non rimossa dai preferiti");
        } catch (SQLException e) {
            fail("Verifica favourite_collection fallita: " + e.getMessage());
        }
    }

    @Test
    void removeCollectionFromFavourites_invalidCollectionId_isHandled() {
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

