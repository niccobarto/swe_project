package ORM;
import DomainModel.Comment;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CommentDAO extends BaseDAO {
    private static final Logger LOGGER = Logger.getLogger(CommentDAO.class.getName());
    public CommentDAO(){
        super();
    }
    public void addComment(String text, int commentAuthorId,int documentId){
        try{
            String query = "INSERT INTO comment (user_id,document_id,text,date) VALUES(?,?,?,?)";
            var statement = connection.prepareStatement(query);
            statement.setInt(1, commentAuthorId);
            statement.setInt(2, documentId);
            statement.setString(3,text);
            statement.setDate(4, java.sql.Date.valueOf(java.time.LocalDate.now()));
            statement.executeUpdate();
            statement.close();
        }catch(Exception e){
            LOGGER.log(Level.SEVERE, "Errore durante addComment(userId=" + commentAuthorId + ", docId=" + documentId + ")", e);
        }
    }
    public void removeComment(int commentId){
        try{
            String query = "DELETE FROM comment WHERE id = ?";
            var statement = connection.prepareStatement(query);
            statement.setInt(1, commentId);
            statement.executeUpdate();
            statement.close();
        }catch(Exception e){
            LOGGER.log(Level.SEVERE, "Errore durante removeComment(id=" + commentId + ")", e);
        }
    }
    public List<Comment> getCommentsByAuthor(int userId){
        List<Comment> comments = new ArrayList<>();
        try{
            String query = "SELECT * FROM comment WHERE user_id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, userId);
            ResultSet rs = statement.executeQuery();
            while(rs.next()){
                comments.add(createCommentFromResultSet(rs));
            }
            rs.close();
            statement.close();
            return comments;
        }catch (SQLException e){
            LOGGER.log(Level.SEVERE, "Errore durante getCommentByAuthor(userId=" + userId + ")", e);
        }
        return comments;
    }
    public List<Comment> getCommentsByDocument(int documentId){
        List<Comment> comments = new ArrayList<>();
        try{
            String query = "SELECT * FROM comment WHERE document_id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, documentId);
            ResultSet rs = statement.executeQuery();
            while(rs.next()){
                comments.add(createCommentFromResultSet(rs));
            }
            rs.close();
            statement.close();
            return comments;
        }catch (SQLException e){
            LOGGER.log(Level.SEVERE, "Errore durante getCommentByDocument(docId=" + documentId + ")", e);
        }
        return comments;
    }

    //private methods

    private Comment createCommentFromResultSet(java.sql.ResultSet rs) throws SQLException {
        return new Comment(
                rs.getInt("id"),
                new UserDAO().getUserById(rs.getInt("user_id")),
                new DocumentDAO().getDocumentById(rs.getInt("document_id")),
                rs.getString("text"),
                rs.getDate("date")
        );
    }
}
