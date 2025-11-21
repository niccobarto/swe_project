// java
package ORM;

import DomainModel.*;

import java.sql.PreparedStatement;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class DocumentDAO extends BaseDAO {
    private static final Logger LOGGER = Logger.getLogger(DocumentDAO.class.getName());
    public DocumentDAO(){
        super();
    }

    public void addDocument(User author,
                            String description,
                            String documentPeriod,
                            DocumentFormat documentFormat,
                            String filePath,
                            String fileName,
                            String documentType){

        try{
            String query = "INSERT INTO document (file_name,description,status,period,file_format,file_path,author_id,creation_date) VALUES(?,?,?,?,?,?,?,?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, fileName);
            statement.setString(2, description);
            statement.setString(3,DocumentStatus.DRAFT.toString());
            statement.setString(4,documentPeriod);
            statement.setString(5,documentFormat.toString());
            statement.setString(6,filePath);
            statement.setInt(7,author.getId());
            statement.setDate(8, java.sql.Date.valueOf(java.time.LocalDate.now()));
            statement.executeUpdate();
            statement.close();
        }catch(SQLException e){
            LOGGER.log(Level.SEVERE, "Errore durante addDocument(authorId=" + (author!=null?author.getId():null) + ")", e);
        }
    }

    public void deleteDocument(int documentId){
        try{
            // rimuovi riferimenti dipendenti per evitare vincoli FK
            String q1 = "DELETE FROM FavouriteDocuments WHERE document_id = ?";
            PreparedStatement st1 = connection.prepareStatement(q1);
            st1.setInt(1, documentId);
            st1.executeUpdate();
            st1.close();

            String q2 = "DELETE FROM DocumentRelation WHERE source_id = ? OR destination_id = ?";
            PreparedStatement st2 = connection.prepareStatement(q2);
            st2.setInt(1, documentId);
            st2.setInt(2, documentId);
            st2.executeUpdate();
            st2.close();

            String q3 = "DELETE FROM DocumentTags WHERE document_id = ?";
            PreparedStatement st3 = connection.prepareStatement(q3);
            st3.setInt(1, documentId);
            st3.executeUpdate();
            st3.close();

            String q4 = "DELETE FROM Comment WHERE document_id = ?";
            PreparedStatement st4 = connection.prepareStatement(q4);
            st4.setInt(1, documentId);
            st4.executeUpdate();
            st4.close();

            String q5 = "DELETE FROM PublishRequest WHERE document_id = ?";
            PreparedStatement st5 = connection.prepareStatement(q5);
            st5.setInt(1, documentId);
            st5.executeUpdate();
            st5.close();

            String q6= "DELETE FROM DocumentCollection WHERE document_id = ?";
            PreparedStatement st6 = connection.prepareStatement(q6);
            st6.setInt(1, documentId);
            st6.executeUpdate();
            st6.close();

            // infine rimuovi il documento
            String qDel = "DELETE FROM document WHERE id=?";
            PreparedStatement statement = connection.prepareStatement(qDel);
            statement.setInt(1, documentId);
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Document deleted successfully");
            } else {
                System.out.println("Document not found");
            }
            statement.close();
        }catch(SQLException e){
            LOGGER.log(Level.SEVERE, "Errore durante deleteDocument(id=" + documentId + ")", e);
        }
    }

    public void addTag(Tag t,int documentId){
        try{
            // inserisce il tag nella tabella tag (colonne esplicite)
            String query = "INSERT INTO tag (tag_label, description) VALUES(?,?)";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, t.getLabel());
            ps.setString(2, t.getDescription());
            ps.executeUpdate();
            ps.close();

            // link tra documento e tag (se la tabella DocumentTags esiste)
            try {
                String linkQuery = "INSERT INTO DocumentTags (document_id, tag_label) VALUES(?,?)";
                PreparedStatement linkStmt = connection.prepareStatement(linkQuery);
                linkStmt.setInt(1, documentId);
                linkStmt.setString(2, t.getLabel());
                linkStmt.executeUpdate();
                linkStmt.close();
            } catch (SQLException ex) {
                LOGGER.log(Level.WARNING, "Impossibile collegare il tag al documento (potrebbe non esistere la tabella di link)", ex);
            }

        }catch (SQLException e){
            LOGGER.log(Level.SEVERE, "Errore durante addTag(documentId=" + documentId + ")", e);
        }
    }

    public void removeTag(int tagId){
        try {
            String query = "DELETE FROM tag WHERE tag_id = ?";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setInt(1, tagId);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                System.out.println("Tag removed successfully");
            } else {
                System.out.println("Tag not found");
            }
            ps.close();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Errore durante removeTag(tagId=" + tagId + ")", e);
        }
    }

    public void updateDocumentStatus(int docId,DocumentStatus status){
        try {
            String query = "UPDATE document SET status = ? WHERE id = ?";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, status.toString());
            ps.setInt(2, docId);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                System.out.println("Document updated successfully");
            } else {
                System.out.println("Error on document update");
            }
            ps.close();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Errore durante updateDocumentStatus(id=" + docId + ")", e);
        }
    }
    private Document createDocumentFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String title = rs.getString("title");
        String description = rs.getString("description");
        String statusStr = rs.getString("status");
        String fileFormat = rs.getString("file_format");
        int authorId = rs.getInt("author_id");
        String filePath = rs.getString("file_path");
        String fileName = rs.getString("file_name");
        Date creationDate = rs.getDate("creation_date");

        DocumentStatus status = statusStr != null ? DocumentStatus.valueOf(statusStr) : DocumentStatus.DRAFT;
        User author = new UserDAO().getUserById(authorId);
        Document document = new Document(id, description, DocumentFormat.valueOf(fileFormat), author, filePath, fileName, creationDate);
        document.setStatus(status);
        document.setTags(getTagsForDocument(id));
        return document;
    }

    private List<Tag> getTagsForDocument(int documentId) {
        List<Tag> tags = new ArrayList<>();
        String query = "SELECT t.tag_label, t.description FROM tag t " +
                "JOIN DocumentTags dt ON dt.tag_label = t.tag_label WHERE dt.document_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, documentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String label = rs.getString("tag_label");
                    String description = rs.getString("description");
                    tags.add(new Tag(label, description));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Impossibile recuperare i tag per il documento " + documentId, e);
        }
        return tags;
    }

    public Document getDocumentById(int documentId){
        Document document = null;
        try {
            String query = "SELECT * FROM document WHERE id = ?";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setInt(1, documentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                document=createDocumentFromResultSet(rs);
            }
            rs.close();
            ps.close();
        }catch (SQLException e){
            LOGGER.log(Level.SEVERE, "Errore durante getDocumentById(id=" + documentId + ")", e);
        }
        return document;
    }

    public List<Document> getDocumentsByAuthor(int userId){
        List<Document> documents = new ArrayList<>();
        try {
            String query = "SELECT * FROM document WHERE author_id = ? ORDER BY creation_date DESC";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                documents.add(createDocumentFromResultSet(rs));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Errore durante getDocumentsByAuthor(userId=" + userId + ")", e);
        }
        return documents;
    }

    public List<Document> getAllDocuments(){
        List<Document> documents = new ArrayList<>();
        try {
            String query = "SELECT * FROM document ORDER BY creation_date DESC";
            PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            while(rs.next()){
                documents.add(createDocumentFromResultSet(rs));
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Errore durante getAllDocuments()", e);
        }
        return documents;
    }

    public List<Document> getDocumentsByStatus(DocumentStatus status){
        List<Document> documents = new ArrayList<>();
        try {
            String query = "SELECT * FROM document WHERE status = ? ORDER BY creation_date DESC";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, status.toString());
            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                documents.add(createDocumentFromResultSet(rs));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Errore durante getDocumentsByStatus(status=" + status + ")", e);
        }
        return documents;
    }

    public List<Document> searchDocuments(DocumentSearchCriteria criteria){
        List<Document> documents = new ArrayList<>();
        try {
            StringBuilder queryBuilder = new StringBuilder("SELECT * FROM document WHERE 1=1");
            List<Object> parameters = new ArrayList<>();

            if (criteria.getDocumentName().isPresent()) {
                queryBuilder.append(" AND file_name LIKE ?");
                parameters.add("%" + criteria.getDocumentName().get() + "%");
            }
            if (criteria.getAuthorId().isPresent()) {
                queryBuilder.append(" AND author_id = ?");
                parameters.add(criteria.getAuthorId().get());
            }
            if (criteria.getStatus().isPresent()) {
                queryBuilder.append(" AND status = ?");
                parameters.add(criteria.getStatus().get().toString());
            }
            if (criteria.getFormat().isPresent()) {
                queryBuilder.append(" AND file_format = ?");
                parameters.add(criteria.getFormat().get().toString());
            }

            PreparedStatement ps = connection.prepareStatement(queryBuilder.toString());
            for (int i = 0; i < parameters.size(); i++) {
                ps.setObject(i + 1, parameters.get(i));
            }

            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                documents.add(createDocumentFromResultSet(rs));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Errore durante searchDocuments()", e);
        }
        return documents;
    }

}
