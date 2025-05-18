import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.DecimalFormat;
import java.awt.print.*;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ReportsDialog extends JDialog implements EventBus.DataUpdateListener {
    private JComboBox<String> reportTypeCombo;
    private JTable reportTable;
    private DefaultTableModel reportModel;
    private JButton generateButton, printButton;
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("\u20B1#,##0.00");

    public ReportsDialog(JFrame parent) {
        super(parent, "Reports", true);
        setSize(800, 600);
        setLocationRelativeTo(parent);

        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel selectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        selectionPanel.add(new JLabel("Report Type:"));

        reportTypeCombo = new JComboBox<>(new String[] {
                "Sales Summary", "Sales by Date", "Top Selling Products", "Inventory Levels",
                "Void Products", "Daily Sales", "Weekly Sales", "Annual Sales", "User Activity Log"
        });
        selectionPanel.add(reportTypeCombo);

        generateButton = new JButton("Generate");
        generateButton.addActionListener(e -> generateReport());
        selectionPanel.add(generateButton);

        printButton = new JButton("Print");
        printButton.addActionListener(e -> printReport());
        printButton.setEnabled(false);
        selectionPanel.add(printButton);

        mainPanel.add(selectionPanel, BorderLayout.NORTH);

        reportModel = new DefaultTableModel();
        reportTable = new JTable(reportModel);
        JScrollPane scrollPane = new JScrollPane(reportTable);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        add(mainPanel);

        // ===== MY FIX STARTS HERE =====
        EventBus.getInstance().registerListener(this);
        
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                EventBus.getInstance().unregisterListener(ReportsDialog.this);
            }
        });
        // ===== MY FIX ENDS HERE =====
    }

    // ===== MY FIX STARTS HERE =====
    @Override
    public void onDataUpdated(EventBus.UpdateType type) {
        if (type == EventBus.UpdateType.SALES_UPDATED && this.isVisible()) {
            SwingUtilities.invokeLater(() -> refreshReportData());
        }
    }
    // ===== MY FIX ENDS HERE =====

    public void refreshInventorySalesData() {
        String sql = "SELECT p.name AS product_name, p.quantity AS stock_quantity, p.reorder_level " +
                     "FROM products p";
        try (Connection conn = DriverManager.getConnection(DatabaseSetup.DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String productName = rs.getString("product_name");
                int stockQuantity = rs.getInt("stock_quantity");
                int reorderLevel = rs.getInt("reorder_level");
                System.out.println("Product: " + productName + ", Stock Quantity: " + stockQuantity + ", Reorder Level: " + reorderLevel);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error refreshing inventory data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void refreshReportData() {
        DefaultTableModel model = (DefaultTableModel) reportTable.getModel();
        model.setRowCount(0);

        try (Connection conn = DriverManager.getConnection(DatabaseSetup.DB_URL)) {
            String query = "SELECT * FROM sales";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                Object[] row = {
                    rs.getInt("sale_id"),
                    rs.getDate("sale_date"),
                    rs.getDouble("total_amount"),
                    rs.getInt("user_id")
                };
                model.addRow(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void generateReport() {
        String reportType = (String) reportTypeCombo.getSelectedItem();
        try {
            switch (reportType) {
                case "Sales Summary": generateSalesSummary(); break;
                case "Sales by Date": generateSalesByDate(); break;
                case "Top Selling Products": generateTopSellingProducts(); break;
                case "Inventory Levels": generateInventoryLevels(); break;
                case "Void Products": generateVoidProducts(); break;
                case "Daily Sales": generateDailySales(); break;
                case "Weekly Sales": generateWeeklySales(); break;
                case "Annual Sales": generateAnnualSales(); break;
                case "User Activity Log": generateUserActivityLog(); break;
            }
            printButton.setEnabled(true);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error generating report: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void generateSalesSummary() throws SQLException {
        reportModel.setColumnIdentifiers(new Object[]{"Product", "Quantity Sold", "Total Sales"});
        reportModel.setRowCount(0);
        String sql = "SELECT p.name AS product_name, SUM(si.quantity) AS quantity_sold, SUM(si.quantity * si.price) AS total_sales " +
                     "FROM sale_items si JOIN products p ON si.product_id = p.id GROUP BY p.name";
        try (Connection conn = DriverManager.getConnection(DatabaseSetup.DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                reportModel.addRow(new Object[]{
                        rs.getString("product_name"),
                        rs.getInt("quantity_sold"),
                        CURRENCY_FORMAT.format(rs.getDouble("total_sales"))
                });
            }
        }
    }

    private void generateSalesByDate() throws SQLException {
        reportModel.setColumnIdentifiers(new String[]{"Date", "Total Sales"});
        reportModel.setRowCount(0);

        String query = "SELECT DATE(s.sale_date) AS sale_date, SUM(s.total_amount) AS total_sales FROM sales s GROUP BY DATE(s.sale_date)";

        try (Connection conn = DriverManager.getConnection(DatabaseSetup.DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                reportModel.addRow(new Object[]{
                        rs.getString("sale_date"),
                        CURRENCY_FORMAT.format(rs.getDouble("total_sales"))
                });
            }
        }
    }

    private void generateTopSellingProducts() throws SQLException {
        reportModel.setColumnIdentifiers(new Object[]{"Product", "Quantity Sold", "Total Sales"});
        reportModel.setRowCount(0);
        String sql = "SELECT p.name AS product_name, SUM(si.quantity) AS quantity_sold, SUM(si.quantity * si.price) AS total_sales " +
                     "FROM sale_items si JOIN products p ON si.product_id = p.id GROUP BY p.name ORDER BY quantity_sold DESC LIMIT 10";
        try (Connection conn = DriverManager.getConnection(DatabaseSetup.DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                reportModel.addRow(new Object[]{
                        rs.getString("product_name"),
                        rs.getInt("quantity_sold"),
                        CURRENCY_FORMAT.format(rs.getDouble("total_sales"))
                });
            }
        }
    }

    private void generateInventoryLevels() throws SQLException {
    reportModel.setColumnIdentifiers(new Object[]{"ID", "Product", "Current Qty", "Reorder Level", "Status"});
    reportModel.setRowCount(0);
    
    String sql = "SELECT id, name, quantity, reorder_level, " +
                "CASE WHEN quantity <= reorder_level THEN 'Low Stock' ELSE 'OK' END AS status " +
                "FROM products WHERE is_active = 1 ORDER BY name";
    
    try (Connection conn = DriverManager.getConnection(DatabaseSetup.DB_URL);
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
        
        while (rs.next()) {
            reportModel.addRow(new Object[]{
                rs.getInt("id"),
                rs.getString("name"),
                rs.getInt("quantity"),
                rs.getInt("reorder_level"),
                rs.getString("status")
            });
        }
    }
}

   private void generateVoidProducts() throws SQLException {
    reportModel.setColumnIdentifiers(new Object[]{
        "Void ID", "Product", "Qty", "Price", 
        "Reason", "Void By", "Date"
    });
    reportModel.setRowCount(0);
    
    String sql = "SELECT vi.id, vi.product_name, vi.quantity, " +
                "vi.original_price, vi.void_reason, " +
                "COALESCE(u.username, 'System') AS void_by, " +  // Never shows null
                "vi.void_date " +
                "FROM voided_items vi " +
                "LEFT JOIN users u ON vi.void_by = u.id " +
                "ORDER BY vi.void_date DESC";
    
    try (Connection conn = DatabaseSetup.getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        while (rs.next()) {
            reportModel.addRow(new Object[]{
                rs.getInt("id"),
                rs.getString("product_name"),
                rs.getInt("quantity"),
                CURRENCY_FORMAT.format(rs.getDouble("original_price")),
                rs.getString("void_reason"),
                rs.getString("void_by"),  // Will never be null
                dateFormat.format(rs.getTimestamp("void_date"))
            });
        }
    }
}

    private void generateDailySales() throws SQLException {
        reportModel.setColumnIdentifiers(new Object[]{"Date", "Total Sales"});
        reportModel.setRowCount(0);

        String sql = "SELECT DATE(s.sale_date) AS sale_date, SUM(s.total_amount) AS total_sales " +
                     "FROM sales s GROUP BY DATE(s.sale_date) ORDER BY sale_date DESC";
        try (Connection conn = DriverManager.getConnection(DatabaseSetup.DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            while (rs.next()) {
                String saleDateString = rs.getString("sale_date");
                java.sql.Date saleDate = null;
                try {
                    saleDate = new java.sql.Date(dateFormat.parse(saleDateString).getTime());
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Date parsing error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }

                String formattedDate = saleDate != null ? dateFormat.format(saleDate) : "N/A";

                reportModel.addRow(new Object[]{
                        formattedDate,
                        CURRENCY_FORMAT.format(rs.getDouble("total_sales"))
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error generating report: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void generateWeeklySales() throws SQLException {
        reportModel.setColumnIdentifiers(new Object[]{"Week", "Total Sales"});
        reportModel.setRowCount(0);
        String sql = "SELECT strftime('%W', s.sale_date) AS week, SUM(s.total_amount) AS total_sales " +
                     "FROM sales s GROUP BY strftime('%W', s.sale_date)";
        try (Connection conn = DriverManager.getConnection(DatabaseSetup.DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                reportModel.addRow(new Object[]{
                        rs.getInt("week"),
                        CURRENCY_FORMAT.format(rs.getDouble("total_sales"))
                });
            }
        }
    }

    private void generateAnnualSales() throws SQLException {
        reportModel.setColumnIdentifiers(new Object[]{"Year", "Total Sales"});
        reportModel.setRowCount(0);
        String sql = "SELECT strftime('%Y', s.sale_date) AS year, SUM(s.total_amount) AS total_sales " +
                     "FROM sales s GROUP BY strftime('%Y', s.sale_date)";
        try (Connection conn = DriverManager.getConnection(DatabaseSetup.DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                reportModel.addRow(new Object[]{
                        rs.getInt("year"),
                        CURRENCY_FORMAT.format(rs.getDouble("total_sales"))
                });
            }
        }
    }

   private void generateUserActivityLog() throws SQLException {
    reportModel.setColumnIdentifiers(new Object[]{"User", "Status Before", "Status After", "Changed By", "Change Date"});
    reportModel.setRowCount(0);
    
    String sql = "SELECT u.full_name, ual.status_before, ual.status_after, admin.full_name AS changed_by, ual.change_date " +
                 "FROM user_activity_log ual " +
                 "JOIN users u ON ual.user_id = u.id " +
                 "LEFT JOIN users admin ON ual.changed_by = admin.id";

    try (Connection conn = DriverManager.getConnection(DatabaseSetup.DB_URL);
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {

        while (rs.next()) {
            String formattedDate = "N/A";
            Object rawDate = rs.getObject("change_date");

            if (rawDate != null) {
                if (rawDate instanceof Timestamp) {
                    formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Timestamp) rawDate);
                } else if (rawDate instanceof java.sql.Date) {
                    formattedDate = new SimpleDateFormat("yyyy-MM-dd").format((java.sql.Date) rawDate);
                } else if (rawDate instanceof Long) {
                    // Handle UNIX timestamps (milliseconds)
                    formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date((Long) rawDate));
                } else if (rawDate instanceof String) {
                    String rawStr = (String) rawDate;
                    try {
                        // Try parsing as UNIX epoch
                        long unixTime = Long.parseLong(rawStr);
                        if (rawStr.length() > 10) {
                            // Assume milliseconds
                            formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(unixTime));
                        } else {
                            // Assume seconds
                            formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(unixTime * 1000));
                        }
                    } catch (NumberFormatException e) {
                        // Try parsing as date string
                        formattedDate = rawStr;
                    }
                }
            }

            reportModel.addRow(new Object[]{
                    rs.getString("full_name"),
                    rs.getString("status_before"),
                    rs.getString("status_after"),
                    rs.getString("changed_by"),
                    formattedDate
            });
        }
    }
}


    private void printReport() {
        try {
            MessageFormat headerFormat = new MessageFormat("POS System Reports");
            MessageFormat footerFormat = new MessageFormat("- {0} -");
            reportTable.print(JTable.PrintMode.FIT_WIDTH, headerFormat, footerFormat);
        } catch (PrinterException e) {
            JOptionPane.showMessageDialog(this, "Error printing report: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}