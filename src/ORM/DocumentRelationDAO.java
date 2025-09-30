package ORM;
import DomainModel.DocumentRelation;
import DomainModel.Document;
import DomainModel.DocumentRelationType;

import java.util.ArrayList;
import java.util.List;

public class DocumentRelationDAO extends BaseDAO {

    public DocumentRelationDAO() {}
    public void addDocumentRelation(Document source,Document destination,DocumentRelationType type){
    }
    public void removeDocumentRelation(int sourceId,int destinationId){    }
    public void updateDocumentRelation(int sourceId,int destinationId,DocumentRelationType new_type){    }
    public List<DocumentRelation> getRelationsFromDocument(int documentId){  return new ArrayList<>();  }

}
