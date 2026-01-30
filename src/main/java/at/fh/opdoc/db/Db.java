package at.fh.opdoc.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Db {

    private Db() {}

    public static Connection getConnection() throws SQLException {
        applyEnvOverrides();

        String url = "jdbc:mysql://" + DbConfig.HOST + ":" + DbConfig.PORT + "/" + DbConfig.DATABASE
                + "?useUnicode=true&characterEncoding=utf8"
                + "&useSSL=false"
                + "&allowPublicKeyRetrieval=true"
                + "&serverTimezone=Europe/Vienna";

        return DriverManager.getConnection(url, DbConfig.USER, DbConfig.PASSWORD);
    }

    private static void applyEnvOverrides() {
        // Default: Zuhause / localhost
        if (DbConfig.HOST == null) DbConfig.HOST = "localhost";
        if (DbConfig.PORT == 0) DbConfig.PORT = 3306;
        if (DbConfig.DATABASE == null) DbConfig.DATABASE = "24nipu";
        if (DbConfig.USER == null) DbConfig.USER = "24nija";
        if (DbConfig.PASSWORD == null) DbConfig.PASSWORD = "geb24";

        // FH-Override nur wenn ENV=fh
        if ("fh".equalsIgnoreCase(DbConfig.ENV)) {
            DbConfig.HOST = "10.25.2.145";
            DbConfig.PORT = 3306;
            DbConfig.DATABASE = "24nipu";
            DbConfig.USER = "24nija";
            DbConfig.PASSWORD = "geb24";
        }
    }

    // Sicherer Verbindungstest
    public static boolean canConnect() {
        try (Connection c = getConnection()) {
            return true;
        } catch (Exception e) {
            System.out.println("DB connect failed: " + e.getMessage());
            return false;
        }
    }
}
