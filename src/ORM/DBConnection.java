package ORM;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class DBConnection {
    private static DBConnection instance;
    private Connection connection;

    private DBConnection() throws SQLException {
        try{
            String DB_URL="jdbc:postgresql://localhost:5432/scriptoria_db";
            String DB_USER="postgres";
            String DB_PASSWORD="Anotherunifithing";

            this.connection=DriverManager.getConnection(DB_URL,DB_USER,DB_PASSWORD);
            System.out.println("Connected to database!");
        }catch(SQLException e){
            System.err.println("Error during database connection!");
            e.printStackTrace();
        }
    }

    public static synchronized DBConnection getInstance() throws SQLException {
        if (instance == null){
            instance= new DBConnection();
        }
        return instance;
    }

    public Connection getConnection(){
        return connection;
    }


}
