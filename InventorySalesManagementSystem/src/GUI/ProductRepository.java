package GUI; // Or ideally, a 'Database' package

import Database.DatabaseConnection;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles all database operations for products, categories, and sales
 * for a specific store.
 */
public class ProductRepository {

    private final int storeId;

    public ProductRepository(int storeId) {
        this.storeId = storeId;
    }

    /**
     * Fetches all products for the current store from the database.
     * @return A list of Product objects.
     * @throws SQLException if a database error occurs.
     */
    public List<Product> getAllProducts() throws SQLException {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT p.product_id, p.name, c.category_name, p.price, p.selling_price, p.quantity_in_stock " +
                     "FROM products p " +
                     "JOIN categories c ON p.category_id = c.category_id " +
                     "WHERE p.store_id = ? " +
                     "ORDER BY p.product_id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, this.storeId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    double costPrice = rs.getDouble("price");
                    int stock = rs.getInt("quantity_in_stock");
                    double totalCost = costPrice * stock;
                    double sellingPrice = rs.getObject("selling_price") == null ? 0.0 : rs.getDouble("selling_price");

                    products.add(new Product(
                        rs.getInt("product_id"),
                        rs.getString("name"),
                        rs.getString("category_name"),
                        costPrice,
                        sellingPrice,
                        stock,
                        totalCost
                    ));
                }
            }
        }
        return products;
    }

    /**
     * Inserts or updates a product based on a JSON object (from QR code).
     * This method adds to existing stock and logs the change.
     * @param data The JSON object with product data.
     * @return The number of affected rows (1 for insert, >1 for update).
     * @throws SQLException if a database error occurs.
     * @throws JSONException if JSON parsing fails.
     */
    public int upsertProductFromQR(JSONObject data) throws SQLException, JSONException {
        String upsertSql = "INSERT INTO products (product_id, name, price, selling_price, quantity_in_stock, category_id, store_id) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                           "ON DUPLICATE KEY UPDATE " +
                           "name = VALUES(name), price = VALUES(price), " +
                           "selling_price = VALUES(selling_price), " +
                           "quantity_in_stock = quantity_in_stock + VALUES(quantity_in_stock), category_id = VALUES(category_id)";

        String logSql = "INSERT INTO stock_log (product_id, quantity_changed, log_type, notes, store_id) VALUES (?, ?, 'STOCK-IN', 'From QR Scan', ?)";

        int productId = data.getInt("id");
        String name = data.getString("name");
        int categoryId = data.getInt("category_id");
        double costPrice = data.getDouble("cost_price");
        double sellingPrice = data.getDouble("selling_price");
        int stockToAdd = data.getInt("stock");

        int affectedRows = 0;

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Start transaction

            try (PreparedStatement upsertPstmt = conn.prepareStatement(upsertSql)) {
                upsertPstmt.setInt(1, productId);
                upsertPstmt.setString(2, name);
                upsertPstmt.setDouble(3, costPrice);
                upsertPstmt.setDouble(4, sellingPrice);
                upsertPstmt.setInt(5, stockToAdd);
                upsertPstmt.setInt(6, categoryId);
                upsertPstmt.setInt(7, this.storeId);

                affectedRows = upsertPstmt.executeUpdate();
            }

            if (affectedRows > 0) {
                try (PreparedStatement logPstmt = conn.prepareStatement(logSql)) {
                    logPstmt.setInt(1, productId);
                    logPstmt.setInt(2, stockToAdd);
                    logPstmt.setInt(3, this.storeId);
                    logPstmt.executeUpdate();
                }
            } else {
                throw new SQLException("Upsert operation affected 0 rows for product ID: " + productId);
            }

            conn.commit(); // Commit transaction
            return affectedRows;

        } catch (SQLException e) {
            // Rollback is handled automatically by try-with-resources if conn.commit() isn't reached
            throw new SQLException("Error during QR database operation: " + e.getMessage(), e);
        }
    }
    
    /**
     * Manually inserts or updates a product (from dialog).
     * This method SETS the stock quantity and does not create a stock log.
     * @return True if the operation was successful.
     * @throws SQLException if a database error occurs.
     */
    public boolean manualUpsertProduct(int id, String name, int categoryId, double costPrice, double sellingPrice, int stock) throws SQLException {
        String sql = "INSERT INTO products (product_id, name, price, selling_price, quantity_in_stock, category_id, store_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE " +
                     "name = VALUES(name), price = VALUES(price), " +
                     "selling_price = VALUES(selling_price), " +
                     "quantity_in_stock = VALUES(quantity_in_stock), category_id = VALUES(category_id)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            pstmt.setString(2, name);
            pstmt.setDouble(3, costPrice);
            pstmt.setDouble(4, sellingPrice);
            pstmt.setInt(5, stock);
            pstmt.setInt(6, categoryId);
            pstmt.setInt(7, this.storeId);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Processes a sale, updating stock and creating sales records.
     * @param productId The product ID to sell.
     * @param quantityToSell The quantity to sell.
     * @param sellingPrice The price per item.
     * @throws SQLException if a database error occurs (e.g., not found, not enough stock).
     */
    public void sellProduct(String productId, int quantityToSell, double sellingPrice) throws SQLException {
        String findProductSql = "SELECT quantity_in_stock FROM products WHERE product_id = ? AND store_id = ?";
        String updateStockSql = "UPDATE products SET quantity_in_stock = ? WHERE product_id = ? AND store_id = ?";
        String salesSql = "INSERT INTO sales (sale_date, total_amount, store_id) VALUES (NOW(), ?, ?)";
        String saleItemsSql = "INSERT INTO sale_items (sale_id, product_id, quantity_sold, unit_price) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Start transaction

            int currentStock;
            try (PreparedStatement findPstmt = conn.prepareStatement(findProductSql)) {
                findPstmt.setString(1, productId);
                findPstmt.setInt(2, this.storeId);
                try (ResultSet rs = findPstmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Product not found in this store.");
                    }
                    currentStock = rs.getInt("quantity_in_stock");
                }
            }

            if (currentStock < quantityToSell) {
                throw new SQLException("Not enough stock in this store.");
            }

            try (PreparedStatement updatePstmt = conn.prepareStatement(updateStockSql)) {
                updatePstmt.setInt(1, currentStock - quantityToSell);
                updatePstmt.setString(2, productId);
                updatePstmt.setInt(3, this.storeId);
                updatePstmt.executeUpdate();
            }

            long saleId;
            try (PreparedStatement salesPstmt = conn.prepareStatement(salesSql, Statement.RETURN_GENERATED_KEYS)) {
                salesPstmt.setDouble(1, sellingPrice * quantityToSell);
                salesPstmt.setInt(2, this.storeId);
                salesPstmt.executeUpdate();
                try (ResultSet generatedKeys = salesPstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        saleId = generatedKeys.getLong(1);
                    } else {
                        throw new SQLException("Creating sale failed, no ID obtained.");
                    }
                }
            }

            try (PreparedStatement saleItemsPstmt = conn.prepareStatement(saleItemsSql)) {
                saleItemsPstmt.setLong(1, saleId);
                saleItemsPstmt.setString(2, productId);
                saleItemsPstmt.setInt(3, quantityToSell);
                saleItemsPstmt.setDouble(4, sellingPrice);
                saleItemsPstmt.executeUpdate();
            }
            
            conn.commit(); // Commit transaction

        } catch (SQLException e) {
            // Rollback is automatic
            throw e; // Re-throw the specific exception
        }
    }
    
    /**
     * Gets the selling price for a product. // <-- Updated doc comment
     * @param productId The product ID.
     * @return The selling price. Returns 0.0 if selling price is NULL in DB.
     * @throws SQLException if product not found or database error.
     */
    public double getProductSellingPrice(String productId) throws SQLException { // <-- Renamed method
        // FIX: Select selling_price instead of price
        String getPriceSql = "SELECT selling_price FROM products WHERE product_id = ? AND store_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(getPriceSql)) {

            pstmt.setString(1, productId);
            pstmt.setInt(2, this.storeId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Handle potential NULL selling price in DB, default to 0.0
                    double sellingPrice = rs.getDouble("selling_price");
                    if (rs.wasNull()) {
                        return 0.0; // Or throw an exception if selling price MUST be set
                    }
                    return sellingPrice;
                } else {
                    // FIX: Updated error message
                    throw new SQLException("Product ID '" + productId + "' not found in store " + this.storeId + ".");
                }
            }
        }
    }

    /**
     * Removes a specified quantity of stock for a product.
     * @param productId The ID of the product.
     * @param quantityToRemove The amount to remove.
     * @throws SQLException if a database error occurs.
     */
    public void removeStock(int productId, int quantityToRemove) throws SQLException {
        String updateSql = "UPDATE products SET quantity_in_stock = quantity_in_stock - ? WHERE product_id = ? AND store_id = ?";
        String logSql = "INSERT INTO stock_log (product_id, quantity_changed, log_type, notes, store_id) VALUES (?, ?, 'REMOVAL', 'Manual stock removal', ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Start transaction

            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setInt(1, quantityToRemove);
                updateStmt.setInt(2, productId);
                updateStmt.setInt(3, this.storeId); // <-- FIX: Added storeId
                updateStmt.executeUpdate();
            }

            try (PreparedStatement logStmt = conn.prepareStatement(logSql)) {
                logStmt.setInt(1, productId);
                logStmt.setInt(2, -quantityToRemove); // Log as a negative change
                logStmt.setInt(3, this.storeId);      // <-- FIX: Added storeId
                logStmt.executeUpdate();
            }

            conn.commit(); // Commit transaction
        } catch (SQLException e) {
            // Rollback is automatic
            throw e; // Re-throw
        }
    }

    /**
     * Deletes a product and all its related data (sales, logs) for this store.
     * @param productId The ID of the product to delete.
     * @return True if the product was successfully deleted.
     * @throws SQLException if a database error occurs.
     */
    public boolean deleteProduct(int productId) throws SQLException {
        String deleteSaleItemsSql = "DELETE FROM sale_items WHERE product_id = ? AND sale_id IN (SELECT sale_id FROM sales WHERE store_id = ?)";
        String deleteStockLogSql = "DELETE FROM stock_log WHERE product_id = ? AND store_id = ?";
        String deleteProductSql = "DELETE FROM products WHERE product_id = ? AND store_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false); // Start transaction

            try (PreparedStatement pstmt = conn.prepareStatement(deleteSaleItemsSql)) {
                pstmt.setInt(1, productId);
                pstmt.setInt(2, this.storeId);
                pstmt.executeUpdate();
            }

            try (PreparedStatement pstmt = conn.prepareStatement(deleteStockLogSql)) {
                pstmt.setInt(1, productId);
                pstmt.setInt(2, this.storeId);
                pstmt.executeUpdate();
            }

            int rowsAffected;
            try (PreparedStatement pstmt = conn.prepareStatement(deleteProductSql)) {
                pstmt.setInt(1, productId);
                pstmt.setInt(2, this.storeId);
                rowsAffected = pstmt.executeUpdate();
            }

            conn.commit(); // Commit all changes
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            // Rollback is automatic
            throw e; // Re-throw
        }
    }

    /**
     * Fetches all categories from the database.
     * @return A list of Category objects.
     * @throws SQLException if a database error occurs.
     */
    public List<Category> getCategories() throws SQLException {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT category_id, category_name FROM categories ORDER BY category_name";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while(rs.next()) {
                categories.add(new Category(
                    rs.getInt("category_id"),
                    rs.getString("category_name")
                ));
            }
        }
        return categories;
    }

    /**
     * Adds a new category to the database.
     * @param categoryName The name of the new category.
     * @throws SQLException if a database error occurs.
     */
    public void addNewCategory(String categoryName) throws SQLException {
        String sql = "INSERT INTO categories (category_name) VALUES (?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, categoryName);
            pstmt.executeUpdate();
        }
    }
}