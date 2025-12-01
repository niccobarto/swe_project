package DomainModel;

public class DocumentRelation {
    private Document source;
    private Document destination;
    private DocumentRelationType relationType;

    public DocumentRelation(Document source, Document destination, DocumentRelationType relationType) {
        this.source = source;
        this.destination = destination;
        this.relationType = relationType;
    }

    public Document getSource() { return source; }

    public Document getDestination() { return destination; }

    public DocumentRelationType getRelationType() { return relationType; }

}
