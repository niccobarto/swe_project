// java
package ORM;

import DomainModel.*;

import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class DocumentDAO extends BaseDAO {
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
            String query = "INSERT INTO document (file_name,description,status,period,file_format,filepath,author_id,creation_date) VALUES(?,?,?,?,?,?,?,?,?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, fileName);
            statement.setString(2, description);
            statement.setString(3,DocumentStatus.DRAFT.toString());
            statement.setString(4,documentPeriod);
            statement.setString(5,documentFormat.toString());
            statement.setString(6,"file_db_path");
            statement.setInt(7,author.getId());
            statement.setDate(8, java.sql.Date.valueOf(java.time.LocalDate.now()));
            statement.execute();
            statement.close();
        }catch(SQLException e){
            // loggare eccezione se necessario
        }
    }

    private void setNullable(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, java.sql.Types.VARCHAR);
        } else {
            statement.setString(index, value);
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
            String qDel = "DELETE FROM document WHERE document_id=?";
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
            // gestire / loggare eccezione secondo necessità
        }
    }

    public void addTag(Tag t,int documentId){
        try{
            // inserisce il tag nella tabella tag (colonne esplicite)
            String query = "INSERT INTO tag (tag_label, description) VALUES(?,?)";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, t.getLabel());
            ps.setString(2, t.getDescription());
            ps.execute();
            ps.close();

            // link tra documento e tag (se la tabella DocumentTags esiste)
            try {
                String linkQuery = "INSERT INTO DocumentTags (document_id, tag_label) VALUES(?,?)";
                PreparedStatement linkStmt = connection.prepareStatement(linkQuery);
                linkStmt.setInt(1, documentId);
                linkStmt.setString(2, t.getLabel());
                linkStmt.execute();
                linkStmt.close();
            } catch (SQLException ignore) {
                // se la tabella o la colonna hanno nomi diversi, gestire altrove
            }

        }catch (SQLException e){
            // gestire / loggare eccezione secondo necessità
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
            // gestire / loggare eccezione secondo necessità
        }
    }

    public void updateDocumentStatus(int docId,DocumentStatus status){
        try {
            String query = "UPDATE document SET status = ? WHERE document_id = ?";
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
            // gestire / loggare eccezione secondo necessità
        }
    }
    private Document createDocumentFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("document_id");
        String title = rs.getString("title");
        String description = rs.getString("description");
        DocumentStatus status = DocumentStatus.valueOf(rs.getString("status"));
        String fileFormat = rs.getString("file_format");
        int authorId = rs.getInt("author_id");
        String filePath = rs.getString("file_path");
        String fileName = rs.getString("file_name");
        Date creationDate = rs.getDate("creation_date");
        User author = new UserDAO().getUserById(authorId);
        Document document = new Document(id, title, description, DocumentFormat.valueOf(fileFormat), author, filePath, fileName, creationDate);
        document.setStatus(status);
        return document;
    }
    public Document getDocumentById(int documentId){
        Document document = null;
        try {
            String query = "SELECT * FROM document WHERE document_id = ?";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setInt(1, documentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                document=createDocumentFromResultSet(rs);
            }
            rs.close();
            ps.close();
        }catch (SQLException e){
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
            // gestire / loggare eccezione secondo necessità
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
                documents.add(getDocumentById(rs.getInt("document_id")));
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            // gestire / loggare eccezione secondo necessità
        }
        return documents;
    }
}
