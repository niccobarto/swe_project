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

    public ArrayList<Document> searchDestinationRelations(DocumentRelationType type){
        DocumentRelationDAO relDAO = new DocumentRelationDAO();
        try{
            List<DocumentRelation> all = relDAO.getRelationsFromDocument(selected.getId());
            if (all == null) all = List.of(); //lista vuota immutabile per evitare eccezione null se DAO dovesse restituire null

            List <Document> dests= all.stream()
                    .filter(r -> r.getSource() != null && r.getSource().getId() == selected.getId())
                    .filter(r -> type == null || r.getRelationType() == type)
                    .map(DocumentRelation::getDestination)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return new ArrayList<>(dests);

        }catch (Exception e){
            System.err.println("searchDestinationRelations failed: selected=" + selected.getId() + ", type=" + type);
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public ArrayList<Document> searchSourceRelations(DocumentRelationType type){
        DocumentRelationDAO relDAO = new DocumentRelationDAO();
        try {
            List<DocumentRelation> all = relDAO.getRelationsFromDocument(selected.getId());
            if (all == null) all = List.of();

            List<Document> sources = all.stream()
                    .filter(r -> r.getDestination() != null && r.getDestination().getId() == selected.getId())
                    .filter(r -> type == null || r.getRelationType() == type)
                    .map(DocumentRelation::getSource)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return new ArrayList<>(sources);
        } catch (Exception e) {
            System.err.println("searchSourceRelations failed: selected=" + selected.getId() + ", type=" + type);
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

}
