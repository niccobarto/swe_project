package ORM;

import DomainModel.User;
import DomainModel.DocumentStatus;
import DomainModel.Document;
import DomainModel.DocumentFormat;
import DomainModel.Tag;

import java.util.List;

public class DocumentDAO extends BaseDAO {
    public DocumentDAO(){}
    public void addComment(User author, String description, String documentPeriod, DocumentFormat format, String documentType, String filePath, String fileName){}
    public void removeDocument(int documentId){}
    public void addTag(Tag t){}
    public void removeTag(int tagId){}
    public void updateDocumentStatus(int docId,DocumentStatus status){}
    public List<Document> getDocumentsByAuthor(int userId){return null;}
    public List<Document> getAllDocuments(){return null;}
}
