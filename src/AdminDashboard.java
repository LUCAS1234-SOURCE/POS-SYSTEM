import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class AdminDashboard extends JFrame {
    private String username;

    public AdminDashboard(String username) {
        this.username = username;
        setTitle("Sabelle's Closet - Admin Dashboard");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(new Color(245, 245, 220)); 
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };

       
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        
        ImageIcon logoIcon = new ImageIcon("C:\\Users\\lucas\\Documents\\POSSystem-20250403T174648Z-001\\POSSystem\\src\\images\\logo.jpg"); 
        Image logoImage = logoIcon.getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH); 
        logoIcon = new ImageIcon(logoImage);
        JLabel logoLabel = new JLabel(logoIcon);
        headerPanel.add(logoLabel, BorderLayout.WEST); 

       
        JLabel headerLabel = new JLabel("Sabelle's Clothing");
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        headerLabel.setForeground(new Color(90, 62, 43)); 
        headerPanel.add(headerLabel, BorderLayout.CENTER); 

       
        headerPanel.add(createLogoutButton(), BorderLayout.EAST); 

        mainPanel.add(headerPanel, BorderLayout.NORTH);

       
        JPanel welcomePanel = new JPanel();
        welcomePanel.setOpaque(false);
        JLabel welcomeLabel = new JLabel("Welcome, " + username + " (Admin)");
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        welcomeLabel.setForeground(new Color(40, 40, 40));
        welcomePanel.add(welcomeLabel);

        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setOpaque(false);

        
        buttonPanel.add(createStyledButton("Manage Users", e -> new ManageUsersDialog(this).setVisible(true)));
        buttonPanel.add(createStyledButton("Manage Products", e -> new ManageProductsDialog(this).setVisible(true)));
        buttonPanel.add(createStyledButton("View Reports", e -> new ReportsDialog(this).setVisible(true)));

        mainPanel.add(welcomePanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    
    private JButton createLogoutButton() {
        JButton logoutButton = new JButton("Logout");
        logoutButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        logoutButton.setBackground(new Color(101, 67, 33)); 
        logoutButton.setForeground(Color.BLACK); 
        logoutButton.setFocusPainted(false);
        logoutButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        logoutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        
        logoutButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                logoutButton.setBackground(new Color(139, 69, 19)); 
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                logoutButton.setBackground(new Color(101, 67, 33)); 
            }
        });

       
        logoutButton.addActionListener(e -> {
            dispose(); 
            new LoginFrame().setVisible(true); 
        });

        return logoutButton;
    }

    
    private JButton createStyledButton(String text, ActionListener action) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        button.setBackground(new Color(101, 67, 33)); 
        button.setForeground(Color.BLACK); 
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

       
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(139, 69, 19)); 
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(101, 67, 33)); 
            }
        });

        button.addActionListener(action);
        return button;
    }

    public static void main(String[] args) {
        new AdminDashboard("Admin").setVisible(true); 
    }
}
