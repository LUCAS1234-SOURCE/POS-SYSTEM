

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class UserDialog extends JDialog {
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JTextField fullNameField = new JTextField();
    private final JComboBox<String> roleCombo = new JComboBox<>(new String[]{"admin", "cashier"});
    private final JComboBox<String> statusCombo = new JComboBox<>(new String[]{"active", "inactive"});
    private final Integer userId;

    public UserDialog(JDialog parent, Integer userId) {
        super(parent, userId == null ? "Add User" : "Edit User", true);
        this.userId = userId;
        setSize(400, 300);
        setLocationRelativeTo(parent);
        
        JPanel formPanel = createFormPanel();
        JPanel buttonPanel = createButtonPanel();
        
        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        if (userId != null) loadUserData();
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridLayout(5, 2, 5, 5));
        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);
        panel.add(new JLabel("Full Name:"));
        panel.add(fullNameField);
        panel.add(new JLabel("Role:"));
        panel.add(roleCombo);
        panel.add(new JLabel("Status:"));
        panel.add(statusCombo);
        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> saveUser());
        cancelButton.addActionListener(e -> dispose());
        
        panel.add(saveButton);
        panel.add(cancelButton);
        return panel;
    }

    private void loadUserData() {
        try (Connection conn = DatabaseSetup.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT username, password, full_name, role, status FROM users WHERE id = ?"
             )) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                usernameField.setText(rs.getString("username"));
                passwordField.setText(rs.getString("password"));
                fullNameField.setText(rs.getString("full_name"));
                roleCombo.setSelectedItem(rs.getString("role"));
                statusCombo.setSelectedItem(rs.getString("status"));
            }
        } catch (SQLException e) {
            showError("Error loading user: " + e.getMessage());
        }
    }

    private void saveUser() {
        try {
            validateInputs();
            if (userId == null) {
                insertUser();
            } else {
                updateUser();
            }
            dispose();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void validateInputs() throws IllegalArgumentException {
        if (usernameField.getText().trim().isEmpty() ||
            passwordField.getPassword().length == 0 ||
            fullNameField.getText().trim().isEmpty()) {
            throw new IllegalArgumentException("All fields are required");
        }
    }

    private void insertUser() throws SQLException {
        try (Connection conn = DatabaseSetup.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "INSERT INTO users (username, password, role, full_name, status) VALUES (?, ?, ?, ?, ?)"
             )) {
            
            setUserParameters(pstmt);
            pstmt.setString(5, (String) statusCombo.getSelectedItem());
            pstmt.executeUpdate();
        }
    }

    private void updateUser() throws SQLException {
        try (Connection conn = DatabaseSetup.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "UPDATE users SET username=?, password=?, role=?, full_name=?, status=? WHERE id=?"
             )) {
            
            setUserParameters(pstmt);
            pstmt.setString(5, (String) statusCombo.getSelectedItem());
            pstmt.setInt(6, userId);
            pstmt.executeUpdate();
        }
    }

    private void setUserParameters(PreparedStatement pstmt) throws SQLException {
        pstmt.setString(1, usernameField.getText().trim());
        pstmt.setString(2, DatabaseSetup.hashPassword(new String(passwordField.getPassword()).trim()));
        pstmt.setString(3, (String) roleCombo.getSelectedItem());
        pstmt.setString(4, fullNameField.getText().trim());
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}