package ORM;
import DomainModel.DocumentRelation;
import DomainModel.Document;
import DomainModel.DocumentRelationType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DocumentRelationDAO extends BaseDAO {
    private static final Logger LOGGER = Logger.getLogger(DocumentRelationDAO.class.getName());

    public DocumentRelationDAO() {
        super();
    }
    public void addDocumentRelation(int sourceId,int destinationId,DocumentRelationType type,boolean confirmed){
        try{
            String query="INSERT INTO document_relation (source_id,destination_id,relation_type,confirmed) VALUES (?,?,?,?)";
            PreparedStatement ps=connection.prepareStatement(query);
            ps.setInt(1,sourceId);
            ps.setInt(2,destinationId);
            ps.setString(3,type.toString());
            ps.setBoolean(4,confirmed);
            ps.executeUpdate();
            ps.close();
        }catch(SQLException e){
            LOGGER.log(Level.SEVERE, "Errore durante addDocumentRelation(sourceId=" + sourceId + ", destId=" + destinationId + ")", e);
        }
    }
    public void removeDocumentRelation(int sourceId,int destinationId){
        try{
            String query="DELETE FROM document_relation WHERE source_id=? AND destination_id=?";
            PreparedStatement ps=connection.prepareStatement(query);
            ps.setInt(1,sourceId);
            ps.setInt(2,destinationId);
            ps.executeUpdate();
            ps.close();
        }catch(SQLException e){
            LOGGER.log(Level.SEVERE, "Errore durante removeDocumentRelation(sourceId=" + sourceId + ", destId=" + destinationId + ")", e);
        }
    }
    public void updateDocumentRelation(int sourceId,int destinationId,DocumentRelationType new_type){
        try{
            String query="UPDATE document_relation SET relation_type=? WHERE source_id=? AND destination_id=?";
            PreparedStatement ps=connection.prepareStatement(query);
            ps.setString(1,new_type.toString());
            ps.setInt(2,sourceId);
            ps.setInt(3,destinationId);
            ps.executeUpdate();
            ps.close();
        }catch(SQLException e){
            LOGGER.log(Level.SEVERE, "Errore durante updateDocumentRelation(sourceId=" + sourceId + ", destId=" + destinationId + ")", e);
        }
    }
    //gives the relations about a type where the document is the source
    public List<DocumentRelation> getSourceRelationDocument(int documentId, DocumentRelationType type) {
        List<DocumentRelation> relations = new ArrayList<>();
        try {
            String query;
            PreparedStatement ps;
            if (type == null) {
                query = "SELECT * FROM document_relation WHERE source_id=?";
                ps = connection.prepareStatement(query);
                ps.setInt(1, documentId);
            } else {
                query = "SELECT * FROM document_relation WHERE source_id=? AND relation_type=?";
                ps = connection.prepareStatement(query);
                ps.setInt(1, documentId);
                ps.setString(2, type.toString());
            }
            var rs = ps.executeQuery();
            while (rs.next()) {
                relations.add(createDocumentRelationFromResultSet(rs));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Errore durante getSourceRelationDocument(documentId=" + documentId + ", type=" + type + ")", e);
        }
        return relations;
    }
    public List<DocumentRelation> getDestinationRelationDocument(int documentId, DocumentRelationType type) {
        List<DocumentRelation> relations = new ArrayList<>();
        try {
            String query;
            PreparedStatement ps;
            if (type == null) {
                query = "SELECT * FROM document_relation WHERE destination_id=?";
                ps = connection.prepareStatement(query);
                ps.setInt(1, documentId);
            } else {
                query = "SELECT * FROM document_relation WHERE destination_id=? AND relation_type=?";
                ps = connection.prepareStatement(query);
                ps.setInt(1, documentId);
                ps.setString(2, type.toString());
            }
            var rs = ps.executeQuery();
            while (rs.next()) {
                relations.add(createDocumentRelationFromResultSet(rs));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Errore durante getDestinationRelationDocument(documentId=" + documentId + ", type=" + type + ")", e);
        }
        return relations;
    }

    public List<DocumentRelation> getAllSourceRelationDocument(int documentId) {
        List<DocumentRelation> relations = new ArrayList<>();
        try {
            String query = "SELECT * FROM document_relation WHERE source_id=?";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setInt(1, documentId);
            var rs = ps.executeQuery();
            while (rs.next()) {
                relations.add(createDocumentRelationFromResultSet(rs));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Errore durante getAllSourceRelationDocument(documentId=" + documentId + ")", e);
        }
        return relations;
    }
    public List<DocumentRelation> getAllDestinationRelationDocument(int documentId) {
        List<DocumentRelation> relations = new ArrayList<>();
        try {
            String query = "SELECT * FROM document_relation WHERE destination_id=?";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setInt(1, documentId);
            var rs = ps.executeQuery();
            while (rs.next()) {
                relations.add(createDocumentRelationFromResultSet(rs));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Errore durante getAllDestinationRelationDocument(documentId=" + documentId + ")", e);
        }
        return relations;
    }

    //Trova le relazioni dove quello passato è source e il campo confirmed è uguale a quello passato
    public List<DocumentRelation> getSourceRelationsByConfirm(int documentId,boolean confirmed){
        List<DocumentRelation> relations = new ArrayList<>();
        try {
            String query = "SELECT * FROM document_relation WHERE source_id=? AND confirmed=?";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setInt(1, documentId);
            ps.setBoolean(2, confirmed);
            var rs = ps.executeQuery();
            while (rs.next()) {
                relations.add(createDocumentRelationFromResultSet(rs));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Errore durante getSourceRelationsByConfirm(documentId=" + documentId + ", confirmed=" + confirmed + ")", e);
        }
        return relations;
    }
    public List<DocumentRelation> getDestinationRelationsByConfirm(int documentId,boolean confirmed){
        List<DocumentRelation> relations = new ArrayList<>();
        try {
            String query = "SELECT * FROM document_relation WHERE destination_id=? AND confirmed=?";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setInt(1, documentId);
            ps.setBoolean(2, confirmed);
            var rs = ps.executeQuery();
            while (rs.next()) {
                relations.add(createDocumentRelationFromResultSet(rs));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Errore durante getDestinationRelationsByConfirm(documentId=" + documentId + ", confirmed=" + confirmed + ")", e);
        }
        return relations;
    }

    public void setRelationConfirmed(int sourceId,int destinationId,boolean confirmed){
        try{
            if (confirmed){
                String query="UPDATE document_relation SET confirmed=? WHERE source_id=? AND destination_id=?";
                PreparedStatement ps=connection.prepareStatement(query);
                ps.setBoolean(1,confirmed);
                ps.setInt(2,sourceId);
                ps.setInt(3,destinationId);
                ps.executeUpdate();
                ps.close();
            }
            else{
                this.removeDocumentRelation(sourceId,destinationId);
            }

        }catch(SQLException e){
            LOGGER.log(Level.SEVERE, "Errore durante setRelationConfirmed(sourceId=" + sourceId + ", destId=" + destinationId + ", confirmed=" + confirmed + ")", e);
        }
    }
    //----private methods----//
    private DocumentRelation createDocumentRelationFromResultSet(ResultSet rs) throws SQLException {
        int sourceId = rs.getInt("source_id");
        int destinationId = rs.getInt("destination_id");
        Document source=new DocumentDAO().getDocumentById(sourceId);
        Document destination=new DocumentDAO().getDocumentById(destinationId);
        String relationType = rs.getString("relation_type");
        boolean confirmed = rs.getBoolean("confirmed");
        return new DocumentRelation(source, destination, DocumentRelationType.valueOf(relationType),confirmed);
    }

}
