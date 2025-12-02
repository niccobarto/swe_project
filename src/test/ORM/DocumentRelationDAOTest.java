package ORM;

import DomainModel.DocumentRelation;
import DomainModel.DocumentRelationType;
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

class DocumentRelationDAOTest {

    private Connection conn;
    private UserDAO userDAO;
    private DocumentDAO documentDAO;
    private DocumentRelationDAO relationDAO;
    private User testUser;

    private int srcDocId;
    private int dstDocId;
    private int dstDocId2; // additional destination for multi-relation tests

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

            String email = "drtest+" + System.currentTimeMillis() + "@example.com";
            userDAO.addUser("Rel", "Tester", email, "pwd", false, false);
            testUser = userDAO.getUserByEmail(email);
            assertNotNull(testUser, "Impossibile creare user di test");

            // crea tre documenti: uno sorgente e due destinazioni
            documentDAO.addDocument(testUser, "SrcDoc", "d", "1900", DocumentFormat.TXT, "document/" + testUser.getId() + "/", "f1", List.of());
            documentDAO.addDocument(testUser, "DstDoc", "d", "1900", DocumentFormat.TXT, "document/" + testUser.getId() + "/", "f2", List.of());
            documentDAO.addDocument(testUser, "DstDoc2", "d", "1900", DocumentFormat.TXT, "document/" + testUser.getId() + "/", "f3", List.of());

            List<DomainModel.Document> docs = documentDAO.getDocumentsByAuthor(testUser.getId());
            assertTrue(docs.size() >= 3, "Non sono stati creati i documenti di test");
            // newest first: DstDoc2, DstDoc, SrcDoc
            dstDocId2 = docs.get(0).getId();
            dstDocId = docs.get(1).getId();
            srcDocId = docs.get(2).getId();

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
    void addAndRemoveDocumentRelation() {
        // ensure no relation
        assertEquals(0, countRelationRows(srcDocId, dstDocId));

        relationDAO.addDocumentRelation(srcDocId, dstDocId, DocumentRelationType.QUOTE, false);
        assertEquals(1, countRelationRows(srcDocId, dstDocId));

        relationDAO.removeDocumentRelation(srcDocId, dstDocId);
        assertEquals(0, countRelationRows(srcDocId, dstDocId));
    }

    @Test
    void duplicateInsertShouldNotCreateMultipleRelations() {
        // inserting the same relation twice should not create duplicates
        relationDAO.addDocumentRelation(srcDocId, dstDocId, DocumentRelationType.QUOTE, false);
        // second insert of the same pair+type
        relationDAO.addDocumentRelation(srcDocId, dstDocId, DocumentRelationType.QUOTE, false);
        int i=countRelationRows(srcDocId, dstDocId);
        assertEquals(1, countRelationRows(srcDocId, dstDocId), "Duplicate insert created multiple rows");
    }

    @Test
    void updateDocumentRelation() {
        relationDAO.addDocumentRelation(srcDocId, dstDocId, DocumentRelationType.ANSWER_TO, false);
        // pair should exist
        assertEquals(1, countRelationRows(srcDocId, dstDocId));

        // update the type
        relationDAO.updateDocumentRelation(srcDocId, dstDocId, DocumentRelationType.TRANSCRIBES);

        // pair count should still be one (relation updated, not duplicated)
        assertEquals(1, countRelationRows(srcDocId, dstDocId));

        // verify the relation now has the new type
        List<DocumentRelation> res = relationDAO.getSourceRelationDocument(srcDocId, DocumentRelationType.TRANSCRIBES);
        assertNotNull(res);
        assertTrue(res.stream().anyMatch(r -> r.getDestination() != null && r.getDestination().getId() == dstDocId));
    }

    @Test
    void getSourceAndDestinationRelationDocument() {
        // create two relations from the same source to two different destinations
        relationDAO.addDocumentRelation(srcDocId, dstDocId, DocumentRelationType.NEW_VERSION_OF, false);
        relationDAO.addDocumentRelation(srcDocId, dstDocId2, DocumentRelationType.QUOTE, false);

        List<DocumentRelation> allSource = relationDAO.getSourceRelationDocument(srcDocId, null);
        assertNotNull(allSource);
        assertTrue(allSource.size() >= 2);

        List<DocumentRelation> sourceFiltered = relationDAO.getSourceRelationDocument(srcDocId, DocumentRelationType.NEW_VERSION_OF);
        assertNotNull(sourceFiltered);
        assertTrue(sourceFiltered.stream().anyMatch(r -> r.getDestination() != null && r.getDestination().getId() == dstDocId));

        List<DocumentRelation> allDest = relationDAO.getDestinationRelationDocument(dstDocId, null);
        assertNotNull(allDest);
        assertTrue(allDest.size() >= 1);

        List<DocumentRelation> destFiltered = relationDAO.getDestinationRelationDocument(dstDocId, DocumentRelationType.NEW_VERSION_OF);
        assertNotNull(destFiltered);
        assertTrue(destFiltered.stream().anyMatch(r -> r.getSource() != null && r.getSource().getId() == srcDocId));
    }

    @Test
    void getAllSourceAndAllDestinationRelationDocument() {
        relationDAO.addDocumentRelation(srcDocId, dstDocId, DocumentRelationType.TRANSCRIBES, false);
        relationDAO.addDocumentRelation(srcDocId, dstDocId2, DocumentRelationType.QUOTE, false);

        List<DocumentRelation> srcAll = relationDAO.getAllSourceRelationDocument(srcDocId);
        assertNotNull(srcAll);
        assertTrue(srcAll.size() >= 2);

        List<DocumentRelation> dstAll = relationDAO.getAllDestinationRelationDocument(dstDocId);
        assertNotNull(dstAll);
        assertTrue(dstAll.size() >= 1);

        List<DocumentRelation> dst2All = relationDAO.getAllDestinationRelationDocument(dstDocId2);
        assertNotNull(dst2All);
        assertTrue(dst2All.size() >= 1);
    }

    @Test
    void getRelationsByConfirmAndSetConfirmed() {
        // add unconfirmed relation
        relationDAO.addDocumentRelation(srcDocId, dstDocId, DocumentRelationType.TRANSCRIBES, false);
        // add confirmed relation (on different destination)
        relationDAO.addDocumentRelation(srcDocId, dstDocId2, DocumentRelationType.NEW_VERSION_OF, true);

        List<DocumentRelation> srcConfirmed = relationDAO.getSourceRelationsByConfirm(srcDocId, true);
        assertNotNull(srcConfirmed);
        assertTrue(srcConfirmed.stream().anyMatch(r -> r.getDestination() != null && (r.getDestination().getId() == dstDocId2 || r.getDestination().getId() == dstDocId)));

        List<DocumentRelation> dstConfirmed = relationDAO.getDestinationRelationsByConfirm(dstDocId2, true);
        assertNotNull(dstConfirmed);
        assertTrue(dstConfirmed.stream().anyMatch(r -> r.getSource() != null && r.getSource().getId() == srcDocId));

        // test setRelationConfirmed(true) sets confirmed for an existing relation
        relationDAO.setRelationConfirmed(srcDocId, dstDocId, true);
        List<DocumentRelation> nowConfirmed = relationDAO.getSourceRelationsByConfirm(srcDocId, true);
        assertTrue(nowConfirmed.stream().anyMatch(r -> r.getDestination() != null && r.getDestination().getId() == dstDocId));
    }

    @Test
    void daoDirectConfirmBehavior() {
        // add relation and confirm via DAO
        relationDAO.addDocumentRelation(srcDocId, dstDocId, DocumentRelationType.NEW_VERSION_OF, false);
        relationDAO.setRelationConfirmed(srcDocId, dstDocId, true);
        List<DocumentRelation> srcC = relationDAO.getSourceRelationsByConfirm(srcDocId, true);
        assertNotNull(srcC);
        assertTrue(srcC.stream().anyMatch(r -> r.getDestination() != null && r.getDestination().getId() == dstDocId));

        // now remove via DAO
        relationDAO.setRelationConfirmed(srcDocId, dstDocId, false);
        List<DocumentRelation> srcAfter = relationDAO.getSourceRelationsByConfirm(srcDocId, true);
        assertTrue(srcAfter.isEmpty());
    }

    // helper to count relation rows directly from DB
    private int countRelationRows(int sourceId, int destinationId) {
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS cnt FROM document_relation WHERE source_id=? AND destination_id=?");
            ps.setInt(1, sourceId);
            ps.setInt(2, destinationId);
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