import javax.swing.*;
import java.util.regex.Pattern;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.stream.*;

public class CrimeRecordUI {

    // Simple demo credentials (for real app, store hashed passwords + secure auth)
    private static final Map<String, String> demoUsers = Map.of(
        "police", "12345",
        "admin", "admin123",
        "clerk", "clerk123"
    );

    private JFrame frame;
    private JTable table;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextField searchField;
    private String csvPath = "Crime_Investigation_Records.csv";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CrimeRecordUI app = new CrimeRecordUI();
            app.showLogin();
        });
    }

    private void showLogin() {
        // Create login panel
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.gridx = 0; c.gridy = 0;
        panel.add(new JLabel("Username:"), c);
        c.gridx = 1;
        JTextField userField = new JTextField(15);
        panel.add(userField, c);

        c.gridx = 0; c.gridy = 1;
        panel.add(new JLabel("Password:"), c);
        c.gridx = 1;
        JPasswordField passField = new JPasswordField(15);
        panel.add(passField, c);

        int option = JOptionPane.showConfirmDialog(
            null, panel, "Login - Crime Records System", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );

        if (option == JOptionPane.OK_OPTION) {
            String username = userField.getText().trim();
            String password = new String(passField.getPassword());

            if (authenticate(username, password)) {
                SwingUtilities.invokeLater(() -> createAndShowGUI(username));
            } else {
                JOptionPane.showMessageDialog(null, "Invalid credentials! Try again.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                showLogin(); // retry
            }
        } else {
            System.exit(0);
        }
    }

    private boolean authenticate(String user, String pass) {
        // demo check; replace with secure check for production
        return demoUsers.containsKey(user) && demoUsers.get(user).equals(pass);
    }

    private void createAndShowGUI(String username) {
        frame = new JFrame("Crime Records Management - Logged in as: " + username);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 600);
        frame.setLocationRelativeTo(null);

        // Top panel with controls
        JPanel topPanel = new JPanel(new BorderLayout(8, 8));
        JPanel leftTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftTop.add(new JLabel("Search (Case ID / Crime Type): "));
        searchField = new JTextField(30);
        leftTop.add(searchField);
        JButton searchBtn = new JButton("Search");
        leftTop.add(searchBtn);

        JButton clearBtn = new JButton("Clear");
        leftTop.add(clearBtn);

        topPanel.add(leftTop, BorderLayout.WEST);

        JPanel rightTop = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshBtn = new JButton("Refresh");
        JButton logoutBtn = new JButton("Logout");
        rightTop.add(refreshBtn);
        rightTop.add(logoutBtn);
        topPanel.add(rightTop, BorderLayout.EAST);

        // Table
        tableModel = new DefaultTableModel();
        table = new JTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setFillsViewportHeight(true);
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(table);

        // Load CSV data initially
        try {
            loadCSV(csvPath);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Error loading CSV: " + e.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
        }

        // Listeners
        searchBtn.addActionListener(e -> applySearch());
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            applySearch();
        });
        refreshBtn.addActionListener(e -> {
            try {
                loadCSV(csvPath);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Error refreshing CSV: " + ex.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        logoutBtn.addActionListener(e -> {
            frame.dispose();
            showLogin();
        });

        // Double click row to see details
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row != -1) {
                        int modelRow = table.convertRowIndexToModel(row);
                        showRecordDetails(modelRow);
                    }
                }
            }
        });

        frame.getContentPane().add(topPanel, BorderLayout.NORTH);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);

        frame.setVisible(true);
    }

    private void applySearch() {
        String text = searchField.getText().trim();
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            // Case-insensitive: filter across all columns but we prioritize Case_ID (col 0) and Crime Type (col 1)
            RowFilter<DefaultTableModel, Object> rf = RowFilter.regexFilter("(?i)" + Pattern.quote(text));
            sorter.setRowFilter(rf);
        }
    }

    private void loadCSV(String path) throws IOException {
        Path p = Paths.get(path);
        if (!Files.exists(p)) {
            throw new FileNotFoundException("CSV file not found at: " + path);
        }

        List<String> lines = Files.readAllLines(p);
        if (lines.isEmpty()) {
            throw new IOException("CSV file is empty");
        }

        // Parse header
        String headerLine = lines.get(0);
        String[] headers = splitCSVLine(headerLine);

        // Reset model
        tableModel.setColumnIdentifiers(headers);

        // Remove existing rows
        tableModel.setRowCount(0);

        // Add rows
        for (int i = 1; i < lines.size(); i++) {
            String[] row = splitCSVLine(lines.get(i));
            // If row has fewer columns, pad with empty strings
            if (row.length < headers.length) {
                String[] padded = new String[headers.length];
                System.arraycopy(row, 0, padded, 0, row.length);
                for (int j = row.length; j < headers.length; j++) padded[j] = "";
                row = padded;
            }
            tableModel.addRow(row);
        }

        // Resize columns to fit content (basic)
        for (int col = 0; col < table.getColumnCount(); col++) {
            TableColumn column = table.getColumnModel().getColumn(col);
            int width = 120; // default
            column.setPreferredWidth(width);
        }
    }

    private void showRecordDetails(int modelRow) {
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < tableModel.getColumnCount(); c++) {
            sb.append(tableModel.getColumnName(c)).append(": ").append(Objects.toString(tableModel.getValueAt(modelRow, c))).append("\n");
        }
        JTextArea ta = new JTextArea(sb.toString());
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(500, 400));
        JOptionPane.showMessageDialog(frame, sp, "Record Details", JOptionPane.INFORMATION_MESSAGE);
    }

    // Basic CSV splitting (handles simple CSV without embedded commas in quotes).
    private String[] splitCSVLine(String line) {
        // If your CSV uses quoted fields with commas, replace this with a proper CSV parser like OpenCSV.
        return Arrays.stream(line.split(",", -1)).map(String::trim).toArray(String[]::new);
    }
}
