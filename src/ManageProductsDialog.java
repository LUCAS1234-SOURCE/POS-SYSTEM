import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class ManageProductsDialog extends JDialog implements EventBus.DataUpdateListener {
    private DefaultTableModel productsModel;
    private JTable productsTable;
    
    public ManageProductsDialog(JFrame parent) {
        super(parent, "Manage Products", true);
        setSize(800, 600);
        setLocationRelativeTo(parent);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        productsModel = new DefaultTableModel(new Object[]{"ID", "Barcode", "Name", "Description", "Price", "Qty", "Category"}, 0);
        productsTable = new JTable(productsModel);
        JScrollPane scrollPane = new JScrollPane(productsTable);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addButton = new JButton("Add");
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");
        JButton refreshButton = new JButton("Refresh");
        JButton closeButton = new JButton("Close");
        
        addButton.addActionListener(e -> addProduct());
        editButton.addActionListener(e -> editProduct());
        deleteButton.addActionListener(e -> deleteProduct());
        refreshButton.addActionListener(e -> refreshProducts());
        closeButton.addActionListener(e -> dispose());
        
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(closeButton);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        loadProducts();

        // ===== MY FIX STARTS HERE =====
        EventBus.getInstance().registerListener(this);
        
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                EventBus.getInstance().unregisterListener(ManageProductsDialog.this);
            }
        });
        // ===== MY FIX ENDS HERE =====
    }

    // ===== MY FIX STARTS HERE =====
    @Override
    public void onDataUpdated(EventBus.UpdateType type) {
        if (type == EventBus.UpdateType.PRODUCTS_UPDATED && this.isVisible()) {
            SwingUtilities.invokeLater(() -> refreshProducts());
        }
    }
    // ===== MY FIX ENDS HERE =====
    
    void loadProducts() {
    // First clear the model and set column names
    productsModel.setRowCount(0);
    productsModel.setColumnIdentifiers(new String[]{
        "ID", 
        "Barcode", 
        "Name", 
        "Quantity", 
        "Reorder Level", 
        "Status", 
        "Price", 
        "Category"
    });
    
    String sql = "SELECT id, barcode, name, quantity, reorder_level, " +
                "CASE WHEN quantity <= reorder_level THEN 'LOW' ELSE 'OK' END AS stock_status, " +
                "price, category FROM products WHERE is_active = 1";
    
    try (Connection conn = DriverManager.getConnection(DatabaseSetup.DB_URL);
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
        
        while (rs.next()) {
            // Create a row with values in the correct order
            Object[] row = new Object[]{
                rs.getInt("id"),
                rs.getString("barcode"),
                rs.getString("name"),
                rs.getInt("quantity"),
                rs.getInt("reorder_level"),
                rs.getString("stock_status"),
                String.format("₱%.2f", rs.getDouble("price")), // Format price with currency
                rs.getString("category")
            };
            productsModel.addRow(row);
        }
        
        // Optional: Auto-resize columns after loading data
        for (int i = 0; i < productsModel.getColumnCount(); i++) {
            productsTable.getColumnModel().getColumn(i).setPreferredWidth(100);
        }
        
    } catch (SQLException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, 
            "Error loading products: " + e.getMessage(), 
            "Error", JOptionPane.ERROR_MESSAGE);
    }
}
    public void refreshProducts() {
        loadProducts();
    }
    
    private void addProduct() {
        ProductDialog dialog = new ProductDialog(this, null);
        dialog.setVisible(true);
        refreshProducts();
    }
    
    private void editProduct() {
        int selectedRow = productsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product to edit", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int productId = (int) productsModel.getValueAt(selectedRow, 0);
        ProductDialog dialog = new ProductDialog(this, productId);
        dialog.setVisible(true);
        refreshProducts();
    }
    
    private void deleteProduct() {
        int selectedRow = productsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a product to delete", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int productId = (int) productsModel.getValueAt(selectedRow, 0);
        String productName = (String) productsModel.getValueAt(selectedRow, 2);
        
        int confirm = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to delete '" + productName + "'?", 
                "Confirm Delete", 
                JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            String sql = "DELETE FROM products WHERE id = ?";
            
            try (Connection conn = DriverManager.getConnection(DatabaseSetup.DB_URL);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setInt(1, productId);
                pstmt.executeUpdate();
                refreshProducts();
                
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error deleting product: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

class ProductDialog extends JDialog {
    private JTextField barcodeField;
    private JTextField nameField;
    private JTextArea descriptionArea;
    private JTextField priceField;
    private JTextField quantityField;
    private JTextField categoryField;
    private Integer productId;
    
    public ProductDialog(JDialog parent, Integer productId) {
        super(parent, productId == null ? "Add Product" : "Edit Product", true);
        this.productId = productId;
        setSize(400, 400);
        setLocationRelativeTo(parent);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel formPanel = new JPanel(new GridLayout(7, 2, 5, 5));
        
        formPanel.add(new JLabel("Barcode:"));
        barcodeField = new JTextField();
        formPanel.add(barcodeField);
        
        formPanel.add(new JLabel("Name:"));
        nameField = new JTextField();
        formPanel.add(nameField);
        
        formPanel.add(new JLabel("Description:"));
        descriptionArea = new JTextArea(3, 20);
        JScrollPane scrollPane = new JScrollPane(descriptionArea);
        formPanel.add(scrollPane);
        
        formPanel.add(new JLabel("Price:"));
        priceField = new JTextField();
        formPanel.add(priceField);
        
        formPanel.add(new JLabel("Quantity:"));
        quantityField = new JTextField();
        formPanel.add(quantityField);
        
        formPanel.add(new JLabel("Category:"));
        categoryField = new JTextField();
        formPanel.add(categoryField);
        
        mainPanel.add(formPanel, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> saveProduct());
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        
        if (productId != null) {
            loadProductData();
        }
    }
    
    private void loadProductData() {
        String sql = "SELECT * FROM products WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(DatabaseSetup.DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, productId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                barcodeField.setText(rs.getString("barcode"));
                nameField.setText(rs.getString("name"));
                descriptionArea.setText(rs.getString("description"));
                priceField.setText(String.valueOf(rs.getDouble("price")));
                quantityField.setText(String.valueOf(rs.getInt("quantity")));
                categoryField.setText(rs.getString("category"));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading product data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void saveProduct() {
        try {
            String barcode = barcodeField.getText().trim();
            String name = nameField.getText().trim();
            String description = descriptionArea.getText().trim();
            double price = Double.parseDouble(priceField.getText().trim());
            int quantity = Integer.parseInt(quantityField.getText().trim());
            String category = categoryField.getText().trim();
            
            if (barcode.isEmpty() || name.isEmpty()) {
                throw new IllegalArgumentException("Barcode and name are required");
            }
            
            if (price <= 0 || quantity < 0) {
                throw new IllegalArgumentException("Price must be positive and quantity must be non-negative");
            }
            
            if (productId == null) {
                String sql = "INSERT INTO products (barcode, name, description, price, quantity, category) " +
                             "VALUES (?, ?, ?, ?, ?, ?)";
                
                try (Connection conn = DriverManager.getConnection(DatabaseSetup.DB_URL);
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    
                    pstmt.setString(1, barcode);
                    pstmt.setString(2, name);
                    pstmt.setString(3, description);
                    pstmt.setDouble(4, price);
                    pstmt.setInt(5, quantity);
                    pstmt.setString(6, category);
                    
                    pstmt.executeUpdate();
                    dispose();
                    
                } catch (SQLException e) {
                    if (e.getMessage().contains("UNIQUE constraint failed")) {
                        throw new IllegalArgumentException("A product with this barcode already exists");
                    }
                    throw new SQLException("Database error: " + e.getMessage());
                }
            } else {
                String sql = "UPDATE products SET barcode = ?, name = ?, description = ?, " +
                             "price = ?, quantity = ?, category = ? WHERE id = ?";
                
                try (Connection conn = DriverManager.getConnection(DatabaseSetup.DB_URL);
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    
                    pstmt.setString(1, barcode);
                    pstmt.setString(2, name);
                    pstmt.setString(3, description);
                    pstmt.setDouble(4, price);
                    pstmt.setInt(5, quantity);
                    pstmt.setString(6, category);
                    pstmt.setInt(7, productId);
                    
                    pstmt.executeUpdate();
                    dispose();
                    
                } catch (SQLException e) {
                    if (e.getMessage().contains("UNIQUE constraint failed")) {
                        throw new IllegalArgumentException("A product with this barcode already exists");
                    }
                    throw new SQLException("Database error: " + e.getMessage());
                }
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers for price and quantity", "Invalid Input", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException | SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}