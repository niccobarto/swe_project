package ORM;


import DomainModel.Document;
import DomainModel.DocumentStatus;
import DomainModel.User;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
        try{
            String query = "DELETE FROM user WHERE id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, userId);
            ResultSet rs= statement.executeQuery();
            if(rs.next()) {
                System.out.println("User removed successfully");
            }
            else{
                System.out.println("User not found");
            }
        }catch (SQLException e){}
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
        }catch (SQLException e){}
    return user;
    }

    public List<User> getModerators() {
        List<User> moderators = new ArrayList<User>();
        try{
            String query = "SELECT * FROM user WHERE is_moderator = true";
            PreparedStatement statement = connection.prepareStatement(query);
            ResultSet rs= statement.executeQuery();
            if(rs.next()) {
                moderators.add(createUserFromResultSet(rs));
            }
            else{
                System.out.println("No moderators found");
            }
        }catch(Exception e){}
        return moderators;
    }
    public void addFavouriteDocuments(int userId,int documentId){
        try{
            String query = "INSERT INTO favourite_documents (user_id,document_id) VALUES(?,?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, userId);
            statement.setInt(2, documentId);
            ResultSet rs=statement.executeQuery();
            if (rs.next()) {
                System.out.println("Document added to favourites");
            }
            else{
                System.out.println("Error adding document to favourites");
            }
        }catch(Exception e){}
    }
    public void removeFavouriteDocuments(int userId,int documentId){
        try{
            String query = "DELETE FROM favourite_documents WHERE user_id = ? AND document_id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, userId);
            statement.setInt(2, documentId);
            ResultSet rs=statement.executeQuery();
            if (rs.next()) {
                System.out.println("Document removed from favourites");
            }
            else{
                System.out.println("Error removing document from favourites");
            }
        }catch(Exception e){}
    }
    public void addFavouriteCollection(int userId,int collectionId){
        try{
            String query = "INSERT INTO favourite_collections (user_id,collection_id) VALUES(?,?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, userId);
            statement.setInt(2, collectionId);
            ResultSet rs=statement.executeQuery();
            if (rs.next()) {
                System.out.println("Collection added to favourites");
            }
            else{
                System.out.println("Error adding collection to favourites");
            }
        }catch(Exception e){}
    }
    public void removeFavouriteCollection(int userId,int collectionId){
        try{
            String query = "DELETE FROM favourite_collections WHERE user_id = ? AND collection_id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, userId);
            statement.setInt(2, collectionId);
            ResultSet rs=statement.executeQuery();
            if (rs.next()) {
                System.out.println("Collection removed from favourites");
            }
            else{
                System.out.println("Error removing collection from favourites");
            }
        }catch(Exception e){}
    }
    public List<Document> getFavouriteDocument(int userId){
        List<Document> documents = new ArrayList<>();
        try{
            String query = "SELECT document_id FROM favourite_documents WHERE user_id = ?";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setInt(1, userId);
            ResultSet rs=ps.executeQuery();
            List<Document> favDocs = new ArrayList<>();
            DocumentDAO documentDAO= new DocumentDAO();
            while (rs.next()) {
                documents.add(documentDAO.getDocumentById(rs.getInt("document_id")));
            }
            return favDocs;
        }catch (SQLException e){}
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
        }catch (SQLException e){}
        return users;
    }
    public List<User> getUserByEmail(String email){
        List<User> users = new ArrayList<>();
        try{
            String query = "SELECT * FROM user WHERE email = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, email);
            ResultSet rs= statement.executeQuery();
            while(rs.next()) {
                users.add(createUserFromResultSet(rs));
            }
        }catch (SQLException e){}
        return users;
    }

    //------ private methods

    private User createUserFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        String surname = rs.getString("surname");
        String email = rs.getString("email");
        String password = rs.getString("password");
        boolean isModerator = rs.getBoolean("is_moderator");
        return new User(id, name, surname, email, password, isModerator);
    }
}