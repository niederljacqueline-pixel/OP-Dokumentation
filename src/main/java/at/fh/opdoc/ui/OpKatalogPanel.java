package at.fh.opdoc.ui;

import at.fh.opdoc.model.OpKatalogEintrag;
import at.fh.opdoc.repo.OpKatalogRepository;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class OpKatalogPanel extends JPanel {

    private final OpKatalogRepository repo;


    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> opList = new JList<>(listModel);
    private final JTextField searchField = new JTextField();

    private final JTextField tfOpId = new JTextField();
    private final JTextField tfName = new JTextField();
    private final JTextArea taBeschreibung = new JTextArea(8, 30);


    private List<OpKatalogEintrag> current = new ArrayList<>();


    public OpKatalogPanel(OpKatalogRepository repo) {
        this.repo = repo;

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        add(buildTop(), BorderLayout.NORTH);
        add(buildSplit(), BorderLayout.CENTER);

        hookEvents();
        loadAsync("");   // ✅ EINSTIEG

        SwingUtilities.invokeLater(() -> opList.requestFocusInWindow());
    }



    private JComponent buildTop() {
        JPanel top = new JPanel(new BorderLayout(10, 0));

        JLabel title = new JLabel("OP-Katalog");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        top.add(title, BorderLayout.WEST);

        JPanel search = new JPanel(new BorderLayout(6, 0));
        search.add(new JLabel("Suche:"), BorderLayout.WEST);
        search.add(searchField, BorderLayout.CENTER);
        top.add(search, BorderLayout.CENTER);

        JButton btnNeu = new JButton("Neu");
        btnNeu.addActionListener(e -> clearForm());
        top.add(btnNeu, BorderLayout.EAST);

        return top;
    }

    private JComponent buildSplit() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.30);
        split.setLeftComponent(buildLeft());
        split.setRightComponent(buildRight());
        return split;
    }

    private JComponent buildLeft() {
        opList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scroll = new JScrollPane(opList);
        scroll.setPreferredSize(new Dimension(260, 400));

        JPanel left = new JPanel(new BorderLayout(0, 8));
        left.add(new JLabel("OPs"), BorderLayout.NORTH);
        left.add(scroll, BorderLayout.CENTER);
        return left;
    }

    private JComponent buildRight() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Details"));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        int y = 0;
        tfOpId.setEditable(false);
        addRow(form, c, y++, "OP-ID:", tfOpId);
        addRow(form, c, y++, "Name:", tfName);

        c.gridx = 0; c.gridy = y; c.gridwidth = 2;
        form.add(new JLabel("Beschreibung:"), c);

        y++;
        taBeschreibung.setLineWrap(true);
        taBeschreibung.setWrapStyleWord(true);
        JScrollPane sp = new JScrollPane(taBeschreibung);

        c.gridx = 0; c.gridy = y;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        form.add(sp, c);

        y++;
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnDelete = new JButton("Löschen");
        JButton btnSave = new JButton("Speichern");
        buttons.add(btnDelete);
        buttons.add(btnSave);

        btnSave.addActionListener(e -> save());
        btnDelete.addActionListener(e -> delete());

        c.gridx = 0; c.gridy = y;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 0;
        form.add(buttons, c);

        return form;
    }


    private void loadAsync(String query) {

        new SwingWorker<List<OpKatalogEintrag>, Void>() {

            @Override
            protected List<OpKatalogEintrag> doInBackground() {
                return (query == null || query.isBlank())
                        ? repo.findAll()
                        : repo.search(query);
            }

            @Override
            protected void done() {
                try {
                    current = get();
                    listModel.clear();
                    for (OpKatalogEintrag op : current)
                        listModel.addElement(op.displayName());

                    if (!current.isEmpty())
                        opList.setSelectedIndex(0);
                    else
                        clearForm();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }


    private void hookEvents() {

        searchField.getDocument().addDocumentListener((SimpleDocumentListener) e ->
                loadAsync(searchField.getText().trim())
        );

        opList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int idx = opList.getSelectedIndex();
            if (idx >= 0 && idx < current.size())
                fill(current.get(idx));
        });
    }


    private void fill(OpKatalogEintrag op) {
        tfOpId.setText(op.getOpId() == null ? "" : String.valueOf(op.getOpId()));
        tfName.setText(op.getName() == null ? "" : op.getName());
        taBeschreibung.setText(op.getBeschreibung() == null ? "" : op.getBeschreibung());
    }

    private void clearForm() {
        tfOpId.setText("");
        tfName.setText("");
        taBeschreibung.setText("");
        opList.clearSelection();
    }

    private void save() {
        String name = tfName.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Name ist Pflicht.",
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        OpKatalogEintrag op = new OpKatalogEintrag();
        if (!tfOpId.getText().isBlank())
            op.setOpId(Integer.parseInt(tfOpId.getText().trim()));

        op.setName(name);
        op.setBeschreibung(taBeschreibung.getText().trim());

        repo.save(op);
        loadAsync(searchField.getText().trim());
    }

    private void delete() {
        if (tfOpId.getText().isBlank()) {
            JOptionPane.showMessageDialog(this,
                    "Kein OP-Eintrag ausgewählt.",
                    "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int res = JOptionPane.showConfirmDialog(
                this,
                "Eintrag wirklich löschen?",
                "Bestätigen",
                JOptionPane.YES_NO_OPTION);

        if (res != JOptionPane.YES_OPTION) return;

        repo.deleteById(Integer.parseInt(tfOpId.getText().trim()));
        loadAsync(searchField.getText().trim());
    }


    private void addRow(JPanel form, GridBagConstraints c,
                        int y, String label, JComponent field) {
        c.gridx = 0; c.gridy = y; c.weightx = 0.35;
        form.add(new JLabel(label), c);
        c.gridx = 1; c.weightx = 0.65;
        form.add(field, c);
    }

    private interface SimpleDocumentListener
            extends javax.swing.event.DocumentListener {

        void update(javax.swing.event.DocumentEvent e);

        @Override default void insertUpdate(javax.swing.event.DocumentEvent e) {
            update(e);
        }
        @Override default void removeUpdate(javax.swing.event.DocumentEvent e) {
            update(e);
        }
        @Override default void changedUpdate(javax.swing.event.DocumentEvent e) {
            update(e);
        }
    }
}