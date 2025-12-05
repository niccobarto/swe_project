package ORM;

import DomainModel.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TagChangeRequestDAO extends BaseDAO {

    private static final Logger LOGGER = Logger.getLogger(TagChangeRequestDAO.class.getName());

    public TagChangeRequestDAO() { super(); }

    //Crea una richiesta PENDING tag esistente (ADD O REMOVE)
    public void addRequestForExistingTag(TagChangeRequest req) {
        try{
            req.validateOrThrow();
            if (!req.isForExistingTag())
                throw new IllegalArgumentException("Expected existingTagLabel (not proposedLabel)"); //per confermare caso d'uso

            String q = "INSERT INTO tag_change_request " +
                    "(document_id, operation, existing_tag_label, proposed_label, status, date_request) " +
                    "VALUES (?, ?, ?, NULL, ?, ?) RETURNING id";
            PreparedStatement ps = connection.prepareStatement(q);
            ps.setInt(1, req.getDocument().getId());
            ps.setString(2, req.getOperation().toString());
            ps.setString(3, req.getExistingTagLabel());
            ps.setString(4, req.getStatus().toString());
            ps.setDate(5, new java.sql.Date(req.getDateRequest().getTime()));

            ResultSet rs = ps.executeQuery();
            if (rs.next())
                req.setId(rs.getInt(1));
            rs.close();
            ps.close();
        }catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error during addRequestForExistingTag(docId="
                    + (req.getDocument() != null ? req.getDocument().getId() : null)
                    + ", label=" + req.getExistingTagLabel() + ")", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Validation failed in addRequestForExistingTag()", e);
        }
    }

    //crea richiesta PENDING per un nuovo label (ADD)
    public void addRequestForNewTag(TagChangeRequest req) {
        try{
            req.validateOrThrow();
            if (!req.isForNewLabel())
                throw new IllegalArgumentException("Expected proposedLabel (not existingTagLabel)");

            String q = "INSERT INTO tag_change_request " +
                    "(document_id, operation, existing_tag_label, proposed_label, status, date_request) " +
                    "VALUES (?, ?, NULL, ?, ?, ?) RETURNING id";
            PreparedStatement ps = connection.prepareStatement(q);
            ps.setInt(1, req.getDocument().getId());
            ps.setString(2, req.getOperation().toString());
            ps.setString(3, req.getProposedLabel());
            ps.setString(4, req.getStatus().toString());
            ps.setDate(5, new java.sql.Date(req.getDateRequest().getTime()));

            ResultSet rs = ps.executeQuery();
            if(rs.next())
                req.setId(rs.getInt(1));
            rs.close();
            ps.close();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE,
                    "Error during addRequestForNewLabel(docId=" +
                            (req.getDocument() != null ? req.getDocument().getId() : null) +
                            ", proposedLabel=" + req.getProposedLabel() + ")", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Validation failed in addRequestForNewLabel()", e);
        }
    }

    //aggiorna stato e imposta date_result
    public void updateStatus(int requestId,int moderatorId, RequestStatus newStatus) {
        try{
            String q = "UPDATE tag_change_request " +
                    "SET status = ?, date_result = ?, moderator_id = ? " +
                    "WHERE id = ?";
            PreparedStatement ps = connection.prepareStatement(q);
            ps.setString(1, newStatus.toString());
            ps.setDate(2, new java.sql.Date(System.currentTimeMillis()));
            ps.setInt(3, moderatorId);
            ps.setInt(4, requestId);

            int affected = ps.executeUpdate();
            ps.close();

            if (affected == 0)
                LOGGER.log(Level.WARNING, "updateStatus affected 0 rows (requestId=" + requestId + ")");
        }catch (SQLException e) {
            LOGGER.log(Level.SEVERE,
                    "Error during updateStatus(requestId=" + requestId + ", status=" + newStatus + ")", e);
        }
    }

    public TagChangeRequest getById(int id) {
        try{
            String q = "SELECT * FROM tag_change_request WHERE id = ?";
            PreparedStatement ps = connection.prepareStatement(q);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            TagChangeRequest out = null;
            if(rs.next())
                out= map(rs);
            rs.close();
            ps.close();
            return out;
        } catch (SQLException e){
            LOGGER.log(Level.SEVERE, "Error during getById(id=" + id + ")", e);
            return null;
        }
    }

    public List<TagChangeRequest> getPending() {
        List<TagChangeRequest> out = new ArrayList<>();
        try {
            String q = "SELECT * FROM tag_change_request " +
                    "WHERE status = ? " +
                    "ORDER BY date_request ASC";
            PreparedStatement ps = connection.prepareStatement(q);
            ps.setString(1, RequestStatus.PENDING.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                out.add(map(rs));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error during getPending()", e);
        }
        return out;
    }

    private TagChangeRequest map(ResultSet rs) throws SQLException {
        int documentId = rs.getInt("document_id");
        Document document = new DocumentDAO().getDocumentById(documentId);

        TagChangeRequest r = new TagChangeRequest();
        r.setId(rs.getInt("id"));
        r.setStatus(RequestStatus.valueOf(rs.getString("status")));
        r.setDateRequest(rs.getDate("date_request"));
        r.setDateResult(rs.getDate("date_result"));
        r.setOperation(TagChangeOperation.valueOf(rs.getString("operation")));
        r.setDocument(document);
        r.setExistingTagLabel(rs.getString("existing_tag_label"));
        r.setProposedLabel(rs.getString("proposed_label"));
        int moderatorId = rs.getInt("moderator_id");
        if (!rs.wasNull()) {
            User moderator = new UserDAO().getUserById(moderatorId);
            r.setModerator(moderator);
        }
        return r;
    }

    public List<TagChangeRequest> getRequestByModerator(int moderatorId) {
        List<TagChangeRequest> requests = new ArrayList<>();

        try {
            String q = "SELECT * FROM tag_change_request " +
                    "WHERE moderator_id = ? " +
                    "ORDER BY date_result DESC";
            PreparedStatement ps = connection.prepareStatement(q);
            ps.setInt(1, moderatorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                requests.add(map(rs));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE,
                    "Error during getRequestByModerator(moderatorId=" + moderatorId + ")", e);
        }

        return requests;
    }

    //lista richieste fatte da autore
    public List<TagChangeRequest> getByAuthor(int userId) {
        List<TagChangeRequest> out = new ArrayList<>();
        try {
            String q = "SELECT r.* " +
                    "FROM tag_change_request r " +
                    "JOIN document d ON d.id = r.document_id " +
                    "WHERE d.author_id = ? " +
                    "ORDER BY r.date_request DESC";
            PreparedStatement ps = connection.prepareStatement(q);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) out.add(map(rs));
            rs.close();
            ps.close();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error during getByAuthor(userId=" + userId + ")", e);
        }
        return out;
    }

    public List<TagChangeRequest> getRequestsByDocument(int documentId) {
        List<TagChangeRequest> out = new ArrayList<>();
        try {
            String q = "SELECT * FROM tag_change_request " +
                    "WHERE document_id = ? " +
                    "ORDER BY date_request ASC";
            PreparedStatement ps = connection.prepareStatement(q);
            ps.setInt(1, documentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                out.add(map(rs));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE,
                    "Error during getRequestsByDocument(documentId=" + documentId + ")", e);
        }
        return out;
    }


    //controlla se c'è un'altra richiesta pending al documento preso in considerazione
    public boolean existsPendingDuplicate(int documentId,
                                          TagChangeOperation op,
                                          String existingTagLabel,
                                          String proposedLabel) {
        try {
            String q = "SELECT 1 FROM tag_change_request " +  //coalesce(x,y) se x non è null restituisci x altrimenti y
                    "WHERE document_id=? AND operation=? AND status=? " +
                    "AND COALESCE(existing_tag_label,'') = COALESCE(?, '') " +
                    "AND COALESCE(proposed_label,'') = COALESCE(?, '') " +
                    "LIMIT 1";
            PreparedStatement ps = connection.prepareStatement(q);
            ps.setInt(1, documentId);
            ps.setString(2, op.toString());
            ps.setString(3, RequestStatus.PENDING.toString());
            ps.setString(4, existingTagLabel);
            ps.setString(5, proposedLabel);
            ResultSet rs = ps.executeQuery();
            boolean exists = rs.next();
            rs.close();
            ps.close();
            return exists;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE,
                    "Error during existsPendingDuplicate(docId=" + documentId +
                            ", op=" + op + ", existing=" + existingTagLabel +
                            ", proposed=" + proposedLabel + ")", e);
            return false;
        }
    }



}
