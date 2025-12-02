package BusinessLogic;

import ORM.DocumentRelationDAO;
import DomainModel.Document;
import DomainModel.DocumentRelation;
import DomainModel.DocumentRelationType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class RelationController {

    private final Document selected;

    public RelationController(Document selected) {
        this.selected = Objects.requireNonNull(selected, "selected cannot be null");
    }

    public Document getSelected() { return selected; }

    public List<DocumentRelation> searchSourceRelations(DocumentRelationType type){
        DocumentRelationDAO relDAO = new DocumentRelationDAO();
        try {
            return relDAO.getSourceRelationDocument(selected.getId(), type);
        } catch (Exception e) {
            System.err.println("searchSourceRelations failed: selected=" + selected.getId() + ", type=" + type);
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<DocumentRelation> searchDestinationRelations(DocumentRelationType type){
        DocumentRelationDAO relDAO = new DocumentRelationDAO();
        try{
            return relDAO.getDestinationRelationDocument(selected.getId(),type);
        }catch (Exception e){
            System.err.println("searchDestinationRelations failed: selected=" + selected.getId() + ", type=" + type);
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void addRelation(Document destination, DocumentRelationType type){
        DocumentRelationDAO relDAO = new DocumentRelationDAO();
        try{
            if(destination == null)
                throw new IllegalArgumentException("Source or Destination document is null");
            if(selected.getId() == destination.getId())
                throw new IllegalArgumentException("Source and Destination documents are the same");
            if(type == null)
                throw new IllegalArgumentException("Relation type is null");
            relDAO.addDocumentRelation(selected.getId(), destination.getId(), type, selected.getAuthor().getId() == destination.getAuthor().getId());
        }catch (Exception e){
            System.err.println("addRelation failed: source=" + selected.getId() +
                    ", destination=" + (destination != null ? destination.getId() : "null") +
                    ", type=" + type);
            e.printStackTrace();
        }
    }

    public void removeRelation(Document destination){
        DocumentRelationDAO relDAO = new DocumentRelationDAO();
        try{
            if(destination == null)
                throw new IllegalArgumentException("Source or Destination document is null");
            relDAO.removeDocumentRelation(selected.getId(), destination.getId());
        }catch (Exception e){
            System.err.println("removeRelation failed: source=" + selected.getId() +
                    ", destination=" + (destination != null ? destination.getId() : "null"));
            e.printStackTrace();
        }
    }

    public void updateRelationType(Document destination, DocumentRelationType new_type){
        DocumentRelationDAO relDAO = new DocumentRelationDAO();
        try{
            if(destination == null)
                throw new IllegalArgumentException("Source or Destination document is null");
            if(selected.getId() == destination.getId())
                throw new IllegalArgumentException("Source and Destination documents are the same");
            if(new_type == null)
                throw new IllegalArgumentException("Relation type is null");
            relDAO.updateDocumentRelation(selected.getId(), destination.getId(), new_type);
        }catch (Exception e){
            System.err.println("updateRelation failed: source=" + selected.getId() +
                    ", destination=" + (destination != null ? destination.getId() : "null") +
                    ", new_type=" + new_type);
            e.printStackTrace();
        }
    }

    public List<DocumentRelation> searchDestinationRelationsByConfirm(boolean confirmed){
        DocumentRelationDAO relDAO = new DocumentRelationDAO();
        try{
            return relDAO.getDestinationRelationsByConfirm(selected.getId(), confirmed);
        } catch (Exception e) {
            System.err.println("searchDestinationRelationsByConfirm failed: selected=" + selected.getId() + ", isConfirmed=" + confirmed);
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void setRelationConfirmed(Document source, boolean confirmed) {
        DocumentRelationDAO relDAO = new DocumentRelationDAO();
        try {
            if (source == null)
                throw new IllegalArgumentException("Source or Destination document is null");
            relDAO.setRelationConfirmed(source.getId(), selected.getId(), confirmed);
        } catch (Exception e) {
            System.err.println("setRelationConfirmed failed: source=" + (source != null ? source.getId() : "null") +
                    ", destination=" + selected.getId() +
                    ", confirmed=" + confirmed);
            e.printStackTrace();
        }
    }
}
