package at.fh.opdoc.model;

import java.util.Date;

public class OpPlan {
    private Integer opPlanId;
    private Integer patientId;
    private Integer opId;
    private Date geplantAm;
    private String status; // zeigt ob die OP PLANNED / DONE / CANCELED ist
    private String bemerkung;

    private String opSaal;
    private Integer dauerMin;


    public Integer getOpPlanId() { return opPlanId; }
    public void setOpPlanId(Integer opPlanId) { this.opPlanId = opPlanId; }

    public Integer getPatientId() { return patientId; }
    public void setPatientId(Integer patientId) { this.patientId = patientId; }

    public Integer getOpId() { return opId; }
    public void setOpId(Integer opId) { this.opId = opId; }

    public Date getGeplantAm() { return geplantAm; }
    public void setGeplantAm(Date geplantAm) { this.geplantAm = geplantAm; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBemerkung() { return bemerkung; }
    public void setBemerkung(String bemerkung) { this.bemerkung = bemerkung; }

    public String getOpSaal() { return opSaal; }
    public void setOpSaal(String opSaal) { this.opSaal = opSaal; }

    public Integer getDauerMin() { return dauerMin; }
    public void setDauerMin(Integer dauerMin) { this.dauerMin = dauerMin; }
}