package at.fh.opdoc.repo;

import at.fh.opdoc.db.Db;
import at.fh.opdoc.model.OpDokumentation;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DbOpDokumentationRepository implements OpDokumentationRepository {

    //Datenbankzugriff für Dokumentationspanel

    @Override
    public List<OpDokumentation> findAll() {
        String sql = """
            SELECT *
            FROM op_dokumentation
            ORDER BY erstellt_am DESC
        """;

        List<OpDokumentation> list = new ArrayList<>();

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(map(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("findAll failed", e);
        }

        return list;
    }

    @Override
    public OpDokumentation findByOpPlanId(Integer opPlanId) {
        if (opPlanId == null) return null;

        String sql = """
            SELECT *
            FROM op_dokumentation
            WHERE op_plan_id = ?
        """;

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, opPlanId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }

        } catch (SQLException e) {
            throw new RuntimeException("findByOpPlanId failed", e);
        }

        return null;
    }

    // Krankengeschichte eines Patienten
    @Override
    public List<OpDokumentation> findByPatientId(Integer patientId) {
        if (patientId == null) return List.of();

        String sql = """
            SELECT d.*
            FROM op_dokumentation d
            JOIN op_plan p ON d.op_plan_id = p.op_plan_id
            WHERE p.patient_id = ?
            ORDER BY d.erstellt_am DESC
        """;

        List<OpDokumentation> list = new ArrayList<>();

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, patientId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("findByPatientId failed", e);
        }

        return list;
    }

    @Override
    public OpDokumentation save(OpDokumentation d) {
        if (d == null) return null;
        if (d.getOpPlanId() == null)
            throw new IllegalArgumentException("opPlanId darf nicht null sein");

        OpDokumentation existing = findByOpPlanId(d.getOpPlanId());

        if (existing != null) {
            d.setDokuId(existing.getDokuId());
            update(d);
            return d;
        }

        insert(d);
        return d;
    }

    @Override
    public void deleteById(Integer id) {
        if (id == null) return;

        try (Connection c = Db.getConnection();
             PreparedStatement ps =
                     c.prepareStatement("DELETE FROM op_dokumentation WHERE doku_id = ?")) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("deleteById failed", e);
        }
    }

    private void insert(OpDokumentation d) {
        String sql = """
            INSERT INTO op_dokumentation (
                op_plan_id, erstellt_am,
                diagnose, indikation, op_verlauf,
                material, komplikationen, medikation, nachsorge
            ) VALUES (?,?,?,?,?,?,?,?,?)
        """;

        try (Connection c = Db.getConnection();
             PreparedStatement ps =
                     c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, d.getOpPlanId());
            ps.setTimestamp(2, new Timestamp(
                    (d.getErstelltAm() != null ? d.getErstelltAm() : new Date()).getTime()
            ));
            ps.setString(3, d.getDiagnose());
            ps.setString(4, d.getIndikation());
            ps.setString(5, d.getOpVerlauf());
            ps.setString(6, d.getMaterial());
            ps.setString(7, d.getKomplikationen());
            ps.setString(8, d.getMedikation());
            ps.setString(9, d.getNachsorge());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) d.setDokuId(keys.getInt(1));
            }

        } catch (SQLException e) {
            throw new RuntimeException("insert failed", e);
        }
    }

    private void update(OpDokumentation d) {
        String sql = """
            UPDATE op_dokumentation SET
                diagnose = ?, indikation = ?, op_verlauf = ?,
                material = ?, komplikationen = ?, medikation = ?, nachsorge = ?
            WHERE doku_id = ?
        """;

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, d.getDiagnose());
            ps.setString(2, d.getIndikation());
            ps.setString(3, d.getOpVerlauf());
            ps.setString(4, d.getMaterial());
            ps.setString(5, d.getKomplikationen());
            ps.setString(6, d.getMedikation());
            ps.setString(7, d.getNachsorge());
            ps.setInt(8, d.getDokuId());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("update failed", e);
        }
    }

    private OpDokumentation map(ResultSet rs) throws SQLException {
        OpDokumentation d = new OpDokumentation();

        d.setDokuId(rs.getInt("doku_id"));
        d.setOpPlanId(rs.getInt("op_plan_id"));

        Timestamp ts = rs.getTimestamp("erstellt_am");
        d.setErstelltAm(ts == null ? null : new Date(ts.getTime()));

        d.setDiagnose(rs.getString("diagnose"));
        d.setIndikation(rs.getString("indikation"));
        d.setOpVerlauf(rs.getString("op_verlauf"));
        d.setMaterial(rs.getString("material"));
        d.setKomplikationen(rs.getString("komplikationen"));
        d.setMedikation(rs.getString("medikation"));
        d.setNachsorge(rs.getString("nachsorge"));

        return d;
    }
}
