

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class ManageUsersDialog extends JDialog {
    private DefaultTableModel usersModel;
    private JTable usersTable;

    public ManageUsersDialog(JFrame parent) {
        super(parent, "Manage Users", true);
        setSize(700, 400);
        setLocationRelativeTo(parent);

        JPanel mainPanel = new JPanel(new BorderLayout());

        usersModel = new DefaultTableModel(new Object[]{"ID", "Username", "Full Name", "Role", "Status"}, 0);
        usersTable = new JTable(usersModel);
        JScrollPane scrollPane = new JScrollPane(usersTable);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addButton = new JButton("Add");
        JButton editButton = new JButton("Edit");
        JButton refreshButton = new JButton("Refresh");
        JButton closeButton = new JButton("Close");

        addButton.addActionListener(e -> addUser());
        editButton.addActionListener(e -> editUser());
        refreshButton.addActionListener(e -> loadUsers());
        closeButton.addActionListener(e -> dispose());

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(closeButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(mainPanel);

        loadUsers();
    }

    private void loadUsers() {
        usersModel.setRowCount(0);
        String sql = "SELECT id, username, full_name, role, status FROM users ORDER BY username";

        try (Connection conn = DriverManager.getConnection(DatabaseSetup.DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                usersModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("full_name"),
                        rs.getString("role"),
                        rs.getString("status")
                });
            }
        } catch (SQLException e) {
            showError("Error loading users: " + e.getMessage());
        }
    }

    private void addUser() {
        UserDialog dialog = new UserDialog(this, null);
        dialog.setVisible(true);
        loadUsers();
    }

    private void editUser() {
        int selectedRow = usersTable.getSelectedRow();
        if (selectedRow == -1) {
            showWarning("Please select a user to edit");
            return;
        }

        int userId = (int) usersModel.getValueAt(selectedRow, 0);
        String oldStatus = (String) usersModel.getValueAt(selectedRow, 4);
        String newStatus = getNewStatusFromUserInput();

        if (oldStatus.equals(newStatus)) {
            showWarning("No changes made to the user status.");
            return;
        }

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DriverManager.getConnection(DatabaseSetup.DB_URL);
            conn.setAutoCommit(false);
            
            String updateSql = "UPDATE users SET status = ? WHERE id = ?";
            ps = conn.prepareStatement(updateSql);
            ps.setString(1, newStatus);
            ps.setInt(2, userId);
            ps.executeUpdate();

            logStatusChange(conn, userId, oldStatus, newStatus);
            conn.commit();
            
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            showError("Error updating user: " + e.getMessage());
        } finally {
            try {
                if (ps != null) ps.close();
                if (conn != null) conn.setAutoCommit(true);
                if (conn != null) conn.close();
            } catch (SQLException ignored) {}
        }

        loadUsers();
    }

    private void logStatusChange(Connection conn, int userId, String oldStatus, String newStatus) throws SQLException {
        String insertLogSql = "INSERT INTO user_activity_log (user_id, status_before, status_after, changed_by, change_date) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertLogSql)) {
            ps.setInt(1, userId);
            ps.setString(2, oldStatus);
            ps.setString(3, newStatus);
            ps.setInt(4, getCurrentAdminId());  // Updated to use admin's ID
            ps.setTimestamp(5, new Timestamp(System.currentTimeMillis()));  // Current timestamp
            ps.executeUpdate();
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    private int getCurrentAdminId() {
        // Assuming you have a way to track the logged-in admin's ID (for example, through a session)
        // Here it's hardcoded to 1, but you should fetch the real logged-in admin ID
        return 1; // Replace with actual logic for fetching the admin ID
    }

    private String getNewStatusFromUserInput() {
        return JOptionPane.showInputDialog(this, "Enter new status (Active/Inactive):");
    }
}
