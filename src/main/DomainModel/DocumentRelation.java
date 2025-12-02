package DomainModel;

public class DocumentRelation {
    private Document source;
    private Document destination;
    private DocumentRelationType relationType;
    private boolean confirmed;

    public DocumentRelation(Document source, Document destination, DocumentRelationType relationType,boolean confirmed) {
        this.source = source;
        this.destination = destination;
        this.relationType = relationType;
        this.confirmed=confirmed;
    }

    public Document getSource() { return source; }

    public Document getDestination() { return destination; }

    public DocumentRelationType getRelationType() { return relationType; }

    public boolean isConfirmed() { return confirmed; }

}
