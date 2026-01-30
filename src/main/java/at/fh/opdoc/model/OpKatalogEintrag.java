package at.fh.opdoc.model;

public class OpKatalogEintrag {
    private Integer opId;
    private String name;
    private String beschreibung;

    public OpKatalogEintrag() {}

    public OpKatalogEintrag(Integer opId, String name, String beschreibung) {
        this.opId = opId;
        this.name = name;
        this.beschreibung = beschreibung;
    }

    @Override
    public String toString() {
        return displayName(); // oder getName()
    }

    public Integer getOpId() { return opId; }
    public void setOpId(Integer opId) { this.opId = opId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBeschreibung() { return beschreibung; }
    public void setBeschreibung(String beschreibung) { this.beschreibung = beschreibung; }

    public String displayName() {
        return opId == null ? name : name + " (#" + opId + ")";
    }
}