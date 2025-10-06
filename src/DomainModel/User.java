package DomainModel;

import java.util.ArrayList;
import java.util.List;

public class User {
    private int id;
    private String name;
    private String email;
    private List<Document> favDocs;
    private List<Collection> favCollections;
    private boolean moderator;

    public User(int id,String name, String email) {
        this.id=id;
        this.name = name;
        this.email = email;
        this.favDocs = new ArrayList<>();
        this.favCollections = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isModerator() { return moderator; }

    public void setModerator(boolean moderator) { this.moderator = moderator; }

    public List<Document> getFavDocs() {
        return favDocs;
    }

    public void setFavDocs(List<Document> favDocs) {
        this.favDocs = favDocs;
    }

    public List<Collection> getFavCollections() {
        return favCollections;
    }

    public void setFavCollections(List<Collection> favCollections) {
        this.favCollections = favCollections;
    }
}
