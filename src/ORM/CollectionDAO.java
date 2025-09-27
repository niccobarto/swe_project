package ORM;

import DomainModel.DocumentRelationType;
import DomainModel.User;

import DomainModel.Collection;
import java.util.List;

public class CollectionDAO extends BaseDAO {
    public CollectionDAO() {}
    public void addCollection(String collectionName, String description, User user){}
    public void removeCollection(int collectionId){}
    public void addDocumentToCollection(int documentId, int collectionId){}
    public void removeDocumentFromCollection(int documentId, int collectionId){}
    public List<Collection> getCollectionsByUser(int userId){return null;}
    public List<Collection> getAllCollection(int userId){return null;}
}
