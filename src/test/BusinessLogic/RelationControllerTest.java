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

    /*
     * selected
     * - verifica che getSelected ritorni il documento passato al controller
     */
    @Test
    void selected() {
        UserController uc = new UserController(currentUser);
        uc.createDocument("SelDoc", "d", "1900", DocumentFormat.TXT, java.util.List.of("t"));
        List<Document> docs = documentDAO.getDocumentsByAuthor(currentUser.getId());
        assertFalse(docs.isEmpty());
        Document selected = docs.get(0);
        RelationController rc = new RelationController(selected);
        assertEquals(selected.getId(), rc.getSelected().getId());
    }

    /*
     * addRelation
     * - caso: inserimento valido crea la relazione
     * - caso: input invalidi (null, stesso doc, tipo null) non devono lanciare eccezioni
     * - caso: chiamate invalide non devono duplicare record
     */
    @Test
    void addRelation() {
        UserController uc = new UserController(currentUser);
        uc.createDocument("SrcDoc", "d", "1900", DocumentFormat.PDF, java.util.List.of("t"));
        uc.createDocument("DstDoc", "d", "1900", DocumentFormat.PDF, java.util.List.of("t"));
        List<Document> docs = documentDAO.getDocumentsByAuthor(currentUser.getId());
        assertTrue(docs.size() >= 2);
        Document src = docs.get(1);
        Document dst = docs.get(0);

        RelationController rc = new RelationController(src);
        // assicurati che non esista già
        int before = countRelationRows(src.getId(), dst.getId(), DocumentRelationType.QUOTE);
        assertEquals(0, before);

        rc.addRelation(dst, DocumentRelationType.QUOTE);

        int after = countRelationRows(src.getId(), dst.getId(), DocumentRelationType.QUOTE);
        assertEquals(1, after, "Relation not inserted in DB");

        // input invalidi: non devono lanciare eccezioni
        assertDoesNotThrow(() -> rc.addRelation(null, DocumentRelationType.QUOTE));
        assertDoesNotThrow(() -> rc.addRelation(src, DocumentRelationType.QUOTE)); // same doc
        assertDoesNotThrow(() -> rc.addRelation(dst, null));

        // verifico che gli input invalidi non abbiano creato duplicati
        int afterInvalidCalls = countRelationRows(src.getId(), dst.getId(), DocumentRelationType.QUOTE);
        assertEquals(1, afterInvalidCalls, "Invalid calls should not have inserted additional rows");
    }

    /*
     * removeRelation
     * - caso: rimozione valida elimina la relazione
     * - caso: input invalidi non devono lanciare eccezioni né modificare il DB
     */
    @Test
    void removeRelation() {
        UserController uc = new UserController(currentUser);
        uc.createDocument("Rsrc", "d", "1900", DocumentFormat.TXT, java.util.List.of("t"));
        uc.createDocument("Rdst", "d", "1900", DocumentFormat.TXT, java.util.List.of("t"));
        List<Document> docs = documentDAO.getDocumentsByAuthor(currentUser.getId());
        assertTrue(docs.size() >= 2);
        Document src = docs.get(1);
        Document dst = docs.get(0);

        // inserisci direttamente la relazione (aggiunto il parametro sameAuthor)
        relationDAO.addDocumentRelation(src.getId(), dst.getId(), DocumentRelationType.ANSWER_TO,
                src.getAuthor().getId() == dst.getAuthor().getId());
        int present = countRelationRows(src.getId(), dst.getId(), DocumentRelationType.ANSWER_TO);
        assertEquals(1, present);

        RelationController rc = new RelationController(src);
        rc.removeRelation(dst);

        int after = countRelationRows(src.getId(), dst.getId(), DocumentRelationType.ANSWER_TO);
        assertEquals(0, after, "Relation not removed from DB");

        // chiamata con input invalidi non deve lanciare eccezioni e non deve modificare lo stato
        assertDoesNotThrow(() -> rc.removeRelation(null));
        int afterInvalid = countRelationRows(src.getId(), dst.getId(), DocumentRelationType.ANSWER_TO);
        assertEquals(0, afterInvalid, "Invalid remove should not change DB");
    }

    /*
     * searchDestination
     * - caso: ricerca destinazioni (con e senza filtro tipo)
     * - verifica che la relazione inserita sia restituita correttamente
     */
    @Test
    void searchDestination() {
        UserController uc = new UserController(currentUser);
        uc.createDocument("Ssrc", "d", "1900", DocumentFormat.TXT, java.util.List.of("t"));
        uc.createDocument("Sdst", "d", "1900", DocumentFormat.TXT, java.util.List.of("t"));
        List<Document> docs = documentDAO.getDocumentsByAuthor(currentUser.getId());
        Document src = docs.get(1);
        Document dst = docs.get(0);

        // RelationController deve essere creato sul documento destinazione per searchDestinationRelations
        RelationController rc = new RelationController(dst);

        // inizialmente non ci sono relazioni
        List<DocumentRelation> emptyRes = rc.searchDestinationRelations(null);
        assertNotNull(emptyRes);

        // inserisci relazione direttamente (src -> dst) e verifica che il metodo sia invocabile
        relationDAO.addDocumentRelation(src.getId(), dst.getId(), DocumentRelationType.NEW_VERSION_OF,
                src.getAuthor().getId() == dst.getAuthor().getId());
        List<DocumentRelation> res = rc.searchDestinationRelations(null);
        assertNotNull(res, "searchDestinationRelations should not return null");
        // filtraggio per tipo specifico
        List<DocumentRelation> resType = rc.searchDestinationRelations(DocumentRelationType.NEW_VERSION_OF);
        assertNotNull(resType);
        assertTrue(resType.stream().anyMatch(r -> r.getSource() != null && r.getSource().getId() == src.getId()));

        List<DocumentRelation> resDifferentType = rc.searchDestinationRelations(DocumentRelationType.TRANSCRIBES);
        assertNotNull(resDifferentType);
        assertTrue(resDifferentType.isEmpty());

        // controlliamo direttamente sul DB che la relazione esista (verifica affidabile)
        int count = countRelationRows(src.getId(), dst.getId(), DocumentRelationType.NEW_VERSION_OF);
        assertEquals(1, count);
    }

    /*
     * searchSource
     * - caso: ricerca sorgenti (con e senza filtro tipo)
     * - verifica che la relazione inserita sia restituita correttamente
     */
    @Test
    void searchSource() {
        UserController uc = new UserController(currentUser);
        uc.createDocument("S2src", "d", "1900", DocumentFormat.TXT, java.util.List.of("t"));
        uc.createDocument("S2dst", "d", "1900", DocumentFormat.TXT, java.util.List.of("t"));
        List<Document> docs = documentDAO.getDocumentsByAuthor(currentUser.getId());
        Document src = docs.get(1);
        Document dst = docs.get(0);

        // RelationController deve essere creato sul documento sorgente per searchSourceRelations
        RelationController rcSrc = new RelationController(src);

        // inserisci relazione direttamente (src -> dst) con sameAuthor
        relationDAO.addDocumentRelation(src.getId(), dst.getId(), DocumentRelationType.TRANSCRIBES,
                src.getAuthor().getId() == dst.getAuthor().getId());

        List<DocumentRelation> res = rcSrc.searchSourceRelations(null);
        assertNotNull(res, "searchSourceRelations should not return null");
        assertTrue(res.stream().anyMatch(r -> r.getDestination() != null && r.getDestination().getId() == dst.getId()));

        List<DocumentRelation> resType = rcSrc.searchSourceRelations(DocumentRelationType.TRANSCRIBES);
        assertNotNull(resType);
        assertTrue(resType.stream().anyMatch(r -> r.getDestination() != null && r.getDestination().getId() == dst.getId()));

        int count = countRelationRows(src.getId(), dst.getId(), DocumentRelationType.TRANSCRIBES);
        assertEquals(1, count);
    }

    /*
     * updateType
     * - caso: aggiorna il tipo di relazione esistente
     * - caso: input invalidi non devono lanciare eccezioni né alterare il DB
     */
    @Test
    void updateType() {
        UserController uc = new UserController(currentUser);
        uc.createDocument("Usrc", "d", "1900", DocumentFormat.TXT, java.util.List.of("t"));
        uc.createDocument("Udst", "d", "1900", DocumentFormat.TXT, java.util.List.of("t"));
        List<Document> docs = documentDAO.getDocumentsByAuthor(currentUser.getId());
        Document src = docs.get(1);
        Document dst = docs.get(0);

        // aggiungi relazione iniziale
        relationDAO.addDocumentRelation(src.getId(), dst.getId(), DocumentRelationType.QUOTE,
                src.getAuthor().getId() == dst.getAuthor().getId());
        assertEquals(1, countRelationRows(src.getId(), dst.getId(), DocumentRelationType.QUOTE));

        RelationController rc = new RelationController(src);
        // cambia tipo
        rc.updateRelationType(dst, DocumentRelationType.NEW_VERSION_OF);

        // verifica cambio tipo nel DB
        assertEquals(0, countRelationRows(src.getId(), dst.getId(), DocumentRelationType.QUOTE));
        assertEquals(1, countRelationRows(src.getId(), dst.getId(), DocumentRelationType.NEW_VERSION_OF));

        // invalid inputs non devono propagare eccezioni
        assertDoesNotThrow(() -> rc.updateRelationType(null, DocumentRelationType.QUOTE));
        assertDoesNotThrow(() -> rc.updateRelationType(src, DocumentRelationType.QUOTE)); // same doc
        assertDoesNotThrow(() -> rc.updateRelationType(dst, null));

        // assicurati che le chiamate invalide non abbiano creato altre relazioni
        assertEquals(1, countRelationRows(src.getId(), dst.getId(), DocumentRelationType.NEW_VERSION_OF));
    }

    /*
     * setConfirmed
     * - caso: impostare confirmed=true aggiorna la relazione
     * - caso: impostare confirmed=false rimuove la relazione
     * - caso: input null non deve lanciare eccezioni
     */
    @Test
    void setConfirmed() {
        UserController uc = new UserController(currentUser);
        uc.createDocument("Csrc", "d", "1900", DocumentFormat.TXT, java.util.List.of("t"));
        uc.createDocument("Cdst", "d", "1900", DocumentFormat.TXT, java.util.List.of("t"));
        List<Document> docs = documentDAO.getDocumentsByAuthor(currentUser.getId());
        Document src = docs.get(1);
        Document dst = docs.get(0);

        // inserisci relazione non confermata (assunto default)
        relationDAO.addDocumentRelation(src.getId(), dst.getId(), DocumentRelationType.TRANSCRIBES,
                src.getAuthor().getId() == dst.getAuthor().getId());

        RelationController rcDst = new RelationController(dst);
        // imposta conferma sulla relazione esistente
        rcDst.setRelationConfirmed(src, true);

        // cerca relazioni confermate per destination
        List<DocumentRelation> confirmed = rcDst.searchDestinationRelationsByConfirm(true);
        assertNotNull(confirmed);
        assertTrue(confirmed.stream().anyMatch(r -> r.getSource() != null && r.getSource().getId() == src.getId()));

        // ora proviamo la rimozione attraverso confirmed=false (il DAO rimuove la relazione se confirmed=false)
        // usiamo la stessa relazione appena creata
        rcDst.setRelationConfirmed(src, false);

        // la relazione deve essere stata rimossa
        int countAfterRemove = countRelationRows(src.getId(), dst.getId(), DocumentRelationType.TRANSCRIBES);
        assertEquals(0, countAfterRemove);

        // chiamata con source null non deve lanciare eccezioni
        assertDoesNotThrow(() -> rcDst.setRelationConfirmed(null, true));
    }

    /*
     * daoByConfirm
     * - test diretto sulle API DAO: getSourceRelationsByConfirm/getDestinationRelationsByConfirm
     * - verifica conferma e rimozione tramite setRelationConfirmed
     */
    @Test
    void daoByConfirm() {
        UserController uc = new UserController(currentUser);
        uc.createDocument("Dsrc", "d", "1900", DocumentFormat.TXT, java.util.List.of("t"));
        uc.createDocument("Ddst", "d", "1900", DocumentFormat.TXT, java.util.List.of("t"));
        List<Document> docs = documentDAO.getDocumentsByAuthor(currentUser.getId());
        Document src = docs.get(1);
        Document dst = docs.get(0);

        // inserisci relazione e conferma tramite DAO/controller
        relationDAO.addDocumentRelation(src.getId(), dst.getId(), DocumentRelationType.QUOTE,
                src.getAuthor().getId() == dst.getAuthor().getId());
        // conferma
        relationDAO.setRelationConfirmed(src.getId(), dst.getId(), true);

        List<DocumentRelation> srcConfirmed = relationDAO.getSourceRelationsByConfirm(src.getId(), true);
        assertNotNull(srcConfirmed);
        assertTrue(srcConfirmed.stream().anyMatch(r -> r.getDestination() != null && r.getDestination().getId() == dst.getId()));

        List<DocumentRelation> dstConfirmed = relationDAO.getDestinationRelationsByConfirm(dst.getId(), true);
        assertNotNull(dstConfirmed);
        assertTrue(dstConfirmed.stream().anyMatch(r -> r.getSource() != null && r.getSource().getId() == src.getId()));

        // rimozione con confirmed=false
        relationDAO.setRelationConfirmed(src.getId(), dst.getId(), false);
        List<DocumentRelation> srcAfter = relationDAO.getSourceRelationsByConfirm(src.getId(), true);
        assertTrue(srcAfter.isEmpty());
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