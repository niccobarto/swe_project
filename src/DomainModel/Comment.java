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
}
