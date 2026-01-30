package at.fh.opdoc.ui;

import at.fh.opdoc.model.*;
import at.fh.opdoc.repo.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class DokumentationPanel extends JPanel {

    private final PatientRepository patientRepo;
    private final OpKatalogRepository opRepo;
    private final OpPlanRepository planRepo;
    private final OpDokumentationRepository dokuRepo;


    private final Map<Integer, Patient> patientCache = new HashMap<>();
    private final Map<Integer, OpKatalogEintrag> opCache = new HashMap<>();


    private final DefaultListModel<OpPlan> listModel = new DefaultListModel<>();
    private final JList<OpPlan> listPlans = new JList<>(listModel);

    private final JTextField tfPatient = new JTextField();
    private final JTextField tfGebDatum = new JTextField();
    private final JTextField tfSVN = new JTextField();
    private final JTextField tfKasse = new JTextField();

    private final JTextField tfTermin = new JTextField();
    private final JTextField tfSaal = new JTextField();
    private final JTextField tfOp = new JTextField();
    private final JTextField tfStatus = new JTextField();

    private final JTextArea taDiagnose = new JTextArea(3, 40);
    private final JTextArea taIndikation = new JTextArea(3, 40);
    private final JTextArea taVerlauf = new JTextArea(8, 40);
    private final JTextArea taMaterial = new JTextArea(3, 40);
    private final JTextArea taKomplikationen = new JTextArea(3, 40);
    private final JTextArea taMedikation = new JTextArea(3, 40);
    private final JTextArea taNachsorge = new JTextArea(3, 40);

    private final JButton btnSave = new JButton("Dokumentation speichern");

    private OpPlan selectedPlan;

    private final SimpleDateFormat df =
            new SimpleDateFormat("yyyy-MM-dd HH:mm");


    public DokumentationPanel(PatientRepository patientRepo,
                              OpKatalogRepository opRepo,
                              OpPlanRepository planRepo,
                              OpDokumentationRepository dokuRepo) {

        this.patientRepo = patientRepo;
        this.opRepo = opRepo;
        this.planRepo = planRepo;
        this.dokuRepo = dokuRepo;

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerSize(8);
        split.setResizeWeight(0.0);
        split.setLeftComponent(buildLeft());
        split.setRightComponent(buildRight());

        add(split, BorderLayout.CENTER);

        setReadonly(tfPatient, tfGebDatum, tfSVN, tfKasse,
                tfTermin, tfSaal, tfOp, tfStatus);

        hookEvents();
        loadAsync();

        SwingUtilities.invokeLater(() -> split.setDividerLocation(0.30));
    }


    private JComponent buildLeft() {
        JPanel left = new JPanel(new BorderLayout(8, 8));
        left.setBorder(BorderFactory.createTitledBorder("OP-Termine"));

        listPlans.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listPlans.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {

                super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);

                if (value instanceof OpPlan p) {
                    String when = p.getGeplantAm() == null ? "?" : df.format(p.getGeplantAm());
                    setText(when + " | " + safe(p.getOpSaal())
                            + " | " + patientName(p.getPatientId())
                            + " | " + opName(p.getOpId()));
                }
                return this;
            }
        });

        left.add(new JScrollPane(listPlans), BorderLayout.CENTER);
        return left;
    }

    private JComponent buildRight() {
        JPanel right = new JPanel(new BorderLayout(10, 10));
        right.setBorder(BorderFactory.createTitledBorder("Dokumentation"));

        right.add(buildPatientBox(), BorderLayout.NORTH);
        right.add(buildDokuBox(), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(btnSave);
        right.add(bottom, BorderLayout.SOUTH);

        return right;
    }

    private JPanel buildPatientBox() {
        JPanel box = new JPanel(new GridBagLayout());
        box.setBorder(BorderFactory.createTitledBorder("Patient & Termin"));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        int y = 0;
        addRow(box, c, y++, "Patient:", tfPatient);
        addRow(box, c, y++, "Geburtsdatum:", tfGebDatum);
        addRow(box, c, y++, "SVN:", tfSVN);
        addRow(box, c, y++, "Kasse:", tfKasse);
        addRow(box, c, y++, "Termin:", tfTermin);
        addRow(box, c, y++, "OP-Saal:", tfSaal);
        addRow(box, c, y++, "OP:", tfOp);
        addRow(box, c, y++, "Status:", tfStatus);

        return box;
    }

    private JPanel buildDokuBox() {
        JPanel box = new JPanel(new BorderLayout(8, 8));
        box.setBorder(BorderFactory.createTitledBorder("OP-Daten"));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(6, 6, 6, 6));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;

        int y = 0;
        addAreaRow(form, c, y++, "Diagnose:", taDiagnose, 80);
        addAreaRow(form, c, y++, "Indikation:", taIndikation, 80);
        addAreaRow(form, c, y++, "OP-Verlauf:", taVerlauf, 160);
        addAreaRow(form, c, y++, "Material:", taMaterial, 80);
        addAreaRow(form, c, y++, "Komplikationen:", taKomplikationen, 80);
        addAreaRow(form, c, y++, "Medikation:", taMedikation, 80);
        addAreaRow(form, c, y++, "Nachsorge:", taNachsorge, 80);

        box.add(new JScrollPane(form), BorderLayout.CENTER);
        return box;
    }


    private void loadAsync() {
        new SwingWorker<List<OpPlan>, Void>() {

            List<Patient> patients;
            List<OpKatalogEintrag> ops;

            @Override
            protected List<OpPlan> doInBackground() {
                patients = patientRepo.findAll();
                ops = opRepo.findAll();
                return planRepo.findAll();
            }

            @Override
            protected void done() {
                try {
                    patientCache.clear();
                    for (Patient p : patients)
                        patientCache.put(p.getPatientId(), p);

                    opCache.clear();
                    for (OpKatalogEintrag o : ops)
                        opCache.put(o.getOpId(), o);

                    listModel.clear();
                    List<OpPlan> plans = get();
                    plans.sort(Comparator.comparing(
                            OpPlan::getGeplantAm,
                            Comparator.nullsLast(Date::compareTo)));

                    for (OpPlan p : plans)
                        listModel.addElement(p);

                    if (!listModel.isEmpty())
                        listPlans.setSelectedIndex(0);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }


    private void hookEvents() {
        listPlans.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            selectedPlan = listPlans.getSelectedValue();
            loadForSelectedPlan();
        });

        btnSave.addActionListener(e -> saveDoku());
    }



    private void loadForSelectedPlan() {
        if (selectedPlan == null) return;

        Patient pat = patientCache.get(selectedPlan.getPatientId());
        OpKatalogEintrag op = opCache.get(selectedPlan.getOpId());

        tfPatient.setText(pat != null ? pat.displayName() : "");
        tfGebDatum.setText(pat != null && pat.getGeburtsdatum() != null
                ? pat.getGeburtsdatum().toString() : "");
        tfSVN.setText(pat != null ? safe(pat.getSvn()) : "");
        tfKasse.setText(pat != null ? safe(pat.getKasse()) : "");

        tfTermin.setText(df.format(selectedPlan.getGeplantAm()));
        tfSaal.setText(safe(selectedPlan.getOpSaal()));
        tfOp.setText(op != null ? op.displayName() : "");
        tfStatus.setText(safe(selectedPlan.getStatus()));

        OpDokumentation d =
                dokuRepo.findByOpPlanId(selectedPlan.getOpPlanId());

        if (d == null) clearDokuFields();
        else {
            taDiagnose.setText(safe(d.getDiagnose()));
            taIndikation.setText(safe(d.getIndikation()));
            taVerlauf.setText(safe(d.getOpVerlauf()));
            taMaterial.setText(safe(d.getMaterial()));
            taKomplikationen.setText(safe(d.getKomplikationen()));
            taMedikation.setText(safe(d.getMedikation()));
            taNachsorge.setText(safe(d.getNachsorge()));
        }
    }

    private void saveDoku() {
        if (selectedPlan == null) return;

        OpDokumentation d = Optional
                .ofNullable(dokuRepo.findByOpPlanId(selectedPlan.getOpPlanId()))
                .orElse(new OpDokumentation());

        d.setOpPlanId(selectedPlan.getOpPlanId());
        d.setErstelltAm(Optional.ofNullable(d.getErstelltAm()).orElse(new Date()));
        d.setDiagnose(taDiagnose.getText().trim());
        d.setIndikation(taIndikation.getText().trim());
        d.setOpVerlauf(taVerlauf.getText().trim());
        d.setMaterial(taMaterial.getText().trim());
        d.setKomplikationen(taKomplikationen.getText().trim());
        d.setMedikation(taMedikation.getText().trim());
        d.setNachsorge(taNachsorge.getText().trim());

        dokuRepo.save(d);
        JOptionPane.showMessageDialog(this, "Gespeichert", "OK",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void clearDokuFields() {
        taDiagnose.setText("");
        taIndikation.setText("");
        taVerlauf.setText("");
        taMaterial.setText("");
        taKomplikationen.setText("");
        taMedikation.setText("");
        taNachsorge.setText("");
    }


    private void addRow(JPanel p, GridBagConstraints c, int y,
                        String label, JComponent field) {
        c.gridx = 0; c.gridy = y; c.weightx = 0.3;
        p.add(new JLabel(label), c);
        c.gridx = 1; c.weightx = 0.7;
        p.add(field, c);
    }

    private void addAreaRow(JPanel p, GridBagConstraints c, int y,
                            String label, JTextArea area, int h) {
        c.gridx = 0; c.gridy = y; c.gridwidth = 2;
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.add(new JLabel(label), BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(area);
        sp.setPreferredSize(new Dimension(0, h));
        wrap.add(sp, BorderLayout.CENTER);
        p.add(wrap, c);
    }

    private void setReadonly(JTextField... f) {
        for (JTextField t : f) {
            t.setEditable(false);
            t.setBackground(new Color(245,245,245));
        }
    }

    private String patientName(Integer id) {
        Patient p = patientCache.get(id);
        return p != null ? p.displayName() : "Patient ?";
    }

    private String opName(Integer id) {
        OpKatalogEintrag o = opCache.get(id);
        return o != null ? o.displayName() : "OP ?";
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}