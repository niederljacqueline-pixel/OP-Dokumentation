package at.fh.opdoc.repo;

import at.fh.opdoc.db.Db;
import at.fh.opdoc.model.Patient;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DbPatientRepository implements PatientRepository {

    @Override
    public List<Patient> findAll() {
        String sql = """
                SELECT patient_id, vorname, nachname, geburtsdatum, svn, kasse, adresse, krankengeschichte
                FROM patient
                ORDER BY nachname ASC, vorname ASC
                """;

        List<Patient> out = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) out.add(map(rs));
            return out;

        } catch (SQLException e) {
            throw new RuntimeException("DB: findAll() fehlgeschlagen: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Patient> search(String q) {
        String query = (q == null) ? "" : q.trim();
        if (query.isBlank()) return findAll();

        // Suche nach Vorname/Nachname oder ID (als Text)
        String sql = """
                SELECT patient_id, vorname, nachname, geburtsdatum, svn, kasse, adresse, krankengeschichte
                FROM patient
                WHERE LOWER(vorname) LIKE ?
                   OR LOWER(nachname) LIKE ?
                   OR CAST(patient_id AS CHAR) LIKE ?
                ORDER BY nachname ASC, vorname ASC
                """;

        List<Patient> out = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            String like = "%" + query.toLowerCase() + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, "%" + query + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
            return out;

        } catch (SQLException e) {
            throw new RuntimeException("DB: search() fehlgeschlagen: " + e.getMessage(), e);
        }
    }

    @Override
    public Patient save(Patient p) {
        if (p == null) throw new IllegalArgumentException("Patient darf nicht null sein.");

        if (p.getPatientId() == null) {
            return insert(p);
        } else {
            return update(p);
        }
    }

    private Patient insert(Patient p) {
        String sql = """
                INSERT INTO patient (vorname, nachname, geburtsdatum, svn, kasse, adresse, krankengeschichte)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bindCommon(ps, p);

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    p.setPatientId(keys.getInt(1));
                }
            }
            return p;

        } catch (SQLException e) {
            throw new RuntimeException("DB: insert() fehlgeschlagen: " + e.getMessage(), e);
        }
    }

    private Patient update(Patient p) {
        String sql = """
                UPDATE patient
                   SET vorname = ?,
                       nachname = ?,
                       geburtsdatum = ?,
                       svn = ?,
                       kasse = ?,
                       adresse = ?,
                       krankengeschichte = ?
                 WHERE patient_id = ?
                """;

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            bindCommon(ps, p);
            ps.setInt(8, p.getPatientId());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                // Falls ID zwar gesetzt ist, aber nicht existiert -> Insert als Fallback
                // (kannst du auch als Fehler behandeln, wenn du willst)
                return insert(p);
            }
            return p;

        } catch (SQLException e) {
            throw new RuntimeException("DB: update() fehlgeschlagen: " + e.getMessage(), e);
        }
    }

    private void bindCommon(PreparedStatement ps, Patient p) throws SQLException {
        ps.setString(1, nullIfBlank(p.getVorname()));
        ps.setString(2, nullIfBlank(p.getNachname()));

        LocalDate gd = p.getGeburtsdatum();
        if (gd == null) ps.setNull(3, Types.DATE);
        else ps.setDate(3, Date.valueOf(gd));

        ps.setString(4, nullIfBlank(p.getSvn()));
        ps.setString(5, nullIfBlank(p.getKasse()));
        ps.setString(6, nullIfBlank(p.getAdresse()));
        ps.setString(7, nullIfBlank(p.getKrankengeschichte()));
    }

    @Override
    public void deleteById(Integer id) {
        if (id == null) return;

        String sql = "DELETE FROM patient WHERE patient_id = ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("DB: deleteById() fehlgeschlagen: " + e.getMessage(), e);
        }
    }


    private Patient map(ResultSet rs) throws SQLException {
        Patient p = new Patient();

        p.setPatientId(rs.getInt("patient_id"));
        p.setVorname(rs.getString("vorname"));
        p.setNachname(rs.getString("nachname"));

        Date d = rs.getDate("geburtsdatum");
        p.setGeburtsdatum(d != null ? d.toLocalDate() : null);

        p.setSvn(rs.getString("svn"));
        p.setKasse(rs.getString("kasse"));
        p.setAdresse(rs.getString("adresse"));
        p.setKrankengeschichte(rs.getString("krankengeschichte"));


        return p;
    }

    private String nullIfBlank(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
