package at.fh.opdoc.ui;

import at.fh.opdoc.model.*;
import at.fh.opdoc.repo.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;

public class OpPlanPanel extends JPanel {

    private final PatientRepository patientRepo;
    private final OpKatalogRepository opRepo;
    private final OpPlanRepository planRepo;

    private final Map<Integer, Patient> patientCache = new HashMap<>();
    private final Map<Integer, OpKatalogEintrag> opCache = new HashMap<>();

    private OpPlan selectedPlan;

    private final MonthView monthView = new MonthView();
    private final WeekScheduleView weekView = new WeekScheduleView();

    private final JComboBox<Patient> cbPatient = new JComboBox<>();
    private final JComboBox<OpKatalogEintrag> cbOp = new JComboBox<>();
    private final JComboBox<String> cbSaal =
            new JComboBox<>(new String[]{"OP1","OP2","OP3","OP4"});
    private final JTextField tfStart = new JTextField();
    private final JComboBox<Integer> cbDauer =
            new JComboBox<>(new Integer[]{30,45,60,90,120});
    private final JComboBox<String> cbStatus =
            new JComboBox<>(new String[]{"PLANNED","DONE","CANCELED"});
    private final JTextArea taBemerkung = new JTextArea(5,28);

    private final JButton btnNeu = new JButton("Neu");
    private final JButton btnSave = new JButton("Speichern");
    private final JButton btnDelete = new JButton("Löschen");

    private List<OpPlan> allPlans = new ArrayList<>();
    private LocalDate selectedDay = LocalDate.now();

    private final SimpleDateFormat df =
            new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public OpPlanPanel(PatientRepository patientRepo,
                       OpKatalogRepository opRepo,
                       OpPlanRepository planRepo) {

        this.patientRepo = patientRepo;
        this.opRepo = opRepo;
        this.planRepo = planRepo;

        setLayout(new BorderLayout(10,10));
        setBorder(new EmptyBorder(10,10,10,10));

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setDividerSize(8);
        mainSplit.setLeftComponent(buildLeft());
        mainSplit.setRightComponent(buildRight());

        add(mainSplit, BorderLayout.CENTER);

        hookEvents();
        loadAsync();

        SwingUtilities.invokeLater(() -> mainSplit.setDividerLocation(0.33));
    }


    private JComponent buildLeft() {
        JPanel monthBox = new JPanel(new BorderLayout());
        monthBox.setBorder(BorderFactory.createTitledBorder("Monatsübersicht"));
        monthBox.add(monthView, BorderLayout.CENTER);

        JPanel details = buildDetails();

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, monthBox, details);
        split.setResizeWeight(0.3);
        return split;
    }

    private JComponent buildRight() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("Wochenplan"));
        //p.add(new JScrollPane(weekView), BorderLayout.CENTER);
        JScrollPane scroll = new JScrollPane(
                weekView,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
        );

        scroll.getHorizontalScrollBar().setUnitIncrement(30);

        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildDetails() {
        JPanel box = new JPanel(new BorderLayout());
        box.setBorder(BorderFactory.createTitledBorder("Termin-Details"));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        int y=0;
        addRow(form,c,y++,"Patient:",cbPatient);
        addRow(form,c,y++,"OP:",cbOp);
        addRow(form,c,y++,"OP-Saal:",cbSaal);
        addRow(form,c,y++,"Start:",tfStart);
        addRow(form,c,y++,"Dauer:",cbDauer);
        addRow(form,c,y++,"Status:",cbStatus);

        c.gridx=0; c.gridy=y; c.gridwidth=2;
        form.add(new JLabel("Bemerkung:"),c);

        y++;
        JScrollPane sp = new JScrollPane(taBemerkung);
        c.gridy=y; c.fill=GridBagConstraints.BOTH; c.weighty=1;
        form.add(sp,c);

        box.add(form,BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.add(btnNeu); btns.add(btnDelete); btns.add(btnSave);
        box.add(btns,BorderLayout.SOUTH);

        return box;
    }

    private void addRow(JPanel p, GridBagConstraints c, int y,
                        String label, JComponent field) {
        c.gridx=0; c.gridy=y; c.weightx=0.35;
        p.add(new JLabel(label),c);
        c.gridx=1; c.weightx=0.65;
        p.add(field,c);
    }


    private void loadAsync() {

        new SwingWorker<Void,Void>() {

            List<Patient> patients;
            List<OpKatalogEintrag> ops;
            List<OpPlan> plans;

            @Override
            protected Void doInBackground() {
                patients = patientRepo.findAll();
                ops = opRepo.findAll();
                plans = planRepo.findAll();
                return null;
            }

            @Override
            protected void done() {
                patientCache.clear();
                for (Patient p:patients) patientCache.put(p.getPatientId(),p);
                opCache.clear();
                for (OpKatalogEintrag o:ops) opCache.put(o.getOpId(),o);

                cbPatient.removeAllItems();
                patients.forEach(cbPatient::addItem);

                cbOp.removeAllItems();
                ops.forEach(cbOp::addItem);

                allPlans = plans;
                updateWeek();
            }
        }.execute();
    }


    private void updateWeek() {
        LocalDate weekStart = weekStartFor(selectedDay);
        LocalDate weekEnd = weekStart.plusDays(6);

        List<OpPlan> weekPlans = new ArrayList<>();
        for (OpPlan p:allPlans) {
            if (p.getGeplantAm()==null) continue;
            LocalDate d = toLocalDate(p.getGeplantAm());
            if (!d.isBefore(weekStart) && !d.isAfter(weekEnd))
                weekPlans.add(p);
        }

        weekView.setWeek(weekStart, weekPlans, patientCache, opCache);
    }

    private void hookEvents() {

        monthView.addDateSelectionListener(d -> {
            selectedDay = d;
            updateWeek();
        });

        weekView.addPlanSelectionListener(p -> {
            selectedPlan = p;
            fillForm(p);
        });

        btnNeu.addActionListener(e -> clearFormForNew());
        btnSave.addActionListener(e -> savePlan());
        btnDelete.addActionListener(e -> deletePlan());
    }

    private void fillForm(OpPlan p) {
        select(cbPatient,p.getPatientId(),Patient::getPatientId);
        select(cbOp,p.getOpId(),OpKatalogEintrag::getOpId);
        cbSaal.setSelectedItem(p.getOpSaal());
        cbDauer.setSelectedItem(p.getDauerMin());
        cbStatus.setSelectedItem(p.getStatus());
        tfStart.setText(df.format(p.getGeplantAm()));
        taBemerkung.setText(p.getBemerkung());
    }

    private <T> void select(JComboBox<T> cb, Integer id,
                            java.util.function.Function<T,Integer> f) {
        for (int i=0;i<cb.getItemCount();i++)
            if (Objects.equals(f.apply(cb.getItemAt(i)),id))
                cb.setSelectedIndex(i);
    }

    private void clearFormForNew() {
        selectedPlan=null;
        cbPatient.setSelectedIndex(0);
        cbOp.setSelectedIndex(0);
        cbSaal.setSelectedIndex(0);
        cbDauer.setSelectedItem(60);
        cbStatus.setSelectedItem("PLANNED");
        taBemerkung.setText("");

        tfStart.setText(df.format(
                Date.from(LocalDate.now()
                        .atTime(9,0)
                        .atZone(ZoneId.systemDefault())
                        .toInstant())
        ));
    }

    private void savePlan() {
        try {
            OpPlan p = selectedPlan!=null?selectedPlan:new OpPlan();
            p.setPatientId(((Patient)cbPatient.getSelectedItem()).getPatientId());
            p.setOpId(((OpKatalogEintrag)cbOp.getSelectedItem()).getOpId());
            p.setOpSaal(String.valueOf(cbSaal.getSelectedItem()));
            p.setDauerMin((Integer)cbDauer.getSelectedItem());
            p.setStatus(String.valueOf(cbStatus.getSelectedItem()));
            p.setBemerkung(taBemerkung.getText().trim());
            p.setGeplantAm(df.parse(tfStart.getText().trim()));

            planRepo.save(p);
            loadAsync();
            selectedPlan=null;

        } catch (ParseException e) {
            JOptionPane.showMessageDialog(this,
                    "Datum: yyyy-MM-dd HH:mm",
                    "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deletePlan() {
        if (selectedPlan==null) return;
        planRepo.deleteById(selectedPlan.getOpPlanId());
        selectedPlan=null;
        loadAsync();
    }

    private static LocalDate toLocalDate(Date d) {
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static LocalDate weekStartFor(LocalDate d) {
        return d.minusDays((d.getDayOfWeek().getValue()+6)%7);
    }

    //Innere Klassen

    private static class MonthView extends JPanel {

        private final JLabel lblHeader = new JLabel("", SwingConstants.CENTER);
        private final JTable table;
        private final CalendarTableModel model = new CalendarTableModel();

        private LocalDate shownMonth = LocalDate.now().withDayOfMonth(1);
        private LocalDate selected = LocalDate.now();

        private final List<DateSelectionListener> listeners = new ArrayList<>();

        MonthView() {
            setLayout(new BorderLayout(4,4));

            JButton prev = new JButton("<");
            JButton next = new JButton(">");

            JPanel head = new JPanel(new BorderLayout());
            head.add(prev, BorderLayout.WEST);
            head.add(lblHeader, BorderLayout.CENTER);
            head.add(next, BorderLayout.EAST);
            add(head, BorderLayout.NORTH);

            table = new JTable(model);
            table.setRowHeight(26);
            table.setCellSelectionEnabled(true);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            add(new JScrollPane(table), BorderLayout.CENTER);

            prev.addActionListener(e -> {
                shownMonth = shownMonth.minusMonths(1);
                refresh();
            });

            next.addActionListener(e -> {
                shownMonth = shownMonth.plusMonths(1);
                refresh();
            });

            table.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    int r = table.rowAtPoint(e.getPoint());
                    int c = table.columnAtPoint(e.getPoint());
                    Integer day = model.getDayAt(r,c);
                    if (day==null) return;

                    LocalDate d = shownMonth.withDayOfMonth(day);
                    selected = d;
                    refresh();
                    fireSelected(d);
                }
            });

            refresh();
        }

        void addDateSelectionListener(DateSelectionListener l) {
            listeners.add(l);
        }

        private void fireSelected(LocalDate d) {
            for (DateSelectionListener l:listeners) l.onSelected(d);
        }

        private void refresh() {
            lblHeader.setText(
                    shownMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.GERMAN)
                            +" "+shownMonth.getYear()
            );
            model.setMonth(shownMonth);
        }

        interface DateSelectionListener {
            void onSelected(LocalDate date);
        }

        private static class CalendarTableModel extends AbstractTableModel {

            private final String[] cols = {"Mo","Di","Mi","Do","Fr","Sa","So"};
            private final Integer[][] grid = new Integer[6][7];

            void setMonth(LocalDate first) {
                for (int r=0;r<6;r++) Arrays.fill(grid[r],null);

                int len = first.lengthOfMonth();
                int startCol = (first.getDayOfWeek().getValue()+6)%7;

                int r=0,c=startCol;
                for (int d=1;d<=len;d++) {
                    grid[r][c]=d;
                    c++;
                    if (c==7) { c=0; r++; }
                }
                fireTableDataChanged();
            }

            Integer getDayAt(int r,int c) {
                return grid[r][c];
            }

            @Override public int getRowCount() { return 6; }
            @Override public int getColumnCount() { return 7; }
            @Override public String getColumnName(int c) { return cols[c]; }
            @Override public Object getValueAt(int r,int c) {
                return grid[r][c]==null?"":grid[r][c];
            }
        }
    }

    private static class WeekScheduleView extends JPanel {

        private LocalDate weekStart;
        private List<OpPlan> plans = new ArrayList<>();
        private Map<Integer, Patient> patientCache;
        private Map<Integer, OpKatalogEintrag> opCache;

        private final List<PlanSelectionListener> listeners = new ArrayList<>();

        // === Layout-Einstellungen ===
        private final String[] rooms = {"OP1", "OP2", "OP3"};

        private final LocalTime startTime = LocalTime.of(7, 0);
        private final LocalTime endTime   = LocalTime.of(18, 0);
        private final int slotMinutes = 15;

        private final int timeColumnWidth = 55;
        private final int headerHeight = 32;
        private final int rowHeight = 22;

        WeekScheduleView() {
            setBackground(Color.WHITE);
            ToolTipManager.sharedInstance().registerComponent(this);

            setPreferredSize(new Dimension(1600, 900));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    OpPlan hit = hitTest(e.getPoint());
                    if (hit != null) {
                        fireSelected(hit);
                    }
                }
            });
        }


        void setWeek(LocalDate start,
                     List<OpPlan> plans,
                     Map<Integer, Patient> patientCache,
                     Map<Integer, OpKatalogEintrag> opCache) {

            this.weekStart = start;
            this.plans = plans != null ? plans : new ArrayList<>();
            this.patientCache = patientCache;
            this.opCache = opCache;

            repaint();
        }

        void addPlanSelectionListener(PlanSelectionListener l) {
            listeners.add(l);
        }

        interface PlanSelectionListener {
            void onSelected(OpPlan plan);
        }

        private void fireSelected(OpPlan p) {
            for (PlanSelectionListener l : listeners) {
                l.onSelected(p);
            }
        }


        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (weekStart == null) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            try {
                int width = getWidth();
                int height = getHeight();

                int totalMinutes =
                        (int) Duration.between(startTime, endTime).toMinutes();
                int rows = totalMinutes / slotMinutes;

                int colsPerDay = rooms.length;
                int totalCols = 7 * colsPerDay;

                int colWidth =
                        Math.max(45, (width - timeColumnWidth) / Math.max(1, totalCols));

                // === Header ===
                g2.setColor(new Color(245, 245, 245));
                g2.fillRect(0, 0, width, headerHeight);

                // Day headers
                for (int d = 0; d < 7; d++) {
                    LocalDate date = weekStart.plusDays(d);
                    String label = date.getDayOfWeek()
                            .getDisplayName(TextStyle.SHORT, Locale.GERMAN)
                            + " " + date.getDayOfMonth() + ".";

                    int x = timeColumnWidth + d * colsPerDay * colWidth;
                    g2.setColor(Color.DARK_GRAY);
                    g2.drawString(label, x + 4, 16);

                    for (int r = 0; r < rooms.length; r++) {
                        int rx = x + r * colWidth;
                        g2.setColor(new Color(120, 120, 120));
                        g2.drawString(rooms[r], rx + 4, 30);
                    }
                }


                g2.setColor(new Color(220, 220, 220));

                for (int r = 0; r <= rows; r++) {
                    int y = headerHeight + r * rowHeight;
                    g2.drawLine(0, y, width, y);

                    if (r % (60 / slotMinutes) == 0) {
                        LocalTime t = startTime.plusMinutes((long) r * slotMinutes);
                        g2.setColor(Color.DARK_GRAY);
                        g2.drawString(
                                String.format("%02d:%02d", t.getHour(), t.getMinute()),
                                5, y - 3
                        );
                        g2.setColor(new Color(220, 220, 220));
                    }
                }

                for (int c = 0; c <= totalCols; c++) {
                    int x = timeColumnWidth + c * colWidth;
                    g2.drawLine(x, headerHeight, x, height);
                }


                for (OpPlan p : plans) {

                    if (p.getGeplantAm() == null || p.getOpSaal() == null) continue;

                    LocalDateTime dt =
                            p.getGeplantAm().toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDateTime();

                    int dayIdx = (int) Duration.between(
                            weekStart.atStartOfDay(),
                            dt.toLocalDate().atStartOfDay()
                    ).toDays();

                    if (dayIdx < 0 || dayIdx > 6) continue;

                    int roomIdx = indexOfRoom(p.getOpSaal());
                    if (roomIdx < 0) continue;

                    int minutesFromStart =
                            (int) Duration.between(startTime, dt.toLocalTime()).toMinutes();
                    if (minutesFromStart < 0) continue;

                    int y = headerHeight
                            + (minutesFromStart / slotMinutes) * rowHeight;

                    int dur = p.getDauerMin() != null ? p.getDauerMin() : 60;
                    int h = Math.max(rowHeight,
                            (dur / slotMinutes) * rowHeight);

                    int col =
                            dayIdx * rooms.length + roomIdx;
                    int x =
                            timeColumnWidth + col * colWidth;

                    Color fill = switch (safe(p.getStatus())) {
                        case "DONE" -> new Color(200, 255, 200);
                        case "CANCELED" -> new Color(255, 210, 210);
                        default -> new Color(210, 225, 255);
                    };

                    g2.setColor(fill);
                    g2.fillRoundRect(x + 2, y + 2,
                            colWidth - 4, h - 4, 10, 10);

                    g2.setColor(new Color(80, 80, 80));
                    g2.drawRoundRect(x + 2, y + 2,
                            colWidth - 4, h - 4, 10, 10);

                    g2.setClip(x + 2, y + 2, colWidth - 4, h - 4);

                    g2.setColor(Color.DARK_GRAY);
                    g2.drawString(
                            patientName(p.getPatientId()),
                            x + 6, y + 16
                    );

                    g2.setColor(new Color(60, 60, 60));
                    g2.drawString(
                            opName(p.getOpId()),
                            x + 6, y + 30
                    );

                    g2.setClip(null);
                }

            } finally {
                g2.dispose();
            }
        }



        private OpPlan hitTest(Point pt) {
            if (weekStart == null) return null;

            int totalCols = 7 * rooms.length;
            int colWidth = Math.max(45,
                    (getWidth() - timeColumnWidth) / Math.max(1, totalCols));

            for (OpPlan p : plans) {

                if (p.getGeplantAm() == null || p.getOpSaal() == null) continue;

                LocalDateTime dt =
                        p.getGeplantAm().toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime();

                int dayIdx = (int) Duration.between(
                        weekStart.atStartOfDay(),
                        dt.toLocalDate().atStartOfDay()
                ).toDays();

                int roomIdx = indexOfRoom(p.getOpSaal());
                if (dayIdx < 0 || dayIdx > 6 || roomIdx < 0) continue;

                int minutesFromStart =
                        (int) Duration.between(startTime, dt.toLocalTime()).toMinutes();
                if (minutesFromStart < 0) continue;

                int y = headerHeight
                        + (minutesFromStart / slotMinutes) * rowHeight;

                int dur = p.getDauerMin() != null ? p.getDauerMin() : 60;
                int h = Math.max(rowHeight,
                        (dur / slotMinutes) * rowHeight);

                int col = dayIdx * rooms.length + roomIdx;
                int x = timeColumnWidth + col * colWidth;

                Rectangle r = new Rectangle(x + 2, y + 2,
                        colWidth - 4, h - 4);

                if (r.contains(pt)) return p;
            }
            return null;
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            OpPlan p = hitTest(e.getPoint());
            if (p == null) return null;

            return "<html><b>" + escape(patientName(p.getPatientId())) + "</b><br>"
                    + escape(opName(p.getOpId())) + "<br>"
                    + "Saal: " + escape(p.getOpSaal()) + "<br>"
                    + "Status: " + escape(p.getStatus()) + "</html>";
        }


        private int indexOfRoom(String room) {
            for (int i = 0; i < rooms.length; i++)
                if (rooms[i].equalsIgnoreCase(room)) return i;
            return -1;
        }

        private String patientName(Integer id) {
            Patient p = patientCache != null ? patientCache.get(id) : null;
            return p != null ? p.displayName() : "Patient ?";
        }

        private String opName(Integer id) {
            OpKatalogEintrag o = opCache != null ? opCache.get(id) : null;
            return o != null ? o.displayName() : "OP ?";
        }

        private static String safe(String s) {
            return s == null ? "" : s;
        }

        private static String escape(String s) {
            if (s == null) return "";
            return s.replace("&","&amp;")
                    .replace("<","&lt;")
                    .replace(">","&gt;");
        }
    }
}