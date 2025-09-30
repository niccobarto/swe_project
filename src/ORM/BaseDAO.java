package ORM;
import java.sql.Connection;
import java.sql.SQLException;

public abstract class BaseDAO {
    protected Connection connection;

    protected BaseDAO() {
        try{
            connection=DBConnection.getInstance().getConnection();
        }catch (SQLException e){
            System.err.println("Error during database connection");
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
