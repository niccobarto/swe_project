package ORM;


import DomainModel.User;

import java.util.List;

public class UserDAO extends BaseDAO {
    public UserDAO() {
    }

    public void addUser(int userId, String name, String surname, String email, String password, boolean isModerator) {
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