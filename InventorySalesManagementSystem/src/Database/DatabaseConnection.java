// DatabaseConnection.java
package Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // --- DATABASE CONNECTION DETAILS ---
    // Replace with your actual database URL, username, and password
    private static final String DB_URL = "jdbc:mysql://localhost:3306/inventory_db1";
    private static final String DB_USER = "root"; // Default XAMPP username
    private static final String DB_PASSWORD = "";   // Default XAMPP password is empty

    /**
     * Establishes and returns a connection to the database.
     * @return A Connection object or null if the connection fails.
     */
    public static Connection getConnection() {
        try {
            // Register the JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            // Attempt to connect to the database
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found!");
            e.printStackTrace();
            return null;
        } catch (SQLException e) {
            System.err.println("Failed to connect to the database!");
            e.printStackTrace();
            return null;
        }
    }
}