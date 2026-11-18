import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.List;

public class FinanceManagementSystem {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FinanceFrame().setVisible(true));
    }
}

class FinanceFrame extends JFrame {
    private JTable table;
    private DefaultTableModel model;
    private JTextField dateField, categoryField, amountField, noteField, monthField;
    private JComboBox<String> typeBox;
    private JLabel incomeLbl, expenseLbl, netLbl, rateLbl;
    private CategoryChart chart;
    private static final String FILE = "finance_data.csv";

    public FinanceFrame() {
        setTitle("Personal Finance Management System");
        setSize(1100, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // ---------- Table ----------
        model = new DefaultTableModel(new String[]{"Date", "Type", "Category", "Amount", "Note"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.setRowHeight(24);
        JScrollPane tableScroll = new JScrollPane(table);

        // ---------- Form ----------
        JPanel formPanel = new JPanel(new GridLayout(5, 2, 8, 8));
        formPanel.setBorder(BorderFactory.createTitledBorder("Transaction"));
        dateField = new JTextField(LocalDate.now().toString());
        typeBox = new JComboBox<>(new String[]{"Income", "Expense"});
        categoryField = new JTextField();
        amountField = new JTextField();
        noteField = new JTextField();
        formPanel.add(new JLabel("Date (yyyy-MM-dd):")); formPanel.add(dateField);
        formPanel.add(new JLabel("Type:"));              formPanel.add(typeBox);
        formPanel.add(new JLabel("Category:"));          formPanel.add(categoryField);
        formPanel.add(new JLabel("Amount:"));            formPanel.add(amountField);
        formPanel.add(new JLabel("Note:"));              formPanel.add(noteField);

        // ---------- Summary panel ----------
        JPanel summaryPanel = new JPanel(new BorderLayout(5, 5));
        summaryPanel.setBorder(BorderFactory.createTitledBorder("Monthly Summary"));
        summaryPanel.setPreferredSize(new Dimension(380, 0));

        JPanel monthRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        monthField = new JTextField(YearMonth.now().toString(), 8);
        JButton showBtn = new JButton("Show");
        monthRow.add(new JLabel("Month (yyyy-MM):"));
        monthRow.add(monthField);
        monthRow.add(showBtn);

        JPanel stats = new JPanel(new GridLayout(4, 2, 5, 5));
        incomeLbl = new JLabel("0.00");
        expenseLbl = new JLabel("0.00");
        netLbl = new JLabel("0.00");
        rateLbl = new JLabel("0.0 %");
        stats.add(new JLabel("Total Income:"));  stats.add(incomeLbl);
        stats.add(new JLabel("Total Expenses:")); stats.add(expenseLbl);
        stats.add(new JLabel("Net:"));           stats.add(netLbl);
        stats.add(new JLabel("Savings Rate:"));  stats.add(rateLbl);

        JPanel summaryTop = new JPanel(new BorderLayout());
        summaryTop.add(monthRow, BorderLayout.NORTH);
        summaryTop.add(stats, BorderLayout.CENTER);

        chart = new CategoryChart();
        summaryPanel.add(summaryTop, BorderLayout.NORTH);
        summaryPanel.add(chart, BorderLayout.CENTER);

        // ---------- Buttons ----------
        JButton addBtn = new JButton("Add");
        JButton updateBtn = new JButton("Update");
        JButton deleteBtn = new JButton("Delete");
        JButton saveBtn = new JButton("Save CSV");
        JButton loadBtn = new JButton("Load CSV");

        addBtn.addActionListener(e -> addRecord());
        updateBtn.addActionListener(e -> updateRecord());
        deleteBtn.addActionListener(e -> deleteRecord());
        saveBtn.addActionListener(e -> saveCSV());
        loadBtn.addActionListener(e -> { loadCSV(); refreshSummary(); });
        showBtn.addActionListener(e -> refreshSummary());
        monthField.addActionListener(e -> refreshSummary());

        JPanel btnPanel = new JPanel();
        btnPanel.add(addBtn); btnPanel.add(updateBtn); btnPanel.add(deleteBtn);
        btnPanel.add(saveBtn); btnPanel.add(loadBtn);

        // ---------- Selection listener ----------
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    dateField.setText(String.valueOf(model.getValueAt(row, 0)));
                    typeBox.setSelectedItem(String.valueOf(model.getValueAt(row, 1)));
                    categoryField.setText(String.valueOf(model.getValueAt(row, 2)));
                    amountField.setText(String.valueOf(model.getValueAt(row, 3)));
                    noteField.setText(String.valueOf(model.getValueAt(row, 4)));
                }
            }
        });

        // ---------- Layout ----------
        JPanel left = new JPanel(new BorderLayout(5, 5));
        left.add(formPanel, BorderLayout.NORTH);
        left.add(tableScroll, BorderLayout.CENTER);

        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(left, BorderLayout.CENTER);
        add(summaryPanel, BorderLayout.EAST);
        add(btnPanel, BorderLayout.SOUTH);

        loadCSV();
        refreshSummary();

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { saveCSV(); }
        });
    }

    // ---------- CRUD ----------
    private String[] readForm() {
        String date = dateField.getText().trim();
        String category = categoryField.getText().trim();
        String amountText = amountField.getText().trim();
        try {
            LocalDate.parse(date);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Date must be in yyyy-MM-dd format.");
            return null;
        }
        if (category.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Category is required.");
            return null;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountText);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Amount must be a positive number.");
            return null;
        }
        return new String[]{date, (String) typeBox.getSelectedItem(), category,
                String.format("%.2f", amount), noteField.getText().trim()};
    }

    private void addRecord() {
        String[] row = readForm();
        if (row == null) return;
        model.addRow(row);
        clearFields();
        refreshSummary();
    }

    private void updateRecord() {
        int sel = table.getSelectedRow();
        if (sel < 0) {
            JOptionPane.showMessageDialog(this, "Select a row to update.");
            return;
        }
        String[] row = readForm();
        if (row == null) return;
        for (int i = 0; i < 5; i++) model.setValueAt(row[i], sel, i);
        refreshSummary();
    }

    private void deleteRecord() {
        int sel = table.getSelectedRow();
        if (sel < 0) {
            JOptionPane.showMessageDialog(this, "Select a row to delete.");
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "Delete selected transaction?",
                "Confirm", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            model.removeRow(sel);
            clearFields();
            refreshSummary();
        }
    }

    private void clearFields() {
        dateField.setText(LocalDate.now().toString());
        typeBox.setSelectedIndex(0);
        categoryField.setText("");
        amountField.setText("");
        noteField.setText("");
    }

    // ---------- Monthly summary ----------
    private void refreshSummary() {
        String month = monthField.getText().trim();
        try {
            YearMonth.parse(month);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Month must be in yyyy-MM format.");
            return;
        }

        double income = 0, expense = 0;
        Map<String, Double> byCategory = new TreeMap<>();

        for (int i = 0; i < model.getRowCount(); i++) {
            String date = String.valueOf(model.getValueAt(i, 0));
            if (!date.startsWith(month)) continue;

            String type = String.valueOf(model.getValueAt(i, 1));
            String category = String.valueOf(model.getValueAt(i, 2));
            double amt;
            try {
                amt = Double.parseDouble(String.valueOf(model.getValueAt(i, 3)));
            } catch (NumberFormatException ex) {
                continue;
            }

            if (type.equals("Income")) {
                income += amt;
            } else {
                expense += amt;
                byCategory.merge(category, amt, Double::sum);
            }
        }

        double net = income - expense;
        double rate = income > 0 ? (net / income) * 100.0 : 0.0;

        incomeLbl.setText(String.format("%.2f", income));
        expenseLbl.setText(String.format("%.2f", expense));
        netLbl.setText(String.format("%.2f", net));
        netLbl.setForeground(net >= 0 ? new Color(0, 130, 0) : Color.RED);
        rateLbl.setText(String.format("%.1f %%", rate));
        chart.setData(byCategory, expense);
    }

    // ---------- CSV ----------
    private void saveCSV() {
        try (PrintWriter out = new PrintWriter(FILE)) {
            for (int i = 0; i < model.getRowCount(); i++) {
                StringBuilder sb = new StringBuilder();
                for (int c = 0; c < 5; c++) {
                    if (c > 0) sb.append(",");
                    sb.append(escapeCSV(String.valueOf(model.getValueAt(i, c))));
                }
                out.println(sb);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error saving CSV: " + e.getMessage());
        }
    }

    private void loadCSV() {
        try (Scanner sc = new Scanner(new File(FILE))) {
            model.setRowCount(0);
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                if (line.isBlank()) continue;
                List<String> parts = parseCSVLine(line);
                String[] row = new String[5];
                for (int i = 0; i < 5; i++) row[i] = i < parts.size() ? parts.get(i) : "";
                model.addRow(row);
            }
        } catch (FileNotFoundException fnf) {
            // first run, no file yet
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading CSV: " + e.getMessage());
        }
    }

    private String escapeCSV(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    // Handles commas inside quoted fields
    private List<String> parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQuotes) {
                if (ch == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"'); i++;
                } else if (ch == '"') {
                    inQuotes = false;
                } else {
                    cur.append(ch);
                }
            } else {
                if (ch == '"') inQuotes = true;
                else if (ch == ',') { result.add(cur.toString()); cur.setLength(0); }
                else cur.append(ch);
            }
        }
        result.add(cur.toString());
        return result;
    }

    // ---------- Spending-by-category bar chart ----------
    static class CategoryChart extends JPanel {
        private Map<String, Double> data = new TreeMap<>();
        private double total = 0;
        private final Color[] colors = {
            new Color(66, 133, 244), new Color(234, 67, 53), new Color(251, 188, 5),
            new Color(52, 168, 83), new Color(171, 71, 188), new Color(255, 112, 67)
        };

        CategoryChart() {
            setBorder(BorderFactory.createTitledBorder("Spending by Category"));
        }

        void setData(Map<String, Double> data, double total) {
            this.data = data;
            this.total = total;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (data.isEmpty()) {
                g2.drawString("No expenses for this month.", 20, 40);
                return;
            }

            int y = 30, barMaxW = getWidth() - 200, idx = 0;
            for (Map.Entry<String, Double> e : data.entrySet()) {
                double pct = total > 0 ? e.getValue() / total : 0;
                int w = (int) (barMaxW * pct);
                g2.setColor(Color.DARK_GRAY);
                g2.drawString(e.getKey(), 10, y + 12);
                g2.setColor(colors[idx % colors.length]);
                g2.fillRect(90, y, Math.max(w, 2), 16);
                g2.setColor(Color.DARK_GRAY);
                g2.drawString(String.format("%.2f (%.0f%%)", e.getValue(), pct * 100), 95 + w, y + 12);
                y += 28;
                idx++;
            }
        }
    }
}