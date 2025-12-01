package ORM;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class DBConnection {
    private static final Logger LOGGER = Logger.getLogger(DBConnection.class.getName());
    private static DBConnection instance;
    private static boolean enableTesting = false; // flag per scegliere il DB
    private Connection connection;

    // NOTE: Valori per il DB di produzione (lasciati come prima)
    private static final String PROD_DB_URL = "jdbc:postgresql://localhost:5432/scriptoria_db";
    private static final String PROD_DB_USER = "postgres";
    private static final String PROD_DB_PASSWORD = "Anotherunifithing";

    // NOTE: Valori per il DB di test: lasciali vuoti come richiesto e compilali tu
    private static final String TEST_DB_URL = "jdbc:postgresql://localhost:5432/scriptoria_db_testing";       // es. jdbc:postgresql://localhost:5432/scriptoria_test
    private static final String TEST_DB_USER = "postgres";      // es. postgres
    private static final String TEST_DB_PASSWORD = "Anotherunifithing";  // es. password

    /**
     * Costruttore privato: crea la connessione usando la modalità (testing/prod) indicata.
     */
    private DBConnection(boolean testing) throws SQLException {
        try{
            final String DB_URL;
            final String DB_USER;
            final String DB_PASSWORD;

            if (testing) {
                DB_URL = TEST_DB_URL;
                DB_USER = TEST_DB_USER;
                DB_PASSWORD = TEST_DB_PASSWORD;
                LOGGER.info("Inizializzazione DBConnection in modalità TEST");
            } else {
                DB_URL = PROD_DB_URL;
                DB_USER = PROD_DB_USER;
                DB_PASSWORD = PROD_DB_PASSWORD;
                LOGGER.info("Inizializzazione DBConnection in modalità PRODUCTION");
            }

            // Se i valori per il DB di test sono vuoti e stiamo in modalità testing, logga un avviso
            if (testing && (DB_URL == null || DB_URL.isBlank())) {
                LOGGER.warning("DB testing configurato ma TEST_DB_URL è vuoto. Compila i parametri di test in DBConnection.");
            }

            this.connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            LOGGER.info("Connessione al database stabilita");
        }catch(SQLException e){
            LOGGER.log(Level.SEVERE, "Errore durante la connessione al database", e);
            throw e;
        }
    }

    /**
     * Restituisce l'istanza singleton usando il flag statico enableTesting.
     */
    public static synchronized DBConnection getInstance() throws SQLException {
        if (instance == null) {
            instance = new DBConnection(enableTesting);
        }
        return instance;
    }

    /**
     * Restituisce l'istanza singleton scegliendo la modalità (testing/prod).
     * Se la modalità richiesta è diversa dall'istanza già creata, l'istanza verrà ricreata.
     */
    public static synchronized DBConnection getInstance(boolean enable_testing) throws SQLException {
        // Se l'istanza esistente è diversa dalla modalità richiesta, la resettiamo
        if (instance == null || enableTesting != enable_testing) {
            enableTesting = enable_testing;
            if (instance != null) {
                try {
                    instance.connection.close();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Errore durante la chiusura della vecchia connessione singleton", e);
                }
                instance = null;
            }
            instance = new DBConnection(enableTesting);
        }
        return instance;
    }

    /**
     * Setta il flag enableTesting. Se è diverso dallo stato corrente, resetta l'istanza per ricrearla successivamente.
     */
    public static synchronized void setEnableTesting(boolean enable_testing) {
        if (enableTesting != enable_testing) {
            enableTesting = enable_testing;
            if (instance != null) {
                try {
                    instance.connection.close();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Errore durante la chiusura della connessione in setEnableTesting", e);
                }
                instance = null;
            }
        }
    }

    /**
     * Forza il reset dell'istanza singleton (utile nei test se vuoi ricreare la connessione).
     */
    public static synchronized void resetInstance() {
        if (instance != null) {
            try {
                instance.connection.close();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Errore durante la chiusura della connessione in resetInstance", e);
            }
            instance = null;
        }
    }

    public Connection getConnection(){
        return connection;
    }


}
