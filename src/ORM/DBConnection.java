package ORM;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class DBConnection {
    private static final Logger LOGGER = Logger.getLogger(DBConnection.class.getName());
    private static DBConnection instance;
    private Connection connection;

    private DBConnection() throws SQLException {
        try{
            String DB_URL="jdbc:postgresql://localhost:5432/scriptoria_db";
            String DB_USER="postgres";
            String DB_PASSWORD="Anotherunifithing";

            this.connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            LOGGER.info("Connessione al database stabilita");
        }catch(SQLException e){
            LOGGER.log(Level.SEVERE, "Errore durante la connessione al database", e);
            throw e;
        }
    }

    public static synchronized DBConnection getInstance() throws SQLException {
        if (instance == null){
            instance = new DBConnection();
        }
        return instance;
    }

    public Connection getConnection(){
        return connection;
    }


}
