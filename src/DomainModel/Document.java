package DomainModel;

import java.util.Date;

public class Document {
    private int id;
    private String title;
    private String description;
    private DocumentStatus status;
    private DocumentFormat format;
    private User author;
    private String fileName;
    private String filePath;
    private Date creationDate;

    public Document(int id,String title,String description,DocumentFormat format,User author,String filePath, String fileName, Date creationDate){
        this.id = id;
        this.title=title;
        this.description=description;
        this.status=DocumentStatus.DRAFT;
        this.format=format;
        this.author=author;
        this.filePath = filePath;
        this.fileName = fileName;
        this.creationDate = creationDate;
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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
}
