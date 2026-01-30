package at.fh.opdoc.ui;

import at.fh.opdoc.repo.*;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    private PatientRepository patientRepo;
    private OpKatalogRepository opRepo;
    private OpPlanRepository planRepo;
    private OpDokumentationRepository dokuRepo;

    public MainFrame() {
        super("OP-Dokumentation");

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Initialer Platzhalter (GUI erscheint sofort)
        add(new JLabel("Lade Daten …", SwingConstants.CENTER), BorderLayout.CENTER);
    }

    // Wird aus App.java aufgerufen (nach DB-Check)
    public void initRepositories() {

        patientRepo = new DbPatientRepository();
        opRepo = new DbOpKatalogRepository();
        planRepo = new DbOpPlanRepository();
        dokuRepo = new DbOpDokumentationRepository();

        SwingUtilities.invokeLater(this::initTabs);
    }

    private void initTabs() {

        JTabbedPane tabs = new JTabbedPane();

        OpPlanPanel opPlanPanel =
                new OpPlanPanel(patientRepo, opRepo, planRepo);

        DokumentationPanel dokuPanel =
                new DokumentationPanel(patientRepo, opRepo, planRepo, dokuRepo);

        tabs.addTab("OP-Plan", opPlanPanel);
        tabs.addTab("Dokumentation", dokuPanel);
        tabs.addTab("Patienten", new PatientenPanel(patientRepo, dokuRepo));
        tabs.addTab("OP-Katalog", new OpKatalogPanel(opRepo));


        getContentPane().removeAll();
        add(tabs, BorderLayout.CENTER);

        revalidate();
        repaint();
    }
}