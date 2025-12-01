package ORM;

import DomainModel.Document;
import DomainModel.PublishRequest;
import DomainModel.RequestStatus;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PublishRequestDAO extends BaseDAO {
    private static final Logger LOGGER = Logger.getLogger(PublishRequestDAO.class.getName());

    public PublishRequestDAO() {
        super();
    }
    public void addRequest(Document doc) {
        try{
            String query="INSERT INTO publish_request (document_id,request_status,date_request) VALUES(?,?,?)";
            PreparedStatement ps=connection.prepareStatement(query);
            ps.setInt(1,doc.getId());
            ps.setString(2, RequestStatus.PENDING.toString());
            ps.setDate(3, java.sql.Date.valueOf(java.time.LocalDate.now()));
            ps.executeUpdate();
            ps.close();

        }catch (SQLException e){
            LOGGER.log(Level.SEVERE, "Errore durante addRequest(docId=" + (doc!=null?doc.getId():null) + ")", e);
        }
    }
    public void removeRequest(int docId) {
        try{
            String query="DELETE FROM publish_request WHERE document_id=?";
            PreparedStatement ps=connection.prepareStatement(query);
            ps.setInt(1,docId);
            ps.executeUpdate();
            ps.close();
        }catch (SQLException e){
            LOGGER.log(Level.SEVERE, "Errore durante removeRequest(docId=" + docId + ")", e);
        }
    }

    public void updateRequestStatus(int docId,int moderator_id,RequestStatus status){
        try{
            String query="UPDATE publish_request SET request_status=?,date_result=? WHERE document_id=?";
            PreparedStatement ps2=connection.prepareStatement(query);
            ps2.setString(1,status.toString());
            ps2.setDate(2,java.sql.Date.valueOf(java.time.LocalDate.now()));
            ps2.setInt(3,docId);
            ps2.executeUpdate();
            ps2.close();
        }catch (SQLException e){
            LOGGER.log(Level.SEVERE, "Errore durante updateRequestStatus(docId=" + docId + ", moderatorId=" + moderator_id + ")", e);
        }
    }
    public List<PublishRequest> getRequestByModerator(int moderatorId){
        List<PublishRequest> publishRequests=new ArrayList<PublishRequest>();
        try{
            String query="SELECT * FROM publish_request WHERE moderator_id=?";
            PreparedStatement ps=connection.prepareStatement(query);
            ps.setInt(1, moderatorId);
            ResultSet rs=ps.executeQuery();
            while(rs.next()){
                publishRequests.add(createRequestFromResultSet(rs));
            }
            rs.close();
            ps.close();
        }catch (SQLException e){
            LOGGER.log(Level.SEVERE, "Errore durante getRequestByModerator(moderatorId=" + moderatorId + ")", e);
        }
        return publishRequests;
    }
    public List<PublishRequest> getRequestsByAuthor(int userId){
        List<PublishRequest> publishRequests=new ArrayList<PublishRequest>();
        try{
            String query="SELECT * FROM publish_request pr JOIN document ON pr.id=d.id WHERE d.author_id=?";
            PreparedStatement ps=connection.prepareStatement(query);
            ps.setInt(1,userId);
            ResultSet rs=ps.executeQuery();
            while(rs.next()){
                publishRequests.add(createRequestFromResultSet(rs));
            }
            rs.close();
            ps.close();
        }catch (SQLException e){
            LOGGER.log(Level.SEVERE, "Errore durante getRequestsByAuthor(userId=" + userId + ")", e);
        }
        return publishRequests;
    }
    public List<PublishRequest> getRequestsByStatus(RequestStatus status){
        List<PublishRequest> publishRequests=new ArrayList<PublishRequest>();
        try{
            String query="SELECT * FROM publish_request WHERE request_status=?";
            PreparedStatement ps=connection.prepareStatement(query);
            ps.setString(1,status.toString());
            ResultSet rs=ps.executeQuery();
            while(rs.next()){
                publishRequests.add(createRequestFromResultSet(rs));
            }
            rs.close();
            ps.close();
        }catch (SQLException e){
            LOGGER.log(Level.SEVERE, "Errore durante getRequestsByStatus(status=" + status + ")", e);
        }
        return publishRequests;
    }

//-----private Methods-----
    private PublishRequest createRequestFromResultSet(ResultSet rs) throws SQLException {
        int id=rs.getInt("id");
        String motivation=rs.getString("denial_motivation");
        Date dateRequest=rs.getDate("date_request");
        Date dateResult=rs.getDate("date_result");
        Document document=new DocumentDAO().getDocumentById(rs.getInt("document_id"));
        PublishRequest pr=new PublishRequest(id,motivation,dateRequest,dateResult,document);
        pr.setStatus(RequestStatus.valueOf(rs.getString("request_status")));
        return pr;
    }

}
