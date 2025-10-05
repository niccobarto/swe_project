package DomainModel;

public class Document {
    private int id;
    private String title;
    private String description;
    private DocumentStatus status;
    private DocumentFormat format;
    private User author;
    private String period;

    public Document(int id,String title,String description,DocumentFormat format,User author,String period){
        this.id = id;
        this.title=title;
        this.description=description;
        this.status=DocumentStatus.DRAFT;
        this.format=format;
        this.author=author;
        this.period=period;
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DocumentFormat getFormat() {
        return format;
    }

    public void setFormat(DocumentFormat format) {
        this.format = format;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public String getPeriod() {
        return period;
    }
    public void setPeriod(String period) {
        this.period = period;
    }

}
