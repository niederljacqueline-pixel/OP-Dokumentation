package at.fh.opdoc.db;

public class DbConfig {

    // Default: lokal (für Zuhause)
    public static String HOST = "localhost";
    public static int PORT = 3306;
    public static String DATABASE = "24nipu";

    public static String USER = "24nija";
    public static String PASSWORD = "geb24";

    /**
     * Umgebung:
     *  - lokal = zuhause / lokale DB
     *  - fh   = FH-WLAN
     * Umschalten mit VPN
     */
    public static String ENV = System.getProperty("opdoc.env", "local");

}
