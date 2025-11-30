package com.inventorysystem.data;

import com.inventorysystem.model.Category;
import com.inventorysystem.model.Product;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Product CRUD operations, sales, stock, and QR code handling
public class ProductRepository {

    private final int userId;

    public ProductRepository(int userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive.");
        }
        this.userId = userId;

        try {
            ensureQuantityDamagedColumn();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Ensure quantity_damaged column exists
    private void ensureQuantityDamagedColumn() throws SQLException {
        String checkColumnSql = "SHOW COLUMNS FROM products LIKE 'quantity_damaged'";
        String addColumnSql = "ALTER TABLE products ADD COLUMN quantity_damaged INT(11) NOT NULL DEFAULT 0 AFTER quantity_in_stock";
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean columnExists = false;
            try (PreparedStatement checkStmt = conn.prepareStatement(checkColumnSql);
                 ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    columnExists = true;
                }
            }
            
            if (!columnExists) {
                try (PreparedStatement addColStmt = conn.prepareStatement(addColumnSql)) {
                    addColStmt.executeUpdate();
                }
            }
        }
    }

    // Get all products for user
    public List<Product> getAllProducts() throws SQLException {
        List<Product> products = new ArrayList<>();
        
        String sql = "SELECT p.product_id, p.name, c.category_name, p.cost_price, p.retail_price, p.quantity_in_stock, " +
                     "COALESCE(p.quantity_damaged, 0) as quantity_damaged " +
                     "FROM products p " +
                     "JOIN categories c ON p.category_id = c.category_id " +
                     "WHERE p.user_id = ? " +
                     "ORDER BY p.product_id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, this.userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    double costPrice = rs.getDouble("cost_price");
                    int stock = rs.getInt("quantity_in_stock");
                    double retailPrice = rs.getObject("retail_price") == null ? 0.0 : rs.getDouble("retail_price");
                    double totalCost = costPrice * stock;

                    products.add(new Product(
                        rs.getInt("product_id"),
                        rs.getString("name"),
                        rs.getString("category_name"),
                        costPrice,
                        retailPrice,
                        stock,
                        totalCost
                    ));
                }
            }
        }
        return products;
    }

    // Get retail price for product
    public double getProductRetailPrice(String productId) throws SQLException, NumberFormatException {
        int prodId = Integer.parseInt(productId);

        String sql = "SELECT retail_price FROM products WHERE product_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, prodId);
            pstmt.setInt(2, this.userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    double retailPrice = rs.getDouble("retail_price");
                    return rs.wasNull() ? 0.0 : retailPrice;
                } else {
                    return 0.0;
                }
            }
        }
    }

    /**
     * Gets the next available product ID for the current user.
     */
    public int getNextProductId() throws SQLException {
        String sql = "SELECT MAX(product_id) FROM products WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, this.userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int maxId = rs.getInt(1);
                    return maxId + 1;
                } else {
                    return 1;
                }
            }
        }
    }

    /**
     * Finds a product by name and category for the current user.
     * Returns product_id if found, -1 if not found.
     * Used to check if a QR code product already exists.
     */
    private int findProductByNameAndCategory(String name, int categoryId) throws SQLException {
        String sql = "SELECT product_id FROM products WHERE name = ? AND category_id = ? AND user_id = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            pstmt.setInt(2, categoryId);
            pstmt.setInt(3, this.userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("product_id");
                }
            }
        }
        return -1; // Not found
    }

    /**
     * Updates ONLY the markup percentage for a specific product.
     * Pass null to remove product-specific markup (use default).
     */
    public void updateProductMarkup(int productId, Double markupPercent) throws SQLException {
        String sql = "UPDATE products SET markup_percent = ? WHERE product_id = ? AND user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            if (markupPercent != null) {
                pstmt.setDouble(1, markupPercent);
            } else {
                pstmt.setNull(1, java.sql.Types.DECIMAL);
            }
            pstmt.setInt(2, productId);
            pstmt.setInt(3, this.userId);
            
            pstmt.executeUpdate();
        }
    }

    /**
     * Clears the retail price for a product so markup calculation is used instead.
     * Sets retail_price to 0 instead of NULL to avoid database constraints.
     */
    public void clearRetailPrice(int productId) throws SQLException {
        String sql = "UPDATE products SET retail_price = 0 WHERE product_id = ? AND user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, productId);
            pstmt.setInt(2, this.userId);
            
            pstmt.executeUpdate();
        }
    }

    /**
     * Gets the product-specific markup percentage.
     * Returns null if no specific markup is set.
     */
    public Double getProductMarkup(int productId) throws SQLException {
        String sql = "SELECT markup_percent FROM products WHERE product_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, productId);
            pstmt.setInt(2, this.userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Object markup = rs.getObject("markup_percent");
                    return markup != null ? rs.getDouble("markup_percent") : null;
                }
            }
        }
        return null;
    }

    /**
     * Inserts or updates a product from QR code JSON data.
     * QR codes don't contain markup info, only basic product data.
     * If scanning same QR code (same name + category), adds to existing product.
     */
    public int upsertProductFromQR(JSONObject data) throws SQLException, JSONException {
        if (data == null) {
            throw new IllegalArgumentException("JSON data cannot be null.");
        }

        String name = data.getString("name");
        int categoryId = data.getInt("category_id");
        double costPrice = data.getDouble("cost_price");
        int stockToAdd = data.getInt("stock");
        
        // Determine product ID: check for existing product with same name + category
        int productId;
        if (data.has("id")) {
            productId = data.getInt("id"); // Old QR codes with explicit ID
        } else {
            // New QR codes without ID - check if product already exists
            productId = findProductByNameAndCategory(name, categoryId);
            if (productId == -1) {
                // Product doesn't exist - auto-generate new ID
                productId = getNextProductId();
            }
            // If productId > 0, it means product exists - will add to existing
        }
        
        String upsertSql = "INSERT INTO products (product_id, name, cost_price, quantity_in_stock, category_id, user_id) " +
                           "VALUES (?, ?, ?, ?, ?, ?) " +
                           "ON DUPLICATE KEY UPDATE " +
                           "name = VALUES(name), cost_price = VALUES(cost_price), " +
                           "quantity_in_stock = quantity_in_stock + VALUES(quantity_in_stock), category_id = VALUES(category_id)";
        String logSql = "INSERT INTO stock_log (product_id, quantity_changed, log_type, notes, user_id) VALUES (?, ?, 'STOCK-IN', 'From QR Scan', ?)";

        if (costPrice < 0 || stockToAdd < 0) {
             throw new IllegalArgumentException("Cost price and stock quantity from QR cannot be negative.");
        }

        Connection conn = null;
        int affectedRows = 0;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement upsertPstmt = conn.prepareStatement(upsertSql)) {
                upsertPstmt.setInt(1, productId);
                upsertPstmt.setString(2, name);
                upsertPstmt.setDouble(3, costPrice);
                upsertPstmt.setInt(4, stockToAdd);
                upsertPstmt.setInt(5, categoryId);
                upsertPstmt.setInt(6, this.userId);
                affectedRows = upsertPstmt.executeUpdate();
            }

            if (affectedRows > 0) {
                try (PreparedStatement logPstmt = conn.prepareStatement(logSql)) {
                    logPstmt.setInt(1, productId);
                    logPstmt.setInt(2, +stockToAdd);
                    logPstmt.setInt(3, this.userId);
                    logPstmt.executeUpdate();
                }
            } else {
                throw new SQLException("Upsert operation affected 0 rows for product ID: " + productId);
            }

            conn.commit();
            return productId; // Return the actual product ID used (auto-generated or from QR) 

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { 
                    System.err.println("Rollback failed: " + ex.getMessage()); 
                }
            }
            throw new SQLException("Error during QR database transaction: " + e.getMessage(), e);
        } finally {
             if (conn != null) {
                 try { conn.setAutoCommit(true); conn.close(); } 
                 catch (SQLException e) { /* Ignored */ }
             }
        }
    }

    /**
     * Manually inserts or updates a product.
     * Returns the product ID (auto-generated for new products, or the provided ID for updates).
     */
    public int manualUpsertProduct(int id, String name, int categoryId, double costPrice, 
                                      Double retailPrice, Double markupPercent, int newStock) throws SQLException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be null or empty.");
        }
        if (newStock < 0 || costPrice < 0) {
             throw new IllegalArgumentException("Cost price and stock quantity cannot be negative.");
        }

        boolean isNewProduct = (id == 0);
        
        String selectSql = "SELECT quantity_in_stock, retail_price FROM products WHERE product_id = ? AND user_id = ? FOR UPDATE";
        String getNextIdSql = "SELECT COALESCE(MAX(product_id), 0) + 1 as next_id FROM products WHERE user_id = ?";
        String insertSql = "INSERT INTO products (product_id, name, cost_price, retail_price, markup_percent, quantity_in_stock, category_id, user_id) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String updateSql = "UPDATE products SET name = ?, cost_price = ?, retail_price = ?, markup_percent = ?, " +
                           "quantity_in_stock = ?, category_id = ? WHERE product_id = ? AND user_id = ?";
        String logSql = "INSERT INTO stock_log (product_id, quantity_changed, log_type, notes, user_id) VALUES (?, ?, ?, ?, ?)";

        Connection conn = null;
        int resultProductId = 0;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            int oldStock = 0;
            double existingRetailPrice = 0.0;
            boolean exists = false;
            
            if (!isNewProduct) {
                try (PreparedStatement selectPstmt = conn.prepareStatement(selectSql)) {
                    selectPstmt.setInt(1, id);
                    selectPstmt.setInt(2, this.userId);
                    try (ResultSet rs = selectPstmt.executeQuery()) {
                        if (rs.next()) {
                            oldStock = rs.getInt("quantity_in_stock");
                            existingRetailPrice = rs.getDouble("retail_price");
                            exists = true;
                        }
                    }
                }
            }

            // If retailPrice is null, preserve existing value or use 0
            double finalRetailPrice = 0.0;
            if (retailPrice != null) {
                finalRetailPrice = retailPrice;
            } else if (exists) {
                finalRetailPrice = existingRetailPrice; // Preserve existing
            }

            int rowsAffected = 0;
            
            if (isNewProduct) {
                // Get next available product_id for this user
                int nextProductId = 1;
                try (PreparedStatement getIdPstmt = conn.prepareStatement(getNextIdSql)) {
                    getIdPstmt.setInt(1, this.userId);
                    try (ResultSet rs = getIdPstmt.executeQuery()) {
                        if (rs.next()) {
                            nextProductId = rs.getInt("next_id");
                        }
                    }
                }
                
                resultProductId = nextProductId;
                
                // Insert new product with manual ID
                try (PreparedStatement insertPstmt = conn.prepareStatement(insertSql)) {
                    insertPstmt.setInt(1, nextProductId);
                    insertPstmt.setString(2, name.trim());
                    insertPstmt.setDouble(3, costPrice);
                    insertPstmt.setDouble(4, finalRetailPrice);
                    
                    if (markupPercent != null) {
                        insertPstmt.setDouble(5, markupPercent);
                    } else {
                        insertPstmt.setNull(5, java.sql.Types.DECIMAL);
                    }
                    
                    insertPstmt.setInt(6, newStock);
                    insertPstmt.setInt(7, categoryId);
                    insertPstmt.setInt(8, this.userId);
                    rowsAffected = insertPstmt.executeUpdate();
                }
            } else {
                // Update existing product
                try (PreparedStatement updatePstmt = conn.prepareStatement(updateSql)) {
                    updatePstmt.setString(1, name.trim());
                    updatePstmt.setDouble(2, costPrice);
                    updatePstmt.setDouble(3, finalRetailPrice);
                    
                    if (markupPercent != null) {
                        updatePstmt.setDouble(4, markupPercent);
                    } else {
                        updatePstmt.setNull(4, java.sql.Types.DECIMAL);
                    }
                    
                    updatePstmt.setInt(5, newStock);
                    updatePstmt.setInt(6, categoryId);
                    updatePstmt.setInt(7, id);
                    updatePstmt.setInt(8, this.userId);
                    rowsAffected = updatePstmt.executeUpdate();
                }
                resultProductId = id;
            }

            if (rowsAffected > 0) {
                int quantityChange = newStock - oldStock;
                String logType = null;
                String notes = null;

                if (isNewProduct) {
                    logType = "STOCK-IN";
                    notes = "Manual product added";
                    quantityChange = newStock;
                } else if (quantityChange > 0) {
                    logType = "STOCK-IN";
                    notes = "Manual stock increase";
                } else if (quantityChange < 0) {
                    logType = "REMOVAL";
                    notes = "Manual stock decrease";
                }

                if (logType != null) {
                    try (PreparedStatement logPstmt = conn.prepareStatement(logSql)) {
                        logPstmt.setInt(1, resultProductId);
                        logPstmt.setInt(2, quantityChange);
                        logPstmt.setString(3, logType);
                        logPstmt.setString(4, notes);
                        logPstmt.setInt(5, this.userId);
                        logPstmt.executeUpdate();
                    }
                }
            }

            conn.commit();

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { 
                    System.err.println("Rollback failed: " + ex.getMessage()); 
                }
            }
            throw e;
        } finally {
             if (conn != null) {
                 try { conn.setAutoCommit(true); conn.close(); } 
                 catch (SQLException e) { /* Ignored */ }
             }
        }
        return resultProductId;
    }


    // ---HELPER METHOD ---
    private void ensureSaleItemsCostColumn() throws SQLException {
        String checkSql = "SHOW COLUMNS FROM sale_items LIKE 'cost_price'";
        // Add column with default 0
        String addSql = "ALTER TABLE sale_items ADD COLUMN cost_price DECIMAL(10, 2) NOT NULL DEFAULT 0.00 AFTER unit_price";
        // Important: Backfill old records with current product cost so history isn't zero
        String backfillSql = "UPDATE sale_items si JOIN products p ON si.product_id = p.product_id SET si.cost_price = p.cost_price WHERE si.cost_price = 0";

        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean exists = false;
            try (PreparedStatement stmt = conn.prepareStatement(checkSql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) exists = true;
            }
            
            if (!exists) {
                try (PreparedStatement stmt = conn.prepareStatement(addSql)) {
                    stmt.executeUpdate();
                }
                // Run backfill immediately after creating
                try (PreparedStatement stmt = conn.prepareStatement(backfillSql)) {
                    stmt.executeUpdate();
                }
            }
        }
    }

     /**
     * Processes a product sale, determining price from retail_price or markup.
     */
    public void sellProduct(String productId, int quantityToSell) throws SQLException, NumberFormatException {
        if (quantityToSell <= 0) throw new IllegalArgumentException("Positive quantity required.");
        
        // 1. Ensure the table has the new column before we try to write to it
        ensureSaleItemsCostColumn();

        int prodId = Integer.parseInt(productId);

        String findSql = "SELECT quantity_in_stock, cost_price, retail_price, markup_percent FROM products WHERE product_id = ? AND user_id = ? FOR UPDATE";
        String updateSql = "UPDATE products SET quantity_in_stock = ? WHERE product_id = ? AND user_id = ?";
        String salesSql = "INSERT INTO sales (sale_date, total_amount, user_id) VALUES (NOW(), ?, ?)";
        
        // UPDATED SQL: Now inserts cost_price
        String itemsSql = "INSERT INTO sale_items (sale_id, product_id, quantity_sold, unit_price, cost_price, subtotal) VALUES (?, ?, ?, ?, ?, ?)";
        
        String logSql = "INSERT INTO stock_log (product_id, quantity_changed, log_type, notes, user_id) VALUES (?, ?, 'SALE', ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            int currentStock;
            double cost, retail;
            Double markup;

            try (PreparedStatement stmt = conn.prepareStatement(findSql)) {
                stmt.setInt(1, prodId);
                stmt.setInt(2, this.userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Product not found.");
                    currentStock = rs.getInt("quantity_in_stock");
                    cost = rs.getDouble("cost_price");
                    retail = rs.getDouble("retail_price");
                    markup = rs.getObject("markup_percent") != null ? rs.getDouble("markup_percent") : null;
                }
            }

            if (currentStock < quantityToSell) throw new SQLException("Insufficient stock.");

            double price = retail;
            if (price <= 0.0) {
                double m = (markup != null) ? markup : new UserRepository().getDefaultMarkup(this.userId);
                price = cost * (1 + m / 100.0);
            }

            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setInt(1, currentStock - quantityToSell);
                stmt.setInt(2, prodId);
                stmt.setInt(3, this.userId);
                stmt.executeUpdate();
            }

            long saleId;
            double total = price * quantityToSell;
            try (PreparedStatement stmt = conn.prepareStatement(salesSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setDouble(1, total);
                stmt.setInt(2, this.userId);
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) saleId = rs.getLong(1);
                    else throw new SQLException("No sale ID.");
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement(itemsSql)) {
                stmt.setLong(1, saleId);
                stmt.setInt(2, prodId);
                stmt.setInt(3, quantityToSell);
                stmt.setDouble(4, price);
                stmt.setDouble(5, cost); // Save the HISTORICAL COST here
                stmt.setDouble(6, total);
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(logSql)) {
                stmt.setInt(1, prodId);
                stmt.setInt(2, -quantityToSell);
                stmt.setString(3, "Sale ID: " + saleId);
                stmt.setInt(4, this.userId);
                stmt.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            throw e;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) {}
        }
    }

    /**
     * Removes a specified quantity from product stock.
     */
    public void removeStock(int productId, int quantityToRemove, String reason) throws SQLException {
        if (quantityToRemove <= 0) {
             throw new IllegalArgumentException("Quantity to remove must be positive.");
        }
        
        String updateSql = "UPDATE products SET quantity_in_stock = quantity_in_stock - ? WHERE product_id = ? AND user_id = ?";
        String logSql = "INSERT INTO stock_log (product_id, quantity_changed, log_type, notes, user_id) VALUES (?, ?, 'REMOVAL', ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // 1. Update product stock
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setInt(1, quantityToRemove);
                updateStmt.setInt(2, productId);
                updateStmt.setInt(3, this.userId);
                updateStmt.executeUpdate();
            }

            // 2. Log the stock removal
            try (PreparedStatement logStmt = conn.prepareStatement(logSql)) {
                logStmt.setInt(1, productId);
                logStmt.setInt(2, -quantityToRemove);
                logStmt.setString(3, reason != null && !reason.isEmpty() ? reason : "Manual stock removal");
                logStmt.setInt(4, this.userId); // Set user ID
                logStmt.executeUpdate();
            }

            conn.commit();

        } catch (SQLException e) {
            if (conn != null) {
                try (Connection c = conn) { c.rollback(); } catch (SQLException ex) { System.err.println("Rollback failed: " + ex.getMessage()); }
            }
            throw e;
        } finally {
             if (conn != null) {
                 try { conn.setAutoCommit(true); } catch (SQLException e) { /* Ignored */ }
                 try { conn.close(); } catch (SQLException e) { /* Ignored */ }
             }
        }
    }

    /**
     * Processes a product rejection (damaged/defective), moving items from sellable to damaged inventory.
     * This is for products with problems that cannot be sold.
     * Damaged items are deducted from sellable stock and moved to quantity_damaged.
     */
    public void rejectProduct(int productId, int quantityToReject, String reason) throws SQLException {
        if (quantityToReject <= 0) {
            throw new IllegalArgumentException("Quantity to reject must be positive.");
        }
        
        String findProductSql = "SELECT quantity_in_stock FROM products WHERE product_id = ? AND user_id = ? FOR UPDATE";
        String updateSql = "UPDATE products SET quantity_in_stock = quantity_in_stock - ?, quantity_damaged = quantity_damaged + ? WHERE product_id = ? AND user_id = ?";
        String logSql = "INSERT INTO stock_log (product_id, quantity_changed, log_type, notes, user_id) VALUES (?, ?, 'REJECT', ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // Check current stock
            int currentStock;
            try (PreparedStatement findPstmt = conn.prepareStatement(findProductSql)) {
                findPstmt.setInt(1, productId);
                findPstmt.setInt(2, this.userId);
                try (ResultSet rs = findPstmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Product ID " + productId + " not found for this user.");
                    }
                    currentStock = rs.getInt("quantity_in_stock");
                }
            }

            // Validate stock availability
            if (currentStock < quantityToReject) {
                throw new SQLException("Not enough stock to reject. Available: " + currentStock + ", Requested: " + quantityToReject);
            }

            // Update: reduce sellable stock and increase damaged quantity
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setInt(1, quantityToReject);
                updateStmt.setInt(2, quantityToReject);
                updateStmt.setInt(3, productId);
                updateStmt.setInt(4, this.userId);
                updateStmt.executeUpdate();
            }

            // Log the rejection in stock_log (negative to show removal from sellable stock)
            try (PreparedStatement logStmt = conn.prepareStatement(logSql)) {
                logStmt.setInt(1, productId);
                logStmt.setInt(2, -quantityToReject);
                logStmt.setString(3, reason + " [Moved to damaged inventory - NOT FOR SALE]");
                logStmt.setInt(4, this.userId);
                logStmt.executeUpdate();
            }

            conn.commit();

        } catch (SQLException e) {
            if (conn != null) {
                try (Connection c = conn) { c.rollback(); } catch (SQLException ex) { System.err.println("Rollback failed: " + ex.getMessage()); }
            }
            throw e;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException e) { /* Ignored */ }
                try { conn.close(); } catch (SQLException e) { /* Ignored */ }
            }
        }
    }

    /**
     * Processes a customer return, adding stock back to sellable inventory.
     * This is for items returned by customers that are in good condition and can be resold.
     */
    public void customerReturn(int productId, int quantityToReturn, String reason) throws SQLException {
        if (quantityToReturn <= 0) {
            throw new IllegalArgumentException("Quantity to return must be positive.");
        }
        
        String updateSql = "UPDATE products SET quantity_in_stock = quantity_in_stock + ? WHERE product_id = ? AND user_id = ?";
        String logSql = "INSERT INTO stock_log (product_id, quantity_changed, log_type, notes, user_id) VALUES (?, ?, 'CUSTOMER-RETURN', ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // Update product stock (add returned quantity back to sellable inventory)
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setInt(1, quantityToReturn);
                updateStmt.setInt(2, productId);
                updateStmt.setInt(3, this.userId);
                int rowsAffected = updateStmt.executeUpdate();
                
                if (rowsAffected == 0) {
                    throw new SQLException("Product ID " + productId + " not found for this user.");
                }
            }

            // Log the customer return in stock_log
            try (PreparedStatement logStmt = conn.prepareStatement(logSql)) {
                logStmt.setInt(1, productId);
                logStmt.setInt(2, +quantityToReturn);
                logStmt.setString(3, reason + " [Added back to sellable stock]");
                logStmt.setInt(4, this.userId);
                logStmt.executeUpdate();
            }

            conn.commit();

        } catch (SQLException e) {
            if (conn != null) {
                try (Connection c = conn) { c.rollback(); } catch (SQLException ex) { System.err.println("Rollback failed: " + ex.getMessage()); }
            }
            throw e;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException e) { /* Ignored */ }
                try { conn.close(); } catch (SQLException e) { /* Ignored */ }
            }
        }
    }

    /**
     * Processes a product refund to supplier, removing stock and logging the transaction.
     * This is for returning products to the supplier for a refund.
     */
    public void refundProduct(int productId, int quantityToRefund, String reason) throws SQLException {
        if (quantityToRefund <= 0) {
            throw new IllegalArgumentException("Quantity to refund must be positive.");
        }
        
        String findProductSql = "SELECT quantity_in_stock, cost_price FROM products WHERE product_id = ? AND user_id = ? FOR UPDATE";
        String updateSql = "UPDATE products SET quantity_in_stock = quantity_in_stock - ? WHERE product_id = ? AND user_id = ?";
        String logSql = "INSERT INTO stock_log (product_id, quantity_changed, log_type, notes, user_id) VALUES (?, ?, 'REFUND', ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // 1. Check current stock and get cost price
            int currentStock;
            double costPrice;
            try (PreparedStatement findPstmt = conn.prepareStatement(findProductSql)) {
                findPstmt.setInt(1, productId);
                findPstmt.setInt(2, this.userId);
                try (ResultSet rs = findPstmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Product ID " + productId + " not found for this user.");
                    }
                    currentStock = rs.getInt("quantity_in_stock");
                    costPrice = rs.getDouble("cost_price");
                }
            }

            // 2. Validate stock availability
            if (currentStock < quantityToRefund) {
                throw new SQLException("Not enough stock to refund. Available: " + currentStock + ", Requested: " + quantityToRefund);
            }

            // 3. Update product stock (remove refunded quantity)
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setInt(1, quantityToRefund);
                updateStmt.setInt(2, productId);
                updateStmt.setInt(3, this.userId);
                updateStmt.executeUpdate();
            }

            // 4. Log the refund in stock_log (negative quantity to indicate stock out)
            String refundNote = reason + " [Refund Amount: ₱" + String.format("%,.2f", costPrice * quantityToRefund) + "]";
            try (PreparedStatement logStmt = conn.prepareStatement(logSql)) {
                logStmt.setInt(1, productId);
                logStmt.setInt(2, -quantityToRefund); // Negative to indicate stock reduction
                logStmt.setString(3, refundNote);
                logStmt.setInt(4, this.userId);
                logStmt.executeUpdate();
            }

            conn.commit();

        } catch (SQLException e) {
            if (conn != null) {
                try (Connection c = conn) { c.rollback(); } catch (SQLException ex) { System.err.println("Rollback failed: " + ex.getMessage()); }
            }
            throw e;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException e) { /* Ignored */ }
                try { conn.close(); } catch (SQLException e) { /* Ignored */ }
            }
        }
    }

    /**
     * Deletes a product and all related data (sale items, stock logs).
     */
    public boolean deleteProduct(int productId) throws SQLException {
        // 1. Delete all associated sale items
        String deleteSaleItemsSql = "DELETE FROM sale_items WHERE product_id = ? AND sale_id IN (SELECT sale_id FROM sales WHERE user_id = ?)";
        
        // 2. Update the total_amount in the sales table based on remaining items
        String updateSalesTotalSql = 
            "UPDATE sales s " +
            "SET total_amount = ( " +
            "    SELECT COALESCE(SUM(si.quantity_sold * si.unit_price), 0) " +
            "    FROM sale_items si " +
            "    WHERE si.sale_id = s.sale_id " +
            ") " +
            "WHERE s.user_id = ?";
        
        // 3. Delete sale records that now have no items (total_amount = 0)
        String deleteEmptySalesSql = "DELETE FROM sales WHERE user_id = ? AND total_amount = 0";
        
        // 4. Delete associated stock logs
        String deleteStockLogSql = "DELETE FROM stock_log WHERE product_id = ? AND user_id = ?";
        
        // 5. Delete the product itself
        String deleteProductSql = "DELETE FROM products WHERE product_id = ? AND user_id = ?";

        Connection conn = null;
        int productRowsAffected = 0;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // 1. Delete associated sale items
            try (PreparedStatement pstmt = conn.prepareStatement(deleteSaleItemsSql)) {
                pstmt.setInt(1, productId);
                pstmt.setInt(2, this.userId);
                pstmt.executeUpdate();
            }

            // 2. Update sales totals based on remaining items
            try (PreparedStatement pstmt = conn.prepareStatement(updateSalesTotalSql)) {
                pstmt.setInt(1, this.userId);
                pstmt.executeUpdate();
            }
            
            // 3. Delete sales records that are now empty (total_amount = 0)
            try (PreparedStatement pstmt = conn.prepareStatement(deleteEmptySalesSql)) {
                pstmt.setInt(1, this.userId);
                pstmt.executeUpdate();
            }

            // 4. Delete associated stock logs
            try (PreparedStatement pstmt = conn.prepareStatement(deleteStockLogSql)) {
                pstmt.setInt(1, productId);
                pstmt.setInt(2, this.userId);
                pstmt.executeUpdate();
            }

            // 5. Delete the product itself
            try (PreparedStatement pstmt = conn.prepareStatement(deleteProductSql)) {
                pstmt.setInt(1, productId);
                pstmt.setInt(2, this.userId);
                productRowsAffected = pstmt.executeUpdate();
            }

            conn.commit();
            return productRowsAffected > 0;

        } catch (SQLException e) {
            if (conn != null) {
                try (Connection c = conn) { c.rollback(); } catch (SQLException ex) { System.err.println("Rollback failed: " + ex.getMessage()); }
            }
            throw e;
        } finally {
             if (conn != null) {
                 try { conn.setAutoCommit(true); } catch (SQLException e) { /* Ignored */ }
                 try { conn.close(); } catch (SQLException e) { /* Ignored */ }
             }
        }
    }


    /**
     * Fetches all categories for the current user.
     */
    public List<Category> getCategories() throws SQLException {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT category_id, category_name FROM categories WHERE user_id = ? ORDER BY category_name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, this.userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while(rs.next()) {
                    categories.add(new Category(
                        rs.getInt("category_id"),
                        rs.getString("category_name")
                    ));
                }
            }
        }
        return categories;
    }

    /**
     * Adds a new category for the current user.
     */
    public void addNewCategory(String categoryName) throws SQLException {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be null or empty.");
        }

        String sql = "INSERT INTO categories (category_name, user_id) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, categoryName.trim());
            pstmt.setInt(2, this.userId);
            pstmt.executeUpdate();
        }
    }

    /**
     * Deletes all products, sale items, and stock logs for the current user.
     * Called during store deletion.
     */
    public void deleteAllProductsForUser(Connection conn) throws SQLException {
        List<Integer> productIds = new ArrayList<>();
        String selectSql = "SELECT product_id FROM products WHERE user_id = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
            pstmt.setInt(1, this.userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) productIds.add(rs.getInt("product_id"));
            }
        }

        if (productIds.isEmpty()) return;

        StringBuilder inClause = new StringBuilder("(");
        for (int i = 0; i < productIds.size(); i++) {
            inClause.append("?");
            if (i < productIds.size() - 1) inClause.append(",");
        }
        inClause.append(")");

        String delItems = "DELETE FROM sale_items WHERE product_id IN " + inClause;
        String delLogs = "DELETE FROM stock_log WHERE product_id IN " + inClause;
        String delProds = "DELETE FROM products WHERE product_id IN " + inClause + " AND user_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(delItems)) {
            for (int i = 0; i < productIds.size(); i++) pstmt.setInt(i + 1, productIds.get(i));
            pstmt.executeUpdate();
        }
        try (PreparedStatement pstmt = conn.prepareStatement(delLogs)) {
            for (int i = 0; i < productIds.size(); i++) pstmt.setInt(i + 1, productIds.get(i));
            pstmt.executeUpdate();
        }
        try (PreparedStatement pstmt = conn.prepareStatement(delProds)) {
            for (int i = 0; i < productIds.size(); i++) pstmt.setInt(i + 1, productIds.get(i));
            pstmt.setInt(productIds.size() + 1, this.userId);
            pstmt.executeUpdate();
        }
    }

    // Process product return (customer return, reject, refund, dispose)
    public void processReturn(int productId, int quantity, String reason, String notes) throws SQLException {
        
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            String logType;
            boolean addBackToStock = false;

            switch (reason.toUpperCase()) {
                case "CUSTOMER-RETURN":
                    logType = "CUSTOMER-RETURN";
                    addBackToStock = true;
                    break;
                case "REJECT":
                    logType = "REJECT";
                    addBackToStock = false;
                    break;
                case "REFUND":
                    logType = "REFUND";
                    addBackToStock = true;
                    break;
                case "DISPOSE":
                    logType = "DISPOSE";
                    addBackToStock = false;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid return reason: " + reason);
            }

            // Update stock if needed
            if (addBackToStock) {
                String updateStockSql = "UPDATE products SET quantity_in_stock = quantity_in_stock + ? WHERE product_id = ? AND user_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateStockSql)) {
                    pstmt.setInt(1, quantity);
                    pstmt.setInt(2, productId);
                    pstmt.setInt(3, this.userId);
                    pstmt.executeUpdate();
                }
            } else {
                // Update damaged quantity
                String updateDamagedSql = "UPDATE products SET quantity_damaged = quantity_damaged + ? WHERE product_id = ? AND user_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateDamagedSql)) {
                    pstmt.setInt(1, quantity);
                    pstmt.setInt(2, productId);
                    pstmt.setInt(3, this.userId);
                    pstmt.executeUpdate();
                }
            }

            // Log the transaction with notes
            String finalNotes = notes;
            // Append descriptive information based on return type
            if (logType.equals("CUSTOMER-RETURN")) {
                finalNotes = (notes != null && !notes.trim().isEmpty() ? notes + " - " : "") + "[Added back to sellable stock]";
            } else if (logType.equals("REJECT")) {
                finalNotes = (notes != null && !notes.trim().isEmpty() ? notes + " - " : "") + "[Moved to damaged inventory - NOT FOR SALE]";
            } else if (logType.equals("REFUND")) {
                finalNotes = (notes != null && !notes.trim().isEmpty() ? notes + " - " : "") + "[Returned to supplier]";
            } else if (logType.equals("DISPOSE")) {
                finalNotes = (notes != null && !notes.trim().isEmpty() ? notes + " - " : "") + "[Disposed]";
            }
            
            String logSql = "INSERT INTO stock_log (product_id, user_id, quantity_changed, log_type, notes, log_date) VALUES (?, ?, ?, ?, ?, NOW())";
            try (PreparedStatement pstmt = conn.prepareStatement(logSql)) {
                pstmt.setInt(1, productId);
                pstmt.setInt(2, this.userId);
                pstmt.setInt(3, quantity);
                pstmt.setString(4, logType);
                pstmt.setString(5, finalNotes != null && !finalNotes.trim().isEmpty() ? finalNotes : null);
                pstmt.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { }
            }
            throw e;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException e) { }
                try { conn.close(); } catch (SQLException e) { }
            }
        }
    }

    /**
     * Retrieves the category name for a specific ID.
     * Returns "Unknown Category" if not found for this user.
     */
    public String getCategoryNameById(int categoryId) throws SQLException {
        String sql = "SELECT category_name FROM categories WHERE category_id = ? AND user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, categoryId);
            pstmt.setInt(2, this.userId); // Crucial: Must match the current user
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("category_name");
                }
            }
        }
        return null; // Not found for this user
    }

    /**
     * Gets the category name for a specific product ID.
     * Used to ensure the dialog shows the correct category after auto-creation.
     */
    public String getProductCategoryName(int productId) throws SQLException {
        String sql = "SELECT c.category_name FROM products p " +
                     "JOIN categories c ON p.category_id = c.category_id " +
                     "WHERE p.product_id = ? AND p.user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            pstmt.setInt(2, this.userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("category_name");
                }
            }
        }
        return "Unknown";
    }
}


