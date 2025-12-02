package DomainModel;

import java.util.Date;

public class Comment {
    private int id;
    private User commentAuthor;
    private Document document;
    private String text;
    private Date date;

    public Comment(int id, User commentAuthor, Document document, String text,Date date) {
        this.id = id;
        this.commentAuthor = commentAuthor;
        this.document = document;
        this.text = text;
        this.date = date;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    // New getters to support tests and consumers
    public User getCommentAuthor() {
        return commentAuthor;
    }

    public Document getDocument() {
        return document;
    }

    public String getText() {
        return text;
    }

    public Date getDate() {
        return date;
    }
}
