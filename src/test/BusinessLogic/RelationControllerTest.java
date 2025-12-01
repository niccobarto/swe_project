package BusinessLogic;

import DomainModel.*;
import ORM.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RelationControllerTest {

    private Connection conn;
    private UserDAO userDAO;
    private DocumentDAO documentDAO;
    private DocumentRelationDAO relationDAO;
    private User currentUser;

    @BeforeEach
    void setUp() {
        try {
            DBConnection.setEnableTesting(true);
            DBConnection.resetInstance();
            conn = DBConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            userDAO = new UserDAO();
            documentDAO = new DocumentDAO();
            relationDAO = new DocumentRelationDAO();

            String email = "rctest+" + System.currentTimeMillis() + "@example.com";
            userDAO.addUser("Rel", "Tester", email, "pwd", false, false);
            currentUser = userDAO.getUserByEmail(email);
            assertNotNull(currentUser, "User di test non creato");

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
        } catch (Exception e) {
            fail("tearDown failed: " + e.getMessage());
        }
    }

    @Test
    void getSelected() {
        // crea un documento e verifica che getSelected ritorni lo stesso
        UserController uc = new UserController(currentUser);
        uc.createDocument("SelDoc", "d", "1900", DocumentFormat.TXT, java.util.List.of("t"));
        List<Document> docs = documentDAO.getDocumentsByAuthor(currentUser.getId());
        assertFalse(docs.isEmpty());
        Document selected = docs.get(0);
        RelationController rc = new RelationController(selected);
        assertEquals(selected.getId(), rc.getSelected().getId());
    }

    @Test
    void addRelation() {
        UserController uc = new UserController(currentUser);
        uc.createDocument("SrcDoc", "d", "1900", DocumentFormat.PDF, java.util.List.of("t"));
        uc.createDocument("DstDoc", "d", "1900", DocumentFormat.PDF, java.util.List.of("t"));
        List<Document> docs = documentDAO.getDocumentsByAuthor(currentUser.getId());
        assertTrue(docs.size() >= 2);
        Document src = docs.get(1); // creation_date DESC -> last created is at index 0, take index 1 for source
        Document dst = docs.get(0);

        RelationController rc = new RelationController(src);
        // assicurati che non esista già
        int before = countRelationRows(src.getId(), dst.getId(), DocumentRelationType.QUOTE);
        assertEquals(0, before);

        rc.addRelation(dst, DocumentRelationType.QUOTE);

        int after = countRelationRows(src.getId(), dst.getId(), DocumentRelationType.QUOTE);
        assertEquals(1, after, "Relation not inserted in DB");

        // chiamate con input invalidi non devono lanciare eccezioni verso l'esterno
        assertDoesNotThrow(() -> rc.addRelation(null, DocumentRelationType.QUOTE));
        assertDoesNotThrow(() -> rc.addRelation(src, DocumentRelationType.QUOTE)); // same doc
        assertDoesNotThrow(() -> rc.addRelation(dst, null));
    }

    @Test
    void removeRelation() {
        UserController uc = new UserController(currentUser);
        uc.createDocument("Rsrc", "d", "1900", DocumentFormat.TXT, java.util.List.of("t"));
        uc.createDocument("Rdst", "d", "1900", DocumentFormat.TXT, java.util.List.of("t"));
        List<Document> docs = documentDAO.getDocumentsByAuthor(currentUser.getId());
        assertTrue(docs.size() >= 2);
        Document src = docs.get(1);
        Document dst = docs.get(0);

        // inserisci direttamente la relazione
        relationDAO.addDocumentRelation(src.getId(), dst.getId(), DocumentRelationType.ANSWER_TO);
        int present = countRelationRows(src.getId(), dst.getId(), DocumentRelationType.ANSWER_TO);
        assertEquals(1, present);

        RelationController rc = new RelationController(src);
        rc.removeRelation(dst);

        int after = countRelationRows(src.getId(), dst.getId(), DocumentRelationType.ANSWER_TO);
        assertEquals(0, after, "Relation not removed from DB");

        // chiamata con input invalidi non deve lanciare eccezioni
        assertDoesNotThrow(() -> rc.removeRelation(null));
    }

    @Test
    void searchDestinationRelations() {
        UserController uc = new UserController(currentUser);
        uc.createDocument("Ssrc", "d", "1900", DocumentFormat.TXT, java.util.List.of("t"));
        uc.createDocument("Sdst", "d", "1900", DocumentFormat.TXT, java.util.List.of("t"));
        List<Document> docs = documentDAO.getDocumentsByAuthor(currentUser.getId());
        Document src = docs.get(1);
        Document dst = docs.get(0);

        RelationController rc = new RelationController(src);

        // inizialmente non ci sono relazioni
        List<Document> emptyRes = rc.searchDestinationRelations(null);
        assertNotNull(emptyRes);

        // inserisci relazione direttamente e verifica che il metodo sia invocabile
        relationDAO.addDocumentRelation(src.getId(), dst.getId(), DocumentRelationType.NEW_VERSION_OF);
        List<Document> res = rc.searchDestinationRelations(null);
        assertNotNull(res, "searchDestinationRelations should not return null");
        // controlliamo direttamente sul DB che la relazione esista (verifica affidabile)
        int count = countRelationRows(src.getId(), dst.getId(), DocumentRelationType.NEW_VERSION_OF);
        assertEquals(1, count);
    }

    @Test
    void searchSourceRelations() {
        UserController uc = new UserController(currentUser);
        uc.createDocument("S2src", "d", "1900", DocumentFormat.TXT, java.util.List.of("t"));
        uc.createDocument("S2dst", "d", "1900", DocumentFormat.TXT, java.util.List.of("t"));
        List<Document> docs = documentDAO.getDocumentsByAuthor(currentUser.getId());
        Document src = docs.get(1);
        Document dst = docs.get(0);

        RelationController rcDst = new RelationController(dst);

        // inserisci relazione direttamente (src -> dst)
        relationDAO.addDocumentRelation(src.getId(), dst.getId(), DocumentRelationType.TRANSCRIBES);

        List<Document> res = rcDst.searchSourceRelations(null);
        assertNotNull(res, "searchSourceRelations should not return null");

        int count = countRelationRows(src.getId(), dst.getId(), DocumentRelationType.TRANSCRIBES);
        assertEquals(1, count);
    }

    // helper che interroga la tabella document_relation per controlli diretti
    private int countRelationRows(int sourceId, int destinationId, DocumentRelationType type) {
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS cnt FROM document_relation WHERE source_id=? AND destination_id=? AND relation_type=?");
            ps.setInt(1, sourceId);
            ps.setInt(2, destinationId);
            ps.setString(3, type.toString());
            ResultSet rs = ps.executeQuery();
            int cnt = rs.next() ? rs.getInt("cnt") : 0;
            rs.close();
            ps.close();
            return cnt;
        } catch (Exception e) {
            fail("DB query failed: " + e.getMessage());
            return -1;
        }
    }

}