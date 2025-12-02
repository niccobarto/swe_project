package BusinessLogic;

import DomainModel.User;
import ORM.UserDAO;
import ORM.DBConnection;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class LoginControllerTest {

    private UserDAO userDAO;
    private LoginController controller;
    private Connection conn;

    @BeforeEach
    void setUp() throws Exception {
        // ATTIVA la modalità TEST e forza la ricreazione dell'istanza singleton
        DBConnection.setEnableTesting(true);
        DBConnection.resetInstance();

        conn = DBConnection.getInstance().getConnection();

        // Metti la connection in modalità transaction: tutte le modifiche verranno annullate nel tearDown
        conn.setAutoCommit(false);
        controller= new LoginController();
        userDAO = new UserDAO();
        // Inserisci un utente di test
        userDAO.addUser("Test", "User", "test@example.com", "pwd123", false, false);

        // Informativa: se stai eseguendo i test contro il DB di produzione, valuta di non procedere.
        try {
            String url = conn.getMetaData().getURL();
            if (url != null && !url.toLowerCase().contains("test")) {
                System.out.println("[LoginControllerTest] Warning: DB URL does not contain 'test' (" + url + "). Make sure you run tests against a test database.");
            }
        } catch (Exception e) {
            // Ignora eventuali problemi nel recuperare la metadata
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (conn != null) {
            try {
                // Annulla tutte le modifiche fatte durante il test
                if (!conn.getAutoCommit()) {
                    conn.rollback();
                }
            } finally {
                // Ripristina lo stato di auto-commit
                conn.setAutoCommit(true);
            }
        }

        // opzionale: resetta il singleton tra i test
        DBConnection.resetInstance();
    }

    /*
     * login
     * - caso: credenziali corrette -> ritorna utente
     * - caso: password errata -> ritorna null
     * - caso: utente inesistente -> ritorna null
     */
    @Test
    void login() {
        // credenziali corrette
        User u = controller.login("test@example.com", "pwd123");
        assertNotNull(u, "Login con credenziali corrette dovrebbe restituire un utente");
        assertEquals("test@example.com", u.getEmail());

        // password errata
        User u2 = controller.login("test@example.com", "wrongpwd");
        assertNull(u2, "Login con password errata dovrebbe restituire null");

        // utente inesistente
        User u3 = controller.login("nonexistent@example.com", "whatever");
        assertNull(u3, "Login per utente inesistente dovrebbe restituire null");
    }

    /*
     * register
     * - caso: registra nuovo utente e lo ritorna
     */
    @Test
    void register() {
        String email = "newuser@example.com";
        User created = controller.register("New", "User", email, "pass", false, false);
        assertNotNull(created, "Registrazione valida dovrebbe restituire l'utente creato");
        assertEquals(email, created.getEmail());
    }

    /*
     * emailAvailable
     * - caso: email già presente -> false
     * - caso: email libera -> true
     */
    @Test
    void emailAvailable() {
        assertFalse(controller.isEmailAvailable("test@example.com"));
        assertTrue(controller.isEmailAvailable("available@example.com"));
    }
}
