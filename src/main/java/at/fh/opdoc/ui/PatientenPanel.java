package at.fh.opdoc.ui;

import at.fh.opdoc.model.OpDokumentation;
import at.fh.opdoc.model.Patient;
import at.fh.opdoc.repo.OpDokumentationRepository;
import at.fh.opdoc.repo.PatientRepository;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class PatientenPanel extends JPanel {



    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> patientList = new JList<>(listModel);
    private final JTextField searchField = new JTextField();

    private final JTextField tfPatientId = new JTextField();
    private final JTextField tfVorname = new JTextField();
    private final JTextField tfNachname = new JTextField();
    private final JTextField tfGeburtsdatum = new JTextField();
    private final JTextField tfSvn = new JTextField();
    private final JTextField tfAdresse = new JTextField();
    private final JTextField tfTelefon = new JTextField();
    private final JTextField tfEmail = new JTextField();
    private final JComboBox<String> cbKasse =
            new JComboBox<>(new String[]{"ÖGK", "BVAEB", "SVS", "KFA", "Privat"});

    private final JTextArea taKrankengeschichte = new JTextArea(10, 30);

    private final JButton btnSave = new JButton("Speichern");
    private final JButton btnDelete = new JButton("Löschen");



    private final PatientRepository patientRepo;
    private final OpDokumentationRepository dokuRepo;

    private List<Patient> currentPatients;

    private static final DateTimeFormatter UI_DATE =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public PatientenPanel(PatientRepository patientRepo,
                          OpDokumentationRepository dokuRepo) {

        this.patientRepo = patientRepo;
        this.dokuRepo = dokuRepo;

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        add(buildTopBar(), BorderLayout.NORTH);
        add(buildMainSplit(), BorderLayout.CENTER);

        hookEvents();
        loadAsync("");   // 🔥 EIN Einstiegspunkt
    }


    private JComponent buildTopBar() {
        JPanel top = new JPanel(new BorderLayout(10, 0));

        JLabel title = new JLabel("Patienten");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        top.add(title, BorderLayout.WEST);

        JPanel search = new JPanel(new BorderLayout(6, 0));
        search.add(new JLabel("Patientensuche:"), BorderLayout.WEST);
        search.add(searchField, BorderLayout.CENTER);
        top.add(search, BorderLayout.CENTER);

        JButton btnAdd = new JButton("Patient anlegen");
        btnAdd.addActionListener(e -> clearForm());
        top.add(btnAdd, BorderLayout.EAST);

        return top;
    }

    private JComponent buildMainSplit() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.30);
        split.setLeftComponent(buildLeftList());
        split.setRightComponent(buildRightForm());
        return split;
    }

    private JComponent buildLeftList() {
        patientList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(patientList);

        JPanel left = new JPanel(new BorderLayout(0, 8));
        left.add(new JLabel("Patienten"), BorderLayout.NORTH);
        left.add(scroll, BorderLayout.CENTER);

        return left;
    }

    private JComponent buildRightForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Details"));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        int y = 0;

        tfPatientId.setEditable(false);
        addRow(form, c, y++, "Patienten ID:", tfPatientId);
        addRow(form, c, y++, "Vorname:", tfVorname);
        addRow(form, c, y++, "Nachname:", tfNachname);
        addRow(form, c, y++, "Geburtsdatum (dd.MM.yyyy):", tfGeburtsdatum);
        addRow(form, c, y++, "SVN:", tfSvn);
        addRow(form, c, y++, "Adresse:", tfAdresse);
        addRow(form, c, y++, "Telefon:", tfTelefon);
        addRow(form, c, y++, "E-Mail:", tfEmail);
        addRow(form, c, y++, "Kasse:", cbKasse);

        c.gridx = 0;
        c.gridy = y;
        c.gridwidth = 2;
        form.add(new JLabel("Krankengeschichte (OP-Historie):"), c);

        y++;
        taKrankengeschichte.setEditable(false);
        taKrankengeschichte.setLineWrap(true);
        taKrankengeschichte.setWrapStyleWord(true);

        JScrollPane sp = new JScrollPane(taKrankengeschichte);
        c.gridy = y;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        form.add(sp, c);

        y++;
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(btnDelete);
        buttons.add(btnSave);

        c.gridy = y;
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(buttons, c);

        return form;
    }

    private void addRow(JPanel form, GridBagConstraints c, int y,
                        String label, JComponent field) {
        c.gridx = 0;
        c.gridy = y;
        c.weightx = 0.35;
        form.add(new JLabel(label), c);

        c.gridx = 1;
        c.weightx = 0.65;
        form.add(field, c);
    }


    private void hookEvents() {

        searchField.getDocument().addDocumentListener((SimpleDocumentListener) e ->
                loadAsync(searchField.getText().trim())
        );

        patientList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int idx = patientList.getSelectedIndex();
            if (idx >= 0 && idx < currentPatients.size()) {
                loadHistoryAsync(currentPatients.get(idx));
            }
        });

        btnSave.addActionListener(e -> savePatient());
        btnDelete.addActionListener(e -> deletePatient());
    }


    private void loadAsync(String query) {

        new SwingWorker<List<Patient>, Void>() {

            @Override
            protected List<Patient> doInBackground() {
                return (query == null || query.isBlank())
                        ? patientRepo.findAll()
                        : patientRepo.search(query);
            }

            @Override
            protected void done() {
                try {
                    currentPatients = get();
                    listModel.clear();
                    for (Patient p : currentPatients)
                        listModel.addElement(p.displayName());

                    if (!currentPatients.isEmpty())
                        patientList.setSelectedIndex(0);
                    else
                        clearForm();

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private void loadHistoryAsync(Patient p) {

        fillForm(p);
        taKrankengeschichte.setText("Lade OP-Historie …");

        new SwingWorker<List<OpDokumentation>, Void>() {

            @Override
            protected List<OpDokumentation> doInBackground() {
                return dokuRepo.findByPatientId(p.getPatientId());
            }

            @Override
            protected void done() {
                try {
                    List<OpDokumentation> docs = get();
                    StringBuilder sb = new StringBuilder();

                    for (OpDokumentation d : docs) {
                        sb.append(UI_DATE.format(
                                        d.getErstelltAm().toInstant()
                                                .atZone(java.time.ZoneId.systemDefault())
                                                .toLocalDate()))
                                .append("\n");

                        if (d.getDiagnose() != null)
                            sb.append("Diagnose: ").append(d.getDiagnose()).append("\n");
                        if (d.getOpVerlauf() != null)
                            sb.append("OP-Verlauf: ").append(d.getOpVerlauf()).append("\n");
                        if (d.getNachsorge() != null)
                            sb.append("Nachsorge: ").append(d.getNachsorge()).append("\n");

                        sb.append("----------------------------------------\n");
                    }

                    taKrankengeschichte.setText(sb.toString());
                    taKrankengeschichte.setCaretPosition(0);

                } catch (Exception ex) {
                    ex.printStackTrace();
                    taKrankengeschichte.setText("");
                }
            }
        }.execute();
    }


    private void savePatient() {
        try {
            Patient p = readPatientFromForm();
            patientRepo.save(p);
            loadAsync(searchField.getText().trim());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deletePatient() {
        if (tfPatientId.getText().isBlank()) return;
        patientRepo.deleteById(Integer.parseInt(tfPatientId.getText()));
        loadAsync(searchField.getText().trim());
        clearForm();
    }


    private Patient readPatientFromForm() {
        Patient p = new Patient();

        if (!tfPatientId.getText().isBlank())
            p.setPatientId(Integer.parseInt(tfPatientId.getText()));

        p.setVorname(tfVorname.getText().trim());
        p.setNachname(tfNachname.getText().trim());

        if (!tfGeburtsdatum.getText().isBlank()) {
            try {
                p.setGeburtsdatum(LocalDate.parse(tfGeburtsdatum.getText(), UI_DATE));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Geburtsdatum muss dd.MM.yyyy sein");
            }
        }

        p.setSvn(tfSvn.getText().trim());
        p.setAdresse(tfAdresse.getText().trim());
        p.setTelefon(tfTelefon.getText().trim());
        p.setEmail(tfEmail.getText().trim());
        p.setKasse(String.valueOf(cbKasse.getSelectedItem()));

        return p;
    }

    private void fillForm(Patient p) {
        tfPatientId.setText(p.getPatientId() == null ? "" : String.valueOf(p.getPatientId()));
        tfVorname.setText(nullToEmpty(p.getVorname()));
        tfNachname.setText(nullToEmpty(p.getNachname()));
        tfGeburtsdatum.setText(
                p.getGeburtsdatum() != null ? UI_DATE.format(p.getGeburtsdatum()) : ""
        );
        tfSvn.setText(nullToEmpty(p.getSvn()));
        tfAdresse.setText(nullToEmpty(p.getAdresse()));
        tfTelefon.setText(nullToEmpty(p.getTelefon()));
        tfEmail.setText(nullToEmpty(p.getEmail()));
        cbKasse.setSelectedItem(p.getKasse());
    }

    private void clearForm() {
        tfPatientId.setText("");
        tfVorname.setText("");
        tfNachname.setText("");
        tfGeburtsdatum.setText("");
        tfSvn.setText("");
        tfAdresse.setText("");
        tfTelefon.setText("");
        tfEmail.setText("");
        cbKasse.setSelectedIndex(0);
        taKrankengeschichte.setText("");
        patientList.clearSelection();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private interface SimpleDocumentListener extends javax.swing.event.DocumentListener {
        void update(javax.swing.event.DocumentEvent e);
        @Override default void insertUpdate(javax.swing.event.DocumentEvent e) { update(e); }
        @Override default void removeUpdate(javax.swing.event.DocumentEvent e) { update(e); }
        @Override default void changedUpdate(javax.swing.event.DocumentEvent e) { update(e); }
    }
}