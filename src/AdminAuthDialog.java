import java.awt.GridLayout;
import javax.swing.*;
import java.sql.*;

public class AdminAuthDialog extends JDialog {
    private boolean authenticated = false;
    private JTextField usernameField;
    private JPasswordField passwordField;

    public AdminAuthDialog(JFrame parent) {
        super(parent, "Admin Authentication", true);
        setSize(350, 200);
        setLocationRelativeTo(parent);
        setupUI();
    }

    private void setupUI() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        panel.add(new JLabel("Admin Username:"));
        usernameField = new JTextField();
        panel.add(usernameField);

        panel.add(new JLabel("Admin Password:"));
        passwordField = new JPasswordField();
        panel.add(passwordField);

        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> authenticate());
        panel.add(okButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        panel.add(cancelButton);

        add(panel);
    }

    private void authenticate() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        
        if (validateAdminCredentials(username, password)) {
            authenticated = true;
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, 
                "Invalid admin credentials", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean validateAdminCredentials(String username, String password) {
        String sql = "SELECT id FROM users WHERE username = ? AND password = ? AND role = 'admin' AND status = 'active'";
        
        try (Connection conn = DatabaseSetup.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, DatabaseSetup.hashPassword(password));
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next(); // Returns true if admin credentials are valid
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isAuthenticated() {
        return authenticated;
    }
}