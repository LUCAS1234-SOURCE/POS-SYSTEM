import java.sql.*;

public class DatabaseHelper {
    private static final String DB_URL = "jdbc:sqlite:pos_system.db";
    
    public static Connection getConnection() throws SQLException {
        // Establish the connection
        Connection conn = DriverManager.getConnection(DB_URL);
        
        // Set to Serialized mode to prevent concurrency issues
        conn.setAutoCommit(false); // Disable autocommit for transactions
        
        // Enable Write-Ahead Logging (WAL) for better concurrency handling
        conn.prepareStatement("PRAGMA journal_mode = WAL;").execute();
        
        // Set the synchronous mode to NORMAL for better performance in concurrent environments
        conn.prepareStatement("PRAGMA synchronous = NORMAL;").execute();
        
        // Set a busy timeout to allow retries for locked database access (e.g., 30 seconds)
        conn.prepareStatement("PRAGMA busy_timeout = 30000;").execute();  // 30 seconds timeout
        
        return conn;
    }
    
    public static int getUserId(String username) throws SQLException {
        String sql = "SELECT id FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getInt("id") : -1;
        }
    }
    
    public static boolean productExists(String barcode) throws SQLException {
        String sql = "SELECT 1 FROM products WHERE barcode = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, barcode);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        }
    }

    // Wrap any complex database updates (e.g., add/update products) in a transaction
    public static void updateProductQuantity(String barcode, int quantity) throws SQLException {
        // Start a transaction
        try (Connection conn = getConnection()) {
            String sql = "UPDATE products SET quantity = quantity - ? WHERE barcode = ?";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, quantity);
                pstmt.setString(2, barcode);
                pstmt.executeUpdate();
                
                // Commit the transaction
                conn.commit();
            } catch (SQLException e) {
                // Rollback if an error occurs
                conn.rollback();
                throw e; // Re-throw the exception to be handled by the caller
            }
        }
    }
}
