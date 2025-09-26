package DomainModel;

import java.util.List;

public class User {
    private String name;
    private String email;
    private List<Document> favDocs;
    private List<Collection> favCollections;

    public User(String name, String email, List<Document> favDocs, List<Collection> favCollections) {
        this.name = name;
        this.email = email;
        this.favDocs = favDocs;
        this.favCollections = favCollections;
    }
}
