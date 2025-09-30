package ORM;


import DomainModel.DocumentStatus;
import DomainModel.User;

import java.sql.PreparedStatement;
import java.util.List;

public class UserDAO extends BaseDAO {

    public UserDAO() {
        super();
    }

    public void addUser(String name, String surname, String email, String password, boolean isModerator) {
        try{
            String query = "INSERT INTO user (name,surname,email,password,is_moderator) VALUES(?,?,?,?,?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, name);
            statement.setString(2, surname);
            statement.setString(3, email);
            statement.setString(4,password);
            statement.setBoolean(5,isModerator);

            statement.execute();
            System.out.println("User added successfully");
        }catch(Exception e){}
    }

    public void removeUser(int userId) {
    }

    public User getUserById(int userId) {
        return null;
    }

    public List<User> getModerators() {
        return null;
    }
    public void addFavouriteDocuments(int userId,int documentId){}
    public void removeFavouriteDocuments(int userId,int documentId){}
    public void addFavouriteCollection(int userId,int collectionId){}
    public void removeFavouriteCollection(int userId,int collectionId){}
    public List<User> getAllUsers(){return null;}
    public List<User> getUserByEmail(String email){return null;}
}