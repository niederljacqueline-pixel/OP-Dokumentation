package at.fh.opdoc.repo;

import at.fh.opdoc.db.Db;
import at.fh.opdoc.model.OpKatalogEintrag;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DbOpKatalogRepository implements OpKatalogRepository {

    @Override
    public List<OpKatalogEintrag> findAll() {
        String sql = """
                SELECT op_id, name, beschreibung
                FROM op_katalog
                ORDER BY name ASC
                """;

        List<OpKatalogEintrag> out = new ArrayList<>();

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                out.add(map(rs));
            }
            return out;

        } catch (SQLException e) {
            throw new RuntimeException("DB: OpKatalog findAll() fehlgeschlagen", e);
        }
    }

    @Override
    public List<OpKatalogEintrag> search(String q) {
        String query = (q == null) ? "" : q.trim();
        if (query.isBlank()) return findAll();

        String sql = """
                SELECT op_id, name, beschreibung
                FROM op_katalog
                WHERE LOWER(name) LIKE LOWER(?)
                   OR LOWER(beschreibung) LIKE LOWER(?)
                ORDER BY name ASC
                """;

        List<OpKatalogEintrag> out = new ArrayList<>();
        String like = "%" + query + "%";

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, like);
            ps.setString(2, like);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs));
                }
            }
            return out;

        } catch (SQLException e) {
            throw new RuntimeException("DB: OpKatalog search() fehlgeschlagen", e);
        }
    }

    @Override
    public OpKatalogEintrag save(OpKatalogEintrag op) {
        if (op == null) return null;

        if (op.getOpId() == null) {
            return insert(op);
        } else {
            return update(op);
        }
    }

    private OpKatalogEintrag insert(OpKatalogEintrag op) {
        String sql = """
                INSERT INTO op_katalog (name, beschreibung)
                VALUES (?, ?)
                """;

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, op.getName());
            ps.setString(2, op.getBeschreibung());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    op.setOpId(keys.getInt(1));
                }
            }
            return op;

        } catch (SQLException e) {
            throw new RuntimeException("DB: OpKatalog insert() fehlgeschlagen", e);
        }
    }

    private OpKatalogEintrag update(OpKatalogEintrag op) {
        String sql = """
                UPDATE op_katalog
                   SET name = ?,
                       beschreibung = ?
                 WHERE op_id = ?
                """;

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, op.getName());
            ps.setString(2, op.getBeschreibung());
            ps.setInt(3, op.getOpId());

            ps.executeUpdate();
            return op;

        } catch (SQLException e) {
            throw new RuntimeException("DB: OpKatalog update() fehlgeschlagen", e);
        }
    }

    @Override
    public void deleteById(Integer id) {
        if (id == null) return;

        String sql = "DELETE FROM op_katalog WHERE op_id = ?";

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("DB: OpKatalog deleteById() fehlgeschlagen", e);
        }
    }



    private OpKatalogEintrag map(ResultSet rs) throws SQLException {
        OpKatalogEintrag op = new OpKatalogEintrag();
        op.setOpId(rs.getInt("op_id"));
        op.setName(rs.getString("name"));
        op.setBeschreibung(rs.getString("beschreibung"));
        return op;
    }
}