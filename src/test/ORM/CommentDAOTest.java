package ORM;

import DomainModel.Comment;
import DomainModel.DocumentFormat;
import DomainModel.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CommentDAOTest {

    private Connection conn;
    private UserDAO userDAO;
    private DocumentDAO documentDAO;
    private CommentDAO commentDAO;
    private User testUser;
    private int testDocId;

    @BeforeEach
    void setUp() {
        try {
            // Attiva la modalità TEST e forza la ricreazione dell'istanza
            DBConnection.setEnableTesting(true);
            DBConnection.resetInstance();
            conn = DBConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            userDAO = new UserDAO();
            documentDAO = new DocumentDAO();
            commentDAO = new CommentDAO();

            String email = "commenttest+" + System.currentTimeMillis() + "@example.com";
            userDAO.addUser("Test", "User", email, "pwd", false, false);
            testUser = userDAO.getUserByEmail(email);
            assertNotNull(testUser, "Impossibile creare user di test");

            // crea un documento per l'utente
            String title = "Doc-" + System.currentTimeMillis();
            boolean created = documentDAO.addDocument(testUser, title, "desc", "1900", DocumentFormat.TXT, "document/" + testUser.getId() + "/", "file_test", List.of());
            assertTrue(created, "Impossibile creare documento di test");

            // prendi il documento più recente dell'autore
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
    void addAndGetCommentsByDocumentAndAuthor() {
        // aggiungi commento
        String uniqueText = "cmt-" + System.currentTimeMillis();
        commentDAO.addComment(uniqueText, testUser.getId(), testDocId);

        List<Comment> byDoc = commentDAO.getCommentsByDocument(testDocId);
        assertNotNull(byDoc);
        assertTrue(byDoc.stream().anyMatch(c -> uniqueText.equals(c.getText())), "Commento non trovato tramite getCommentsByDocument");

        List<Comment> byAuthor = commentDAO.getCommentsByAuthor(testUser.getId());
        assertNotNull(byAuthor);
        assertTrue(byAuthor.stream().anyMatch(c -> uniqueText.equals(c.getText())), "Commento non trovato tramite getCommentsByAuthor");
    }

    @Test
    void removeComment() {
        String uniqueText = "cmt-rem-" + System.currentTimeMillis();
        commentDAO.addComment(uniqueText, testUser.getId(), testDocId);

        List<Comment> comments = commentDAO.getCommentsByDocument(testDocId);
        Comment added = comments.stream().filter(c -> uniqueText.equals(c.getText())).findFirst().orElse(null);
        assertNotNull(added, "Commento aggiunto non trovato prima della rimozione");

        commentDAO.removeComment(added.getId());

        List<Comment> after = commentDAO.getCommentsByDocument(testDocId);
        assertFalse(after.stream().anyMatch(c -> uniqueText.equals(c.getText())), "Commento non rimosso");
    }
}
