package DomainModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Document {
    private final int id;
    private String title;
    private String description;
    private DocumentStatus status;
    private DocumentFormat format;
    private User author;
    private String fileName;
    private String filePath;
    private Date creationDate;
    private List<Tag> tags;

    public Document(int id, String title,String description,DocumentFormat format,User author,String filePath, String fileName,Date creationDate){
        this.id=id;
        this.title=title;
        this.description=description;
        this.status=DocumentStatus.DRAFT;
        this.format=format;
        this.author=author;
        this.filePath = filePath;
        this.fileName = fileName;
        this.creationDate = creationDate;
        this.tags=new ArrayList<>();
    }
    public int getId() {
        return id;
    }
    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
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

    public List<Tag> getTags() {
        return tags;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

}

