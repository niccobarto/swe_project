package DomainModel;

public class Comment {
    private int id;
    private User commentAuthor;
    private Document document;
    private String text;

    public Comment(int id, User commentAuthor, Document document, String text) {
        this.id = id;
        this.commentAuthor = commentAuthor;
        this.document = document;
        this.text = text;
    }
}
