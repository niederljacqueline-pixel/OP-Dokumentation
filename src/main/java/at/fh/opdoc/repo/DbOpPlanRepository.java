package at.fh.opdoc.repo;

import at.fh.opdoc.db.Db;
import at.fh.opdoc.model.OpPlan;

import java.sql.*;
import java.util.*;
import java.util.Date;

public class DbOpPlanRepository implements OpPlanRepository {


    private List<OpPlan> cache = null;
    private boolean dirty = true;


    @Override
    public List<OpPlan> findAll() {
        // 🔥 Cache-Hit → kein DB-Zugriff
        if (!dirty && cache != null) {
            return new ArrayList<>(cache); // defensive copy
        }

        String sql = """
                SELECT op_plan_id, patient_id, op_id, geplant_am,
                       dauer_min, op_saal, status, bemerkung
                FROM op_plan
                ORDER BY geplant_am ASC
                """;

        List<OpPlan> out = new ArrayList<>();

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                out.add(map(rs));
            }

            cache = out;
            dirty = false;

            return new ArrayList<>(cache);

        } catch (SQLException e) {
            throw new RuntimeException("DB: findAll() fehlgeschlagen", e);
        }
    }

    @Override
    public List<OpPlan> findByPatientId(Integer patientId) {
        if (patientId == null) return findAll();

        String sql = """
                SELECT op_plan_id, patient_id, op_id, geplant_am,
                       dauer_min, op_saal, status, bemerkung
                FROM op_plan
                WHERE patient_id = ?
                ORDER BY geplant_am ASC
                """;

        List<OpPlan> out = new ArrayList<>();

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, patientId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs));
                }
            }

            return out;

        } catch (SQLException e) {
            throw new RuntimeException("DB: findByPatientId() fehlgeschlagen", e);
        }
    }


    @Override
    public OpPlan save(OpPlan plan) {
        if (plan == null) return null;

        OpPlan result;
        if (plan.getOpPlanId() == null) {
            result = insert(plan);
        } else {
            result = update(plan);
        }

        dirty = true; // ❗ Cache ungültig
        return result;
    }

    private OpPlan insert(OpPlan p) {
        String sql = """
                INSERT INTO op_plan
                (patient_id, op_id, geplant_am, dauer_min, op_saal, status, bemerkung)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bind(ps, p);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    p.setOpPlanId(keys.getInt(1));
                }
            }

            return p;

        } catch (SQLException e) {
            throw new RuntimeException("DB: insert() fehlgeschlagen", e);
        }
    }

    private OpPlan update(OpPlan p) {
        String sql = """
                UPDATE op_plan
                   SET patient_id = ?,
                       op_id = ?,
                       geplant_am = ?,
                       dauer_min = ?,
                       op_saal = ?,
                       status = ?,
                       bemerkung = ?
                 WHERE op_plan_id = ?
                """;

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            bind(ps, p);
            ps.setInt(8, p.getOpPlanId());

            ps.executeUpdate();
            return p;

        } catch (SQLException e) {
            throw new RuntimeException("DB: update() fehlgeschlagen", e);
        }
    }

    @Override
    public void deleteById(Integer id) {
        if (id == null) return;

        String sql = "DELETE FROM op_plan WHERE op_plan_id = ?";

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

            dirty = true; // ❗ Cache ungültig

        } catch (SQLException e) {
            throw new RuntimeException("DB: deleteById() fehlgeschlagen", e);
        }
    }



    private OpPlan map(ResultSet rs) throws SQLException {
        OpPlan p = new OpPlan();

        p.setOpPlanId(rs.getInt("op_plan_id"));
        p.setPatientId(rs.getInt("patient_id"));
        p.setOpId(rs.getInt("op_id"));

        Timestamp ts = rs.getTimestamp("geplant_am");
        p.setGeplantAm(ts != null ? new Date(ts.getTime()) : null);

        p.setDauerMin(rs.getInt("dauer_min"));
        p.setOpSaal(rs.getString("op_saal"));
        p.setStatus(rs.getString("status"));
        p.setBemerkung(rs.getString("bemerkung"));

        return p;
    }

    private void bind(PreparedStatement ps, OpPlan p) throws SQLException {
        ps.setInt(1, p.getPatientId());
        ps.setInt(2, p.getOpId());

        if (p.getGeplantAm() != null) {
            ps.setTimestamp(3, new Timestamp(p.getGeplantAm().getTime()));
        } else {
            ps.setNull(3, Types.TIMESTAMP);
        }

        ps.setInt(4, p.getDauerMin() != null ? p.getDauerMin() : 0);
        ps.setString(5, p.getOpSaal());
        ps.setString(6, p.getStatus());
        ps.setString(7, p.getBemerkung());
    }
}
