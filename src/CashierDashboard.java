import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.table.DefaultTableModel;


public class CashierDashboard extends JFrame {
    private String username;
    private DefaultTableModel cartModel;
    private JTable cartTable;
    private JLabel totalLabel;
    private JTextField barcodeField; // For barcode input (existing feature)
    private JTextField productIdField; // New field for manual product addition
    private double totalAmount = 0.0;
    private int userId;

    public CashierDashboard(String username) {
        this.username = username;
        this.userId = userId;
        setTitle("POS System - Cashier Dashboard");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(245, 245, 220));

        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerPanel.setBackground(new Color(139, 69, 19));
        JLabel headerLabel = new JLabel("Sabelles Clothing");
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerPanel.add(headerLabel);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Barcode input panel (existing)
        JPanel barcodePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        barcodePanel.setBackground(new Color(245, 245, 220));
        barcodePanel.add(new JLabel("Barcode:"));
        barcodeField = new JTextField(20);
        barcodePanel.add(barcodeField);

        barcodeField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    addProductByBarcode(barcodeField.getText());
                    barcodeField.setText(""); // Clear the field after scanning
                }
            }
        });
        mainPanel.add(barcodePanel, BorderLayout.WEST);

        // Manual Product ID input panel
        JPanel productIdPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        productIdPanel.setBackground(new Color(245, 245, 220));
        productIdPanel.add(new JLabel("Product ID:"));
        productIdField = new JTextField(20);
        productIdPanel.add(productIdField);

        productIdField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    addProductById(productIdField.getText());
                    productIdField.setText(""); // Clear the input field after adding
                }
            }
        });
        mainPanel.add(productIdPanel, BorderLayout.NORTH); // Add this panel at the top

        // Cart Table to display items
        cartModel = new DefaultTableModel(new Object[]{"ID", "Barcode", "Name", "Price", "Qty", "Subtotal"}, 0);
        cartTable = new JTable(cartModel);
        JScrollPane scrollPane = new JScrollPane(cartTable);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(new Color(245, 245, 220));

        JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        totalPanel.add(new JLabel("Total:"));
        totalLabel = new JLabel("$0.00");
        totalPanel.add(totalLabel);
        bottomPanel.add(totalPanel, BorderLayout.NORTH);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton checkoutButton = new JButton("Checkout");
        JButton cancelButton = new JButton("Cancel Sale");
        JButton logoutButton = new JButton("Logout");
        JButton voidButton = new JButton("Void Item");

        Color brown = new Color(139, 69, 19);
        checkoutButton.setBackground(brown);
        checkoutButton.setForeground(Color.BLACK);
        cancelButton.setBackground(brown);
        cancelButton.setForeground(Color.BLACK);
        logoutButton.setBackground(brown);
        logoutButton.setForeground(Color.BLACK);
        voidButton.setBackground(brown);
        voidButton.setForeground(Color.BLACK);

        checkoutButton.addActionListener(e -> checkout());
        cancelButton.addActionListener(e -> cancelSale());
        logoutButton.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });
        voidButton.addActionListener(e -> voidItem());

        buttonsPanel.add(cancelButton);
        buttonsPanel.add(checkoutButton);
        buttonsPanel.add(voidButton);
        buttonsPanel.add(logoutButton);
        bottomPanel.add(buttonsPanel, BorderLayout.SOUTH);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        add(mainPanel);
    }

    // Method to add product by Barcode
    private void addProductByBarcode(String barcode) {
        String sql = "SELECT * FROM products WHERE barcode = ?";
        try (Connection conn = DriverManager.getConnection(DatabaseSetup.DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, barcode);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int productId = rs.getInt("id");
                String productName = rs.getString("name");
                double price = rs.getDouble("price");
                int quantity = rs.getInt("quantity"); // Changed 'stock' to 'quantity'
                if (quantity <= 0) {
                    JOptionPane.showMessageDialog(this, "Product out of stock!", "Error", JOptionPane.WARNING_MESSAGE);
                    return;  // Don't add the product if it's out of stock
                }

                int qty = 1;
                boolean found = false;
                for (int i = 0; i < cartModel.getRowCount(); i++) {
                    if ((int) cartModel.getValueAt(i, 0) == productId) {
                        int currentQty = (int) cartModel.getValueAt(i, 4);
                        // Check if adding 1 more would exceed available stock
                        if (currentQty + 1 > quantity) {
                            JOptionPane.showMessageDialog(this, "Cannot add more than available stock", "Stock Error", JOptionPane.WARNING_MESSAGE);
                            return;  // Don't add more if it exceeds stock
                        }
                        cartModel.setValueAt(currentQty + 1, i, 4);
                        cartModel.setValueAt((currentQty + 1) * price, i, 5);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    cartModel.addRow(new Object[]{productId, barcode, productName, price, qty, price * qty});
                }
                updateTotal();
            } else {
                JOptionPane.showMessageDialog(this, "Product not found!", "Error", JOptionPane.WARNING_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error occurred. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    // Method to manually add product by Product ID
    private void addProductById(String productId) {
        String sql = "SELECT * FROM products WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DatabaseSetup.DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, productId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("id");
                String productName = rs.getString("name");
                double price = rs.getDouble("price");
                int quantity = rs.getInt("quantity"); // Changed 'stock' to 'quantity'
                if (quantity <= 0) {
                    JOptionPane.showMessageDialog(this, "Product out of stock!", "Error", JOptionPane.WARNING_MESSAGE);
                    return;  // Don't add the product if it's out of stock
                }

                int qty = 1;
                boolean found = false;
                for (int i = 0; i < cartModel.getRowCount(); i++) {
                    if ((int) cartModel.getValueAt(i, 0) == id) {
                        int currentQty = (int) cartModel.getValueAt(i, 4);
                        // Check if adding 1 more would exceed available stock
                        if (currentQty + 1 > quantity) {
                            JOptionPane.showMessageDialog(this, "Cannot add more than available stock", "Stock Error", JOptionPane.WARNING_MESSAGE);
                            return;  // Don't add more if it exceeds stock
                        }
                        cartModel.setValueAt(currentQty + 1, i, 4);
                        cartModel.setValueAt((currentQty + 1) * price, i, 5);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    cartModel.addRow(new Object[]{id, productId, productName, price, qty, price * qty});
                }
                updateTotal();
            } else {
                JOptionPane.showMessageDialog(this, "Product not found!", "Error", JOptionPane.WARNING_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error occurred. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void updateTotal() {
        totalAmount = 0.0;
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            totalAmount += (double) cartModel.getValueAt(i, 5); // Sum up subtotal
        }
        totalLabel.setText(String.format("$%.2f", totalAmount));
    }

    private void checkout() {
        if (cartModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Cart is empty", "Checkout Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        new CheckoutDialog(this, totalAmount, cartModel, userId).setVisible(true);
    }

    private void cancelSale() {
        if (cartModel.getRowCount() > 0) {
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to cancel the sale?", "Cancel Sale", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                cartModel.setRowCount(0);
                totalAmount = 0.0;
                totalLabel.setText("$0.00");
            }
        }
    }

    private void voidItem() {
    int selectedRow = cartTable.getSelectedRow();
    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(this, "Please select an item to void", 
            "Void Error", JOptionPane.WARNING_MESSAGE);
        return;
    }

    // Show admin authentication dialog
    AdminAuthDialog authDialog = new AdminAuthDialog(this);
    authDialog.setVisible(true);

    if (authDialog.isAuthenticated()) {
        voidSelectedItem(selectedRow);
    }
}

private void voidSelectedItem(int selectedRow) {
    try {
        int productId = (int) cartModel.getValueAt(selectedRow, 0);
        String productName = (String) cartModel.getValueAt(selectedRow, 2);
        double price = (double) cartModel.getValueAt(selectedRow, 3);
        int quantity = (int) cartModel.getValueAt(selectedRow, 4);
        String barcode = (String) cartModel.getValueAt(selectedRow, 1);
        
        // Show reason dialog
        String reason = JOptionPane.showInputDialog(this, 
            "Enter reason for voiding " + productName, "Void Reason", JOptionPane.QUESTION_MESSAGE);
        if (reason == null || reason.trim().isEmpty()) {
            return; // User cancelled or didn't enter reason
        }

        // Record void transaction
        recordVoidTransaction(productId, productName, barcode, price, quantity, reason);
        
        // Remove from cart
        cartModel.removeRow(selectedRow);
        updateTotal();
        
        // Update inventory
        updateInventoryAfterVoid(productId, quantity);
        
        JOptionPane.showMessageDialog(this, "Item voided successfully", 
            "Success", JOptionPane.INFORMATION_MESSAGE);
        
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error voiding item: " + e.getMessage(), 
            "Error", JOptionPane.ERROR_MESSAGE);
    }
}

private void recordVoidTransaction(int productId, String productName, String barcode, 
        double price, int quantity, String reason) throws SQLException {
    String sql = "INSERT INTO voided_items (" +
                 "product_id, product_name, barcode, quantity, " +
                 "void_reason, void_by, original_price" +
                 ") VALUES (?, ?, ?, ?, ?, ?, ?)";
    
    try (Connection conn = DatabaseSetup.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, productId);
        pstmt.setString(2, productName != null ? productName : "Unknown");
        pstmt.setString(3, barcode != null ? barcode : "N/A");
        pstmt.setInt(4, quantity);
        pstmt.setString(5, reason);
        pstmt.setInt(6, this.userId);
        pstmt.setDouble(7, price);
        pstmt.executeUpdate();
    }
}

private void updateInventoryAfterVoid(int productId, int quantity) throws SQLException {
    String sql = "UPDATE products SET quantity = quantity + ? WHERE id = ?";
    
    try (Connection conn = DatabaseSetup.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, quantity);
        pstmt.setInt(2, productId);
        int updated = pstmt.executeUpdate();
        
        if (updated == 0) {
            throw new SQLException("Product not found in inventory");
        }
    }
}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CashierDashboard("Cashier1").setVisible(true));
    }
}
