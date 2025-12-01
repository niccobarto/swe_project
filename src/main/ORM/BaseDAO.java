package ORM;
import java.sql.Connection;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BaseDAO {
    protected Connection connection;
    private static final Logger LOGGER = Logger.getLogger(BaseDAO.class.getName());

    protected BaseDAO() {
        try{
            connection = DBConnection.getInstance().getConnection();
        }catch (SQLException e){
            LOGGER.log(Level.SEVERE, "Errore durante l'ottenimento della connessione al database", e);
            throw new IllegalStateException("Impossibile stabilire la connessione al database", e);
        }
    }

    protected Connection getConnection() throws SQLException{
        return connection = DBConnection.getInstance().getConnection();
    }
    protected Connection closeConnection() throws SQLException{
        connection.close();
        return null;
    }
}
