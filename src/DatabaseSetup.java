import java.sql.*;

public class DatabaseSetup {
    public static final String DB_URL = "jdbc:sqlite:pos_system.db";

    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("PRAGMA foreign_keys = ON");

            // Create tables only if they don't exist (preserves existing data)
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT UNIQUE NOT NULL," +
                "password TEXT NOT NULL," +
                "role TEXT NOT NULL CHECK(role IN ('admin', 'cashier'))," +
                "full_name TEXT NOT NULL," +
                "status TEXT NOT NULL DEFAULT 'active' CHECK(status IN ('active', 'inactive')))");

            stmt.execute("CREATE TABLE IF NOT EXISTS products (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "barcode TEXT UNIQUE NOT NULL," +
                "name TEXT NOT NULL," +
                "description TEXT," +
                "price REAL NOT NULL," +
                "quantity INTEGER NOT NULL," +
                "category TEXT," +
                "reorder_level INTEGER DEFAULT 0," +
                "is_active INTEGER DEFAULT 1 CHECK(is_active IN (0, 1)))");

            stmt.execute("CREATE TABLE IF NOT EXISTS sales (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "sale_date TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "subtotal REAL NOT NULL," +
                "tax_amount REAL NOT NULL," +
                "discount_amount REAL NOT NULL," +
                "discount_type TEXT," +
                "total_amount REAL NOT NULL," +
                "user_id INTEGER NOT NULL," +
                "is_voided INTEGER DEFAULT 0 CHECK(is_voided IN (0, 1))," +
                "FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE)");

            // Safely alter voided_items table without losing data
            try {
                // First try to add the new columns if they don't exist
                stmt.execute("ALTER TABLE voided_items ADD COLUMN product_name TEXT");
                stmt.execute("ALTER TABLE voided_items ADD COLUMN barcode TEXT");
            } catch (SQLException e) {
                // Columns already exist or table doesn't exist
                if (e.getMessage().contains("no such table")) {
                    // Create the table if it doesn't exist
                    stmt.execute("CREATE TABLE IF NOT EXISTS voided_items (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "sale_id INTEGER," +
                        "product_id INTEGER," +
                        "product_name TEXT NOT NULL DEFAULT 'Unknown'," +
                        "barcode TEXT NOT NULL DEFAULT 'N/A'," +
                        "quantity INTEGER NOT NULL," +
                        "void_reason TEXT," +
                        "void_date TEXT DEFAULT CURRENT_TIMESTAMP," +
                        "void_by INTEGER NOT NULL," +
                        "original_price REAL NOT NULL," +
                        "discount_amount REAL DEFAULT 0," +
                        "FOREIGN KEY(sale_id) REFERENCES sales(id) ON DELETE SET NULL," +
                        "FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE SET NULL," +
                        "FOREIGN KEY(void_by) REFERENCES users(id))");
                }
            }

            // Other tables remain unchanged
            stmt.execute("CREATE TABLE IF NOT EXISTS sale_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "sale_id INTEGER NOT NULL," +
                "product_id INTEGER NOT NULL," +
                "quantity INTEGER NOT NULL," +
                "price REAL NOT NULL," +
                "vat REAL DEFAULT 0," +
                "discount REAL DEFAULT 0," +
                "FOREIGN KEY(sale_id) REFERENCES sales(id) ON DELETE CASCADE," +
                "FOREIGN KEY(product_id) REFERENCES products(id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS user_activity_log (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER NOT NULL," +
                "status_before TEXT," +
                "status_after TEXT," +
                "change_date TEXT DEFAULT CURRENT_TIMESTAMP," +
                "changed_by INTEGER," +
                "FOREIGN KEY(user_id) REFERENCES users(id)," +
                "FOREIGN KEY(changed_by) REFERENCES users(id))");

            // Ensure admin user exists
            stmt.execute("INSERT OR IGNORE INTO users (username, password, role, full_name, status) " +
                "VALUES ('admin', 'admin123', 'admin', 'System Administrator', 'active')");

        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static String hashPassword(String password) {
        return password; // In production, replace with proper hashing
    }
}