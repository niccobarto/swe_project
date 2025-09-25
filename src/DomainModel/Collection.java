package DomainModel;

import java.util.List;

public class Collection {
    private String name;
    private String description;
    private User author;
    private List<Document> documents;

    public Collection(String name, String description, User author, List<Document> documents) {
        this.name = name;
        this.description = description;
        this.author = author;
        this.documents = documents;
    }

    public void addDocument(Document document) {
        this.documents.add(document);
    }

    public void removeDocument(Document document) {
        this.documents.remove(document);
    }


}
