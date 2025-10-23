package DomainModel;

import java.util.ArrayList;
import java.util.List;

public class User {
    private int id;
    private String name;
    private String email;
    private String password;
    private List<Document> favDocs;
    private List<Collection> favCollections;
    private boolean isModerator;
    private boolean isAdmin;

    public User(int id,String name, String surname,String email, String password,boolean isModerator,boolean isAdmin) {
        this.id=id;
        this.name = name+surname;
        this.email = email;
        this.password = password;
        this.favDocs = new ArrayList<>();
        this.favCollections = new ArrayList<>();
        this.isModerator=isModerator;
        this.isAdmin=isAdmin;
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

    public boolean isModerator() {
        return isModerator;
    }

    public String getPassword() {
        return password;
    }

    public boolean isAdmin() {
        return isAdmin;
    }
}
