package at.fh.opdoc;

import at.fh.opdoc.db.Db;
import at.fh.opdoc.ui.MainFrame;

import javax.swing.*;

public class App {

    public static void main(String[] args) {

        if (!Db.canConnect()) {
            JOptionPane.showMessageDialog(
                    null,
                    "Datenbank nicht erreichbar.\nProgramm wird beendet.",
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE
            );
            System.exit(1);
        }

        SwingUtilities.invokeLater(() -> {

            try {
                UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName()
                );
            } catch (Exception ignored) {}


            MainFrame frame = new MainFrame();

            frame.setVisible(true);

            frame.initRepositories();
        });
    }
}