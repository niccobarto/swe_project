package DomainModel;

public class TextPart extends Document {
    private String instrument;
    private String tonality;
    private String compositor;

    public TextPart(String title, String description, DocumentFormat format, User author, String instrument, String tonality, String compositor) {
        super(title, description, format, author);
        this.instrument = instrument;
        this.tonality = tonality;
        this.compositor = compositor;
    }

    public String getInstrument() {
        return instrument;
    }

    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }

    public String getTonality() {
        return tonality;
    }

    public void setTonality(String tonality) {
        this.tonality = tonality;
    }

    public String getCompositor() {
        return compositor;
    }

    public void setCompositor(String compositor) {
        this.compositor = compositor;
    }
}
