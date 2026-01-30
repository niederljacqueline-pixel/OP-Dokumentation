package at.fh.opdoc.model;

import java.util.Date;

public class OpDokumentation {

    private Integer dokuId;
    private Integer opPlanId;
    private Date erstelltAm;

    private String diagnose;
    private String indikation;
    private String opVerlauf;
    private String material;
    private String komplikationen;
    private String medikation;
    private String nachsorge;

    public Integer getDokuId() { return dokuId; }
    public void setDokuId(Integer dokuId) { this.dokuId = dokuId; }

    public Integer getOpPlanId() { return opPlanId; }
    public void setOpPlanId(Integer opPlanId) { this.opPlanId = opPlanId; }

    public Date getErstelltAm() { return erstelltAm; }
    public void setErstelltAm(Date erstelltAm) { this.erstelltAm = erstelltAm; }

    public String getDiagnose() { return diagnose; }
    public void setDiagnose(String diagnose) { this.diagnose = diagnose; }

    public String getIndikation() { return indikation; }
    public void setIndikation(String indikation) { this.indikation = indikation; }

    public String getOpVerlauf() { return opVerlauf; }
    public void setOpVerlauf(String opVerlauf) { this.opVerlauf = opVerlauf; }

    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }

    public String getKomplikationen() { return komplikationen; }
    public void setKomplikationen(String komplikationen) { this.komplikationen = komplikationen; }

    public String getMedikation() { return medikation; }
    public void setMedikation(String medikation) { this.medikation = medikation; }

    public String getNachsorge() { return nachsorge; }
    public void setNachsorge(String nachsorge) { this.nachsorge = nachsorge; }
}
