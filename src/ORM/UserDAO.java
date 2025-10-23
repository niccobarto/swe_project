package ORM;


import DomainModel.Document;
import DomainModel.User;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserDAO extends BaseDAO {
    private static final Logger LOGGER = Logger.getLogger(UserDAO.class.getName());

    public UserDAO() {
        super();
    }

    public void addUser(String name, String surname, String email, String password, boolean isModerator, boolean isAdmin) {
        try{
            String query = "INSERT INTO user (name,surname,email,password,is_moderator, is_admin) VALUES(?,?,?,?,?,?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, name);
            statement.setString(2, surname);
            statement.setString(3, email);
            statement.setString(4,password);
            statement.setBoolean(5,isModerator);
            statement.setBoolean(6,isAdmin);

            statement.executeUpdate();
            statement.close();
            System.out.println("User added successfully");
        }catch(Exception e){
            LOGGER.log(Level.SEVERE, "Errore durante addUser(email=" + email + ")", e);
        }
    }

    public void removeUser(int userId) {
        try{
            String query = "DELETE FROM user WHERE id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, userId);
            int affected = statement.executeUpdate();
            statement.close();
            if(affected > 0) {
                System.out.println("User removed successfully");
            }
            else{
                System.out.println("User not found");
            }
        }catch (SQLException e){
            LOGGER.log(Level.SEVERE, "Errore durante removeUser(id=" + userId + ")", e);
        }
    }

    public User getUserById(int userId) {
        User user = null;
        try{
            String query = "SELECT * FROM user WHERE id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, userId);
            ResultSet rs= statement.executeQuery();
            if(rs.next()) {
                user = createUserFromResultSet(rs);
            }
            else{
                System.out.println("User not found");
            }
            rs.close();
            statement.close();
        }catch (SQLException e){
            LOGGER.log(Level.SEVERE, "Errore durante getUserById(id=" + userId + ")", e);
        }
        return user;
    }

    public List<User> getModerators() {
        List<User> moderators = new ArrayList<User>();
        try{
            String query = "SELECT * FROM user WHERE is_moderator = true";
            PreparedStatement statement = connection.prepareStatement(query);
            ResultSet rs= statement.executeQuery();
            while(rs.next()) {
                moderators.add(createUserFromResultSet(rs));
            }
            rs.close();
            statement.close();
        }catch(Exception e){
            LOGGER.log(Level.SEVERE, "Errore durante getModerators()", e);
        }
        return moderators;
    }
    public void addFavouriteDocuments(int userId,int documentId){
        try{
            String query = "INSERT INTO favourite_documents (user_id,document_id) VALUES(?,?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, userId);
            statement.setInt(2, documentId);
            int affected = statement.executeUpdate();
            statement.close();
            if (affected > 0) {
                System.out.println("Documento aggiunto ai preferiti");
            }
            else{
                System.out.println("Errore durante l'aggiunta del documento ai preferiti");
            }
        }catch(Exception e){
            LOGGER.log(Level.SEVERE, "Errore durante addFavouriteDocuments(userId=" + userId + ", docId=" + documentId + ")", e);
        }
    }
    public void removeFavouriteDocuments(int userId,int documentId){
        try{
            String query = "DELETE FROM favourite_documents WHERE user_id = ? AND document_id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, userId);
            statement.setInt(2, documentId);
            int affected = statement.executeUpdate();
            statement.close();
            if (affected > 0) {
                System.out.println("Documento rimosso dai preferiti");
            }
            else{
                System.out.println("Errore durante la rimozione del documento dai preferiti");
            }
        }catch(Exception e){
            LOGGER.log(Level.SEVERE, "Errore durante removeFavouriteDocuments(userId=" + userId + ", docId=" + documentId + ")", e);
        }
    }
    public void addFavouriteCollection(int userId,int collectionId){
        try{
            String query = "INSERT INTO favourite_collections (user_id,collection_id) VALUES(?,?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, userId);
            statement.setInt(2, collectionId);
            int affected = statement.executeUpdate();
            statement.close();
            if (affected > 0) {
                System.out.println("Collezione aggiunta ai preferiti");
            }
            else{
                System.out.println("Errore durante l'aggiunta della collezione ai preferiti");
            }
        }catch(Exception e){
            LOGGER.log(Level.SEVERE, "Errore durante addFavouriteCollection(userId=" + userId + ", collectionId=" + collectionId + ")", e);
        }
    }
    public void removeFavouriteCollection(int userId,int collectionId){
        try{
            String query = "DELETE FROM favourite_collections WHERE user_id = ? AND collection_id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, userId);
            statement.setInt(2, collectionId);
            int affected = statement.executeUpdate();
            statement.close();
            if (affected > 0) {
                System.out.println("Collezione rimossa dai preferiti");
            }
            else{
                System.out.println("Errore durante la rimozione della collezione dai preferiti");
            }
        }catch(Exception e){
            LOGGER.log(Level.SEVERE, "Errore durante removeFavouriteCollection(userId=" + userId + ", collectionId=" + collectionId + ")", e);
        }
    }
    public List<Document> getFavouriteDocument(int userId){
        List<Document> documents = new ArrayList<>();
        try{
            String query = "SELECT document_id FROM favourite_documents WHERE user_id = ?";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setInt(1, userId);
            ResultSet rs=ps.executeQuery();
            DocumentDAO documentDAO= new DocumentDAO();
            while (rs.next()) {
                documents.add(documentDAO.getDocumentById(rs.getInt("document_id")));
            }
            rs.close();
            ps.close();
        }catch (SQLException e){
            LOGGER.log(Level.SEVERE, "Errore durante getFavouriteDocument(userId=" + userId + ")", e);
        }
        return documents;
    }
    public List<User> getAllUsers(){
        List<User> users = new ArrayList<>();
        try{
            String query = "SELECT * FROM user";
            PreparedStatement statement = connection.prepareStatement(query);
            ResultSet rs= statement.executeQuery();
            while(rs.next()) {
                users.add(createUserFromResultSet(rs));
            }
            rs.close();
            statement.close();
        }catch (SQLException e){
            LOGGER.log(Level.SEVERE, "Errore durante getAllUsers()", e);
        }
        return users;
    }
    public User getUserByEmail(String email){
        User user=null;
        try{
            String query = "SELECT * FROM user WHERE email = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, email);
            ResultSet rs= statement.executeQuery();
            if(rs.next()) {
                user=createUserFromResultSet(rs);
            }
            rs.close();
            statement.close();
        }catch (SQLException e){
            LOGGER.log(Level.SEVERE, "Errore durante getUserByEmail(email=" + email + ")", e);
        }
        return user;
    }

    //------ private methods

    private User createUserFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        String surname = rs.getString("surname");
        String email = rs.getString("email");
        String password = rs.getString("password");
        boolean isModerator = rs.getBoolean("is_moderator");
        boolean isAdmin=rs.getBoolean("is_admin");
        return new User(id, name, surname, email, password, isModerator, isAdmin);
    }
}

