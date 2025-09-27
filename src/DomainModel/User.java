package DomainModel;

import java.util.List;

public class User {
    private int id;
    private String name;
    private String email;
    private List<Document> favDocs;
    private List<Collection> favCollections;

    public User(int id,String name, String email, List<Document> favDocs, List<Collection> favCollections) {
        this.id=id;
        this.name = name;
        this.email = email;
        this.favDocs = favDocs;
        this.favCollections = favCollections;
    }
}
