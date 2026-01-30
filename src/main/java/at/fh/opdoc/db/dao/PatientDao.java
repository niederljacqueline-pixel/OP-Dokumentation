package at.fh.opdoc.db.dao;

import at.fh.opdoc.db.Db;
import at.fh.opdoc.model.Patient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class PatientDao {

    public List<Patient> findAll() throws Exception {
        String sql = """
            SELECT patient_id, vorname, nachname, geburtsdatum, svn, adresse, telefon, email, kasse, krankengeschichte
            FROM patient
            ORDER BY nachname, vorname
        """;
        return runQuery(sql, null);
    }

    public List<Patient> search(String query) throws Exception {
        String sql = """
            SELECT patient_id, vorname, nachname, geburtsdatum, svn, adresse, telefon, email, kasse, krankengeschichte
            FROM patient
            WHERE LOWER(vorname) LIKE ? OR LOWER(nachname) LIKE ? OR LOWER(patient_id) LIKE ?
            ORDER BY nachname, vorname
        """;
        String q = "%" + query.toLowerCase() + "%";
        return runQuery(sql, new String[]{q, q, q});
    }

    private List<Patient> runQuery(String sql, String[] params) throws Exception {
        List<Patient> out = new ArrayList<>();

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    ps.setString(i + 1, params[i]);
                }
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Patient p = new Patient();

                    int id = rs.getInt("patient_id");
                    p.setPatientId(rs.wasNull() ? null : id);

                    p.setVorname(rs.getString("vorname"));
                    p.setNachname(rs.getString("nachname"));

                    java.sql.Date d = rs.getDate("geburtsdatum");
                    p.setGeburtsdatum(d == null ? null : d.toLocalDate());

                    p.setSvn(rs.getString("svn"));
                    p.setAdresse(rs.getString("adresse"));
                    p.setTelefon(rs.getString("telefon"));
                    p.setEmail(rs.getString("email"));
                    p.setKasse(rs.getString("kasse"));
                    p.setKrankengeschichte(rs.getString("krankengeschichte"));

                    out.add(p);
                }
            }
        }

        return out;
    }
}