package DomainModel;

import java.util.List;

public class Collection {
    private int id;
    private String name;
    private String description;
    private User author;
    private List<Document> documents;

    public Collection(int id, String name, String description, User author, List<Document> documents) {
        this.id=id;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }
}
