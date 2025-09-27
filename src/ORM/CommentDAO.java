package ORM;
import DomainModel.Comment;
import DomainModel.Document;
import DomainModel.User;

import java.util.List;

public class CommentDAO extends BaseDAO {
    public CommentDAO(){}
    public void addComment(String text, User commentAuthor,Document document){}
    public void removeComment(int commentId){}
    public List<Comment> getCommentByAuthor(int userId){return null;}
    public List<Comment> getCommentByDocument(int documentId){return null;}
}
