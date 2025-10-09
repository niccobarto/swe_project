package ORM;

import DomainModel.Document;
import DomainModel.PublishRequest;
import DomainModel.RequestStatus;
import DomainModel.User;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PublishRequestDAO extends BaseDAO {

    public PublishRequestDAO() {
        super();
    }
    public void addRequest(Document doc) {
        try{
            String query="INSERT INTO publish_request (document_id,request_status,date_request,author_id) VALUES(?,?,?,?)";
            PreparedStatement ps=connection.prepareStatement(query);
            ps.setInt(1,doc.getId());
            ps.setString(2, RequestStatus.PENDING.toString());
            ps.setDate(3, java.sql.Date.valueOf(java.time.LocalDate.now()));
            ps.setInt(4,doc.getAuthor().getId());
            ps.execute();
            ps.close();

        }catch (SQLException e){}
    }
    public void removeRequest(int docId) {
        try{
            String query="DELETE FROM publish_request WHERE document_id=?";
            PreparedStatement ps=connection.prepareStatement(query);
            ps.setInt(1,docId);
            ps.execute();
            ps.close();
        }catch (SQLException e){}
    }
    //Todo da decidere se permettere di modificare la richiesta
    // anche ad altri moderatori.
    // In caso per ogni aggiornamento va creata una nuova istanza di
    // PublishRequestManaged nel DB.
    public void updateRequestStatus(int docId,int moderator_id,RequestStatus status){
        try{
            String query = "INSERT INTO publish_request_managed (publish_request_id,moderator_id) VALUES(?,?)";
            PreparedStatement ps=connection.prepareStatement(query);
            ps.setInt(1,docId);
            ps.setInt(2,moderator_id);
            ps.execute();
            ps.close();
            String query2="UPDATE publish_request SET request_status=?,date_result=? WHERE document_id=?";
            PreparedStatement ps2=connection.prepareStatement(query2);
            ps2.setString(1,status.toString());
            ps2.setInt(2,docId);
            ps2.setDate(3,java.sql.Date.valueOf(java.time.LocalDate.now()));
            ps2.execute();
            ps2.close();
        }catch (SQLException e){}
    }
    public List<PublishRequest> getRequestByModerator(int mdoeratorId){
        List<PublishRequest> publishRequests=new ArrayList<PublishRequest>();
        try{
            String query="SELECT * FROM publish_request_managed WHERE moderator_id=?";
            PreparedStatement ps=connection.prepareStatement(query);
            ps.setInt(1,mdoeratorId);
            ResultSet rs=ps.executeQuery();
            while(rs.next()){
                publishRequests.add(createRequestFromResultSet(rs));
            }
        }catch (SQLException e){}
        return publishRequests;
    }
    public List<PublishRequest> getRequestsByAuthor(int userId){
        List<PublishRequest> publishRequests=new ArrayList<PublishRequest>();
        try{
            String query="SELECT * FROM publish_request WHERE author_id=?";
            PreparedStatement ps=connection.prepareStatement(query);
            ps.setInt(1,userId);
            ResultSet rs=ps.executeQuery();
            while(rs.next()){
                publishRequests.add(createRequestFromResultSet(rs));
            }
        }catch (SQLException e){}
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
        }catch (SQLException e){}
        return publishRequests;
    }

//-----private Methods-----
    private PublishRequest createRequestFromResultSet(ResultSet rs) throws SQLException {
        int id=rs.getInt("id");
        String motivation=rs.getString("motivation");
        Date dateRequest=rs.getDate("date_request");
        Date dateResult=rs.getDate("date_result");
        Document document=new DocumentDAO().getDocumentById(rs.getInt("document_id"));
        PublishRequest pr=new PublishRequest(id,motivation,dateRequest,dateResult,document);
        pr.setStatus(RequestStatus.valueOf(rs.getString("request_status")));
        return pr;
    }

}
