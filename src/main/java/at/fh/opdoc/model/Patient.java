package at.fh.opdoc.model;

import java.time.LocalDate;

public class Patient {

    private Integer patientId;
    private String vorname;
    private String nachname;
    private LocalDate geburtsdatum;
    private String svn;
    private String adresse;
    private String telefon;
    private String email;
    private String kasse;
    private String krankengeschichte;

    public String displayName() {
        String base = ((vorname == null ? "" : vorname) + " " + (nachname == null ? "" : nachname)).trim();
        return patientId == null ? base : base + " (#" + patientId + ")";
    }

    @Override
    public String toString() {
        return displayName();
    }

    public Integer getPatientId() { return patientId; }
    public void setPatientId(Integer patientId) { this.patientId = patientId; }

    public String getVorname() { return vorname; }
    public void setVorname(String vorname) { this.vorname = vorname; }

    public String getNachname() { return nachname; }
    public void setNachname(String nachname) { this.nachname = nachname; }

    public LocalDate getGeburtsdatum() { return geburtsdatum; }
    public void setGeburtsdatum(LocalDate geburtsdatum) { this.geburtsdatum = geburtsdatum; }

    public String getSvn() { return svn; }
    public void setSvn(String svn) { this.svn = svn; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getTelefon() { return telefon; }
    public void setTelefon(String telefon) { this.telefon = telefon; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getKasse() { return kasse; }
    public void setKasse(String kasse) { this.kasse = kasse; }

    public String getKrankengeschichte() { return krankengeschichte; }
    public void setKrankengeschichte(String krankengeschichte) { this.krankengeschichte = krankengeschichte; }
}