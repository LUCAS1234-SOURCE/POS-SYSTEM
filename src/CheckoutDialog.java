import javax.swing.*;
import java.awt.*;
import java.awt.print.*;
import java.sql.*;
import java.text.DecimalFormat;
import javax.swing.table.DefaultTableModel;

public class CheckoutDialog extends JDialog {
    private final double subtotal;
    private final DefaultTableModel cartModel;
    private final int userId;
    private String cashierName;
    private final double TAX_RATE = 0.12;
    private final double DISCOUNT_RATE = 0.20;

    public CheckoutDialog(JFrame parent, double subtotal, DefaultTableModel cartModel, int userId) {
        super(parent, "Checkout", true);
        this.subtotal = subtotal;
        this.cartModel = cartModel;
        this.userId = userId;

        setSize(400, 400);
        setLocationRelativeTo(parent);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel calcPanel = new JPanel(new GridLayout(6, 2, 5, 5));
        DecimalFormat df = new DecimalFormat("\u20B1#,##0.00");

        calcPanel.add(new JLabel("Subtotal:"));
        JLabel subtotalLabel = new JLabel(df.format(subtotal));
        calcPanel.add(subtotalLabel);

        double tax = subtotal * TAX_RATE;
        calcPanel.add(new JLabel("VAT (12%):"));
        JLabel taxLabel = new JLabel(df.format(tax));
        calcPanel.add(taxLabel);

        calcPanel.add(new JLabel("Discount Type:"));
        JComboBox<String> discountCombo = new JComboBox<>(new String[]{"None", "Senior Citizen (20%)", "PWD (20%)"});
        calcPanel.add(discountCombo);

        calcPanel.add(new JLabel("Discount Amount:"));
        JLabel discountLabel = new JLabel(df.format(0));
        calcPanel.add(discountLabel);

        calcPanel.add(new JLabel("Total:"));
        JLabel totalLabel = new JLabel(df.format(subtotal + tax));
        calcPanel.add(totalLabel);

        calcPanel.add(new JLabel("Amount Paid:"));
        JTextField amountPaidField = new JTextField();
        calcPanel.add(amountPaidField);

        discountCombo.addActionListener(e -> {
            double discount = 0;
            if (discountCombo.getSelectedIndex() == 1 || discountCombo.getSelectedIndex() == 2) {
                discount = subtotal * DISCOUNT_RATE;
            }
            discountLabel.setText(df.format(discount));
            totalLabel.setText(df.format(subtotal + tax - discount));
        });

        mainPanel.add(calcPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton completeBtn = new JButton("Complete Payment");
        JButton cancelBtn = new JButton("Cancel");

        completeBtn.addActionListener(e -> {
            double amountPaid;
            try {
                amountPaid = Double.parseDouble(amountPaidField.getText().replace("\u20B1", "").replace(",", ""));
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid amount paid.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            double discount = parseCurrency(discountLabel.getText());
            processPayment(subtotal, tax, discount, (String) discountCombo.getSelectedItem(), amountPaid);
        });
        cancelBtn.addActionListener(e -> dispose());

        buttonPanel.add(completeBtn);
        buttonPanel.add(cancelBtn);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
        fetchCashierName();
    }

   private void fetchCashierName() {
    String query = "SELECT username FROM users WHERE id = ?";
    try (Connection conn = DriverManager.getConnection(DatabaseSetup.DB_URL);
         PreparedStatement stmt = conn.prepareStatement(query)) {
        stmt.setInt(1, userId); // Ensure userId is correctly passed
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            cashierName = rs.getString("username");
        } else {
            // Fallback if cashier name is not found
            cashierName = "Unknown Cashier"; // Or you can handle it as needed
        }
    } catch (SQLException e) {
        e.printStackTrace();
        cashierName = "Unknown Cashier"; // Handle any SQL errors gracefully
    }
}


    private double parseCurrency(String currencyString) {
        return Double.parseDouble(currencyString.replace("\u20B1", "").replace(",", ""));
    }

    private void processPayment(double subtotal, double tax, double discount, String discountType, double amountPaid) {
        double total = subtotal + tax - discount;

        if (amountPaid < total) {
            JOptionPane.showMessageDialog(this, "Insufficient funds. Total amount: \u20B1" + total, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double change = amountPaid - total;

        try (Connection conn = DriverManager.getConnection(DatabaseSetup.DB_URL)) {
            conn.setAutoCommit(false);

            // 1. Insert sale record (SQLite compatible)
            String saleSql = "INSERT INTO sales (sale_date, subtotal, tax_amount, discount_amount, discount_type, total_amount, user_id) " +
                           "VALUES (CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, ?)";
            
            int saleId;
            try (PreparedStatement saleStmt = conn.prepareStatement(saleSql)) {
                saleStmt.setDouble(1, subtotal);
                saleStmt.setDouble(2, tax);
                saleStmt.setDouble(3, discount);
                saleStmt.setString(4, discountType.equals("None") ? null : discountType);
                saleStmt.setDouble(5, total);
                saleStmt.setInt(6, userId);
                saleStmt.executeUpdate();

                // Get the inserted sale ID (SQLite specific)
                try (Statement idStmt = conn.createStatement();
                     ResultSet rs = idStmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (!rs.next()) {
                        conn.rollback();
                        JOptionPane.showMessageDialog(this, "Failed to create sale record", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    saleId = rs.getInt(1);
                }
            }

            // 2. Process each cart item
            String itemSql = "INSERT INTO sale_items (sale_id, product_id, quantity, price, vat) VALUES (?, ?, ?, ?, ?)";
            String updateQtySql = "UPDATE products SET quantity = quantity - ? WHERE id = ?";
            
            try (PreparedStatement itemStmt = conn.prepareStatement(itemSql);
                 PreparedStatement updateQtyStmt = conn.prepareStatement(updateQtySql)) {

                for (int i = 0; i < cartModel.getRowCount(); i++) {
                    // Get product details from cart - FIXED QUANTITY COLUMN INDEX HERE
                    int productId = getCartIntValue(i, 0, "Product ID");
                    int quantity = getCartIntValue(i, 4, "Quantity");  // Changed from 2 to 4
                    double price = getCartDoubleValue(i, 3, "Price");

                    // Insert sale item
                    itemStmt.setInt(1, saleId);
                    itemStmt.setInt(2, productId);
                    itemStmt.setInt(3, quantity);
                    itemStmt.setDouble(4, price);
                    itemStmt.setDouble(5, tax);
                    itemStmt.executeUpdate();

                    // Update product quantity
                    updateQtyStmt.setInt(1, quantity);
                    updateQtyStmt.setInt(2, productId);
                    int updatedRows = updateQtyStmt.executeUpdate();
                    
                    if (updatedRows == 0) {
                        conn.rollback();
                        JOptionPane.showMessageDialog(this, 
                            "Failed to update inventory for product ID: " + productId, 
                            "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                // Success - commit transaction
                conn.commit();
                
                // Print receipt and notify listeners
                printReceipt(saleId, change, subtotal, tax, discount, discountType, amountPaid);
                EventBus.getInstance().notifyDataUpdated(EventBus.UpdateType.PRODUCTS_UPDATED);
                EventBus.getInstance().notifyDataUpdated(EventBus.UpdateType.SALES_UPDATED);
                
                // Show success message
                JOptionPane.showMessageDialog(this, 
                    "Payment successful! Change: \u20B1" + change, 
                    "Success", JOptionPane.INFORMATION_MESSAGE);
                    
                // Clear cart and close
                cartModel.setRowCount(0);
                dispose();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Database error: " + ex.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, 
                ex.getMessage(), 
                "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int getCartIntValue(int row, int col, String fieldName) throws NumberFormatException {
        try {
            Object value = cartModel.getValueAt(row, col);
            if (value instanceof Integer) {
                return (Integer) value;
            }
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            throw new NumberFormatException("Invalid " + fieldName + " in cart at row " + (row+1));
        }
    }

    private double getCartDoubleValue(int row, int col, String fieldName) throws NumberFormatException {
        try {
            Object value = cartModel.getValueAt(row, col);
            if (value instanceof Double) {
                return (Double) value;
            }
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            throw new NumberFormatException("Invalid " + fieldName + " in cart at row " + (row+1));
        }
    }

   private void printReceipt(int saleId, double change, double subtotal, double tax, double discount, String discountType, double amountPaid) {
    try {
        PrinterJob job = PrinterJob.getPrinterJob();
        PageFormat format = job.defaultPage();
        Paper paper = format.getPaper();

        double width = 164;
        double height = 600;

        paper.setSize(width, height);
        paper.setImageableArea(0, 0, width, height);
        format.setPaper(paper);

        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;

            Graphics2D g2d = (Graphics2D) graphics;
            g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

            int y = 10;
            g2d.setFont(new Font("Monospaced", Font.BOLD, 10));
            g2d.drawString("  Sabelle's Clothing", 0, y);
            y += 15;

            g2d.setFont(new Font("Monospaced", Font.PLAIN, 10));
            g2d.drawString("****************************", 0, y); y += 15;

            g2d.drawString("Serial No.: " + saleId, 0, y); y += 10;
            g2d.drawString("Cashier", 0, y); y += 10;

            g2d.drawString("****************************", 0, y); y += 15;

            // Fetch and print each product in the sale
            String query = "SELECT p.name, si.quantity, si.price " +
                           "FROM sale_items si " +
                           "JOIN products p ON si.product_id = p.id " +
                           "WHERE si.sale_id = ?";
            try (Connection conn = DriverManager.getConnection(DatabaseSetup.DB_URL);
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, saleId);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    String productName = rs.getString("name");
                    int quantity = rs.getInt("quantity");
                    double price = rs.getDouble("price");

                    g2d.drawString(productName + " x" + quantity + " @ \u20B1" + String.format("%.2f", price), 0, y);
                    y += 15;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            g2d.drawString("****************************", 0, y); y += 15;

            g2d.drawString("VAT: \u20B1" + String.format("%.2f", tax), 0, y); y += 15;
            g2d.drawString("Discount: \u20B1" + String.format("%.2f", discount), 0, y); y += 15;
            g2d.drawString("Discount Type: " + discountType, 0, y); y += 15;

            g2d.drawString("****************************", 0, y); y += 15;

            double total = subtotal + tax - discount;
            g2d.drawString("Subtotal: \u20B1" + String.format("%.2f", subtotal), 0, y); y += 15;
            g2d.drawString("Total: \u20B1" + String.format("%.2f", total), 0, y); y += 15;
            g2d.drawString("Amount Paid: \u20B1" + String.format("%.2f", amountPaid), 0, y); y += 15;
            g2d.drawString("Change: \u20B1" + String.format("%.2f", change), 0, y); y += 15;

            g2d.drawString("****************************", 0, y); y += 15;
            g2d.drawString("  Thank you for shopping!", 0, y); y += 15;

            // Add current timestamp
            g2d.drawString("Date: " + java.time.LocalDateTime.now(), 0, y); y += 15;

            return Printable.PAGE_EXISTS;
        });

        if (job.printDialog()) {
            job.print();
        }
    } catch (PrinterException e) {
        e.printStackTrace();
    }
}

}