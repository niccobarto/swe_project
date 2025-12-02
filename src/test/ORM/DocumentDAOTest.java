package ORM;

import DomainModel.Document;
import DomainModel.DocumentFormat;
import DomainModel.DocumentStatus;
import DomainModel.DocumentSearchCriteriaBuilder;
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

class DocumentDAOTest {

    private Connection conn;
    private UserDAO userDAO;
    private DocumentDAO documentDAO;
    private User testUser;
    private int doc1Id;
    private int doc2Id;
    private int doc3Id;

    @BeforeEach
    void setUp() {
        try {
            DBConnection.setEnableTesting(true);
            DBConnection.resetInstance();
            conn = DBConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            userDAO = new UserDAO();
            documentDAO = new DocumentDAO();

            // crea utente di test
            String email = "doctest+" + System.currentTimeMillis() + "@example.com";
            userDAO.addUser("Doc", "Tester", email, "pwd", false, false);
            testUser = userDAO.getUserByEmail(email);
            assertNotNull(testUser, "Impossibile creare user di test");

            // crea tre documenti con caratteristiche diverse
            boolean c1 = documentDAO.addDocument(testUser, "Title One", "desc1", "1900", DocumentFormat.TXT, "path/", "file1", List.of());
            boolean c2 = documentDAO.addDocument(testUser, "Another Title", "desc2", "2000", DocumentFormat.PDF, "path/", "file2", List.of());
            boolean c3 = documentDAO.addDocument(testUser, "Special Title", "desc3", "2001", DocumentFormat.TXT, "path/", "file3", List.of());
            assertTrue(c1 && c2 && c3, "Impossibile creare documenti di test");

            List<Document> docs = documentDAO.getDocumentsByAuthor(testUser.getId());
            assertTrue(docs.size() >= 3, "Dovrebbero esserci almeno 3 documenti dell'autore di test");
            //i documenti vengono ritornati in ordine decrescente rispetto alla data di creazione
            doc1Id = docs.get(2).getId();
            doc2Id = docs.get(1).getId();
            doc3Id = docs.get(0).getId();

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
    void addDocument() {
        Document d = documentDAO.getDocumentById(doc1Id);
        assertNotNull(d);
        assertEquals("Title One", d.getTitle());
        assertEquals(testUser.getId(), d.getAuthor().getId());
    }

    @Test
    void deleteDocument() {
        // crea un documento temporaneo
        boolean created = documentDAO.addDocument(testUser, "ToDelete", "todel", "1999", DocumentFormat.TXT, "p/", "todelete", List.of());
        assertTrue(created);
        int tmpId = documentDAO.getDocumentsByAuthor(testUser.getId()).get(0).getId();

        documentDAO.deleteDocument(tmpId);
        Document after = documentDAO.getDocumentById(tmpId);
        assertNull(after, "Documento non rimosso correttamente");
    }

    @Test
    void updateDocumentStatus() {
        documentDAO.updateDocumentStatus(doc1Id, DocumentStatus.PENDING);
        Document d = documentDAO.getDocumentById(doc1Id);
        assertNotNull(d);
        assertEquals(DocumentStatus.PENDING, d.getStatus());
    }

    @Test
    void getDocumentById() {
        Document d = documentDAO.getDocumentById(doc2Id);
        assertNotNull(d);
        assertEquals("Another Title", d.getTitle());
        assertEquals(DocumentFormat.PDF, d.getFormat());
    }

    @Test
    void getDocumentsByAuthor() {
        List<Document> docs = documentDAO.getDocumentsByAuthor(testUser.getId());
        assertNotNull(docs);
        assertTrue(docs.stream().anyMatch(x -> x.getId() == doc1Id));
    }

    @Test
    void getAllDocuments() {
        List<Document> all = documentDAO.getAllDocuments();
        assertNotNull(all);
        assertTrue(all.size() >= 3, "getAllDocuments dovrebbe restituire almeno 3 documenti");
    }

    @Test
    void getDocumentsByStatus() {
        // imposta status diversi
        documentDAO.updateDocumentStatus(doc1Id, DocumentStatus.PENDING);
        documentDAO.updateDocumentStatus(doc2Id, DocumentStatus.PUBLISHED);

        List<Document> pending = documentDAO.getDocumentsByStatus(DocumentStatus.PENDING);
        assertTrue(pending.stream().anyMatch(d -> d.getId() == doc1Id));

        List<Document> published = documentDAO.getDocumentsByStatus(DocumentStatus.PUBLISHED);
        assertTrue(published.stream().anyMatch(d -> d.getId() == doc2Id));
    }

    @Test
    void searchDocuments() {
        try {
            // aggiungo un tag direttamente nelle tabelle per testare il filtro
            PreparedStatement psTag = conn.prepareStatement("INSERT INTO tag (tag_label, description) VALUES (?, ?)");
            psTag.setString(1, "testtag");
            psTag.setString(2, "a test tag");
            psTag.executeUpdate();
            psTag.close();

            PreparedStatement psDocTag = conn.prepareStatement("INSERT INTO document_tags (document_id, tag_label) VALUES (?, ?)");
            psDocTag.setInt(1, doc3Id);
            psDocTag.setString(2, "testtag");
            psDocTag.executeUpdate();
            psDocTag.close();

            // cerca per titolo parziale
            var criteria1 = DocumentSearchCriteriaBuilder.getInstance().setDocumentTitle("Title").build();
            List<Document> res1 = documentDAO.searchDocuments(criteria1);
            assertTrue(res1.stream().anyMatch(d -> d.getId() == doc1Id || d.getId() == doc3Id));

            // cerca per format
            var criteria2 = DocumentSearchCriteriaBuilder.getInstance().setFormat(DocumentFormat.PDF).build();
            List<Document> res2 = documentDAO.searchDocuments(criteria2);
            assertTrue(res2.stream().anyMatch(d -> d.getFormat() == DocumentFormat.PDF));

            // cerca per tag
            var criteria3 = DocumentSearchCriteriaBuilder.getInstance().setTags(List.of("testtag")).build();
            List<Document> res3 = documentDAO.searchDocuments(criteria3);
            assertTrue(res3.stream().anyMatch(d -> d.getId() == doc3Id), "searchDocuments by tag non ha restituito il documento atteso");

        } catch (SQLException e) {
            fail("Eccezione in searchDocuments: " + e.getMessage());
        }
    }

}