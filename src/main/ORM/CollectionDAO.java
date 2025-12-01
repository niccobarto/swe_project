package ORM;

import DomainModel.Document;
import DomainModel.DocumentRelationType;
import DomainModel.User;
import DomainModel.Collection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CollectionDAO extends BaseDAO {
    private static final Logger LOGGER = Logger.getLogger(CollectionDAO.class.getName());
    public CollectionDAO() {
        super();
    }
    public void addCollection(String collectionName, String description, User user){
        try{
            String query= "INSERT INTO collection (collection_name,description,user_id) VALUES(?,?,?)";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, collectionName);
            ps.setString(2, description);
            ps.setInt(3, user.getId());
            ps.executeUpdate();
            ps.close();
        }catch (SQLException e){
            LOGGER.log(Level.SEVERE, "Errore durante addCollection(userId=" + (user!=null?user.getId():null) + ")", e);
        }
    }
    public void deleteCollection(int collectionId){
        try{
            String q1="DELETE FROM document_collection WHERE collection_id=?";
            PreparedStatement ps1=connection.prepareStatement(q1);
            ps1.setInt(1,collectionId);
            ps1.executeUpdate();
            ps1.close();

            String q2="DELETE FROM favourite_collection WHERE collection_id=?";
            PreparedStatement ps2=connection.prepareStatement(q2);
            ps2.setInt(1,collectionId);
            ps2.executeUpdate();
            ps2.close();

            String q3="DELETE FROM collection WHERE id=?";
            PreparedStatement ps3=connection.prepareStatement(q3);
            ps3.setInt(1,collectionId);
            ps3.executeUpdate();
            ps3.close();
        }catch (SQLException e){
            LOGGER.log(Level.SEVERE, "Errore durante deleteCollection(id=" + collectionId + ")", e);
        }
    }
    public void addDocumentToCollection(int documentId, int collectionId){
        try{
            String query="INSERT INTO document_collection (collection_id,document_id) VALUES(?,?)";
            PreparedStatement ps=connection.prepareStatement(query);
            ps.setInt(1,collectionId);
            ps.setInt(2,documentId);
            ps.executeUpdate();
            ps.close();
        }catch (SQLException e){
            LOGGER.log(Level.SEVERE, "Errore durante addDocumentToCollection(docId=" + documentId + ", collectionId=" + collectionId + ")", e);
        }
    }
    public void removeDocumentFromCollection(int documentId, int collectionId){
        try{
            String query="DELETE FROM document_collection WHERE collection_id=? AND document_id=?";
            PreparedStatement ps=connection.prepareStatement(query);
            ps.setInt(1,collectionId);
            ps.setInt(2,documentId);
            ps.executeUpdate();
            ps.close();
        }catch (SQLException e){
            LOGGER.log(Level.SEVERE, "Errore durante removeDocumentFromCollection(docId=" + documentId + ", collectionId=" + collectionId + ")", e);

        }
    }
    public List<Collection> getCollectionsByUser(int userId){
        List<Collection> collections = new ArrayList<Collection>();
        try {
            String query="SELECT * FROM collection WHERE user_id=?";
            PreparedStatement ps=connection.prepareStatement(query);
            ps.setInt(1,userId);
            ResultSet rs=ps.executeQuery();
            while(rs.next()){
                collections.add(createCollectionFromResultSet(rs));
            }
            rs.close();
            ps.close();
        }catch (SQLException e){
            LOGGER.log(Level.SEVERE, "Errore durante getCollectionsByUser(userId=" + userId + ")", e);
        }
        return collections;
    }
    public List<Document> getDocumentsByCollection(int collectionId){
        List<Document> documents=new ArrayList<Document>();
        try{
            String query="SELECT * FROM document_collection WHERE collection_id=?";
            PreparedStatement ps=connection.prepareStatement(query);
            ps.setInt(1,collectionId);
            ResultSet rs=ps.executeQuery();
            while(rs.next()){
                DocumentDAO docDao=new DocumentDAO();
                Document doc=docDao.getDocumentById(rs.getInt("document_id"));
                documents.add(doc);
            }
            rs.close();
            ps.close();
        }catch (SQLException e){
            LOGGER.log(Level.SEVERE, "Errore durante getDocumentsByCollection(collectionId=" + collectionId + ")", e);
        }
        return documents;
    }
    public List<Collection> getAllCollections(){
        List<Collection> collections=new ArrayList<>();
        try{
            String query="SELECT * FROM collection";
            PreparedStatement ps=connection.prepareStatement(query);
            ResultSet rs=ps.executeQuery();
            while(rs.next()){
                collections.add(createCollectionFromResultSet(rs));
            }
            rs.close();
            ps.close();
        }catch (SQLException e){
            LOGGER.log(Level.SEVERE, "Errore durante getAllCollection()", e);
        }
        return collections;
    }
    public Collection getCollectionById(int collectionId){
        Collection c=null;
        try{
            String query="SELECT * FROM collection WHERE collection_id=?";
            PreparedStatement ps=connection.prepareStatement(query);
            ps.setInt(1,collectionId);
            ResultSet rs=ps.executeQuery();
            if(rs.next()){
               c=createCollectionFromResultSet(rs);
            }
            rs.close();
            ps.close();
        }catch (SQLException e){
            LOGGER.log(Level.SEVERE, "Errore durante getCollectionById(id=" + collectionId + ")", e);
        }
        return c;
    }

    //private methods

    private Collection createCollectionFromResultSet(ResultSet rs) throws SQLException {
        int id=rs.getInt("collection_id");
        String name=rs.getString("collection_name");
        String description=rs.getString("description");
        UserDAO userDao=new UserDAO();
        User u=userDao.getUserById(rs.getInt("user_id"));
        List<Document> documents=getDocumentsByCollection(id);
        return new Collection(id,name,description,u,documents);
    }
}
