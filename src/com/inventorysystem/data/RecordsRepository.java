package com.inventorysystem.data;

import com.inventorysystem.model.TransactionRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Transaction history (sales and stock movements)
public class RecordsRepository {

    // Get all transaction records for user
    public List<TransactionRecord> getTransactionHistory(int userId) throws SQLException {
        List<TransactionRecord> history = new ArrayList<>();

        // Get user's default markup
        UserRepository userRepo = new UserRepository();
        double markupPercent = userRepo.getDefaultMarkup(userId);

        String sql =
            "(SELECT s.sale_date AS transaction_date, p.name AS product_name, 'STOCK-OUT' AS transaction_type, " +
            "-si.quantity_sold AS quantity, si.unit_price AS unitPrice, p.retail_price AS retailPrice, " +
            "p.cost_price AS costPrice, s.total_amount AS total " +
            "FROM sales s " +
            "JOIN sale_items si ON s.sale_id = si.sale_id " +
            "JOIN products p ON si.product_id = p.product_id AND s.user_id = p.user_id " +
            "WHERE s.user_id = ?) " +
            
            "UNION ALL " +

            "(SELECT sl.log_date AS transaction_date, p.name AS product_name, sl.log_type AS transaction_type, " +
            "sl.quantity_changed AS quantity, p.cost_price AS unitPrice, p.retail_price AS retailPrice, " +
            "p.cost_price AS costPrice, (p.cost_price * sl.quantity_changed) AS total " +
            "FROM stock_log sl " +
            "JOIN products p ON sl.product_id = p.product_id AND sl.user_id = p.user_id " +
            "WHERE sl.user_id = ? AND sl.log_type IN ('STOCK-IN', 'REMOVAL', 'REJECT', 'REFUND', 'CUSTOMER-RETURN', 'DISPOSE', 'SALE')) " +
            
            "ORDER BY transaction_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    double retailPrice = rs.getDouble("retailPrice");
                    double costPrice = rs.getDouble("costPrice");
                    
                    // Calculate retail if not set
                    if (retailPrice <= 0.0 && costPrice > 0.0) {
                        retailPrice = costPrice * (1 + markupPercent / 100.0);
                        retailPrice = Math.round(retailPrice * 100.0) / 100.0;
                    }
                    
                    history.add(new TransactionRecord(
                        rs.getTimestamp("transaction_date"),
                        rs.getString("product_name"),
                        rs.getString("transaction_type"),
                        rs.getInt("quantity"), 
                        rs.getDouble("unitPrice"),
                        retailPrice,
                        rs.getDouble("total")
                    ));
                }
            }
        }
        return history;
    }

    /**
     * Fetches transaction history with date range filtering.
     */
    public List<TransactionRecord> getTransactionHistoryWithDateRange(int userId, java.sql.Date startDate, java.sql.Date endDate) throws SQLException {
        List<TransactionRecord> history = new ArrayList<>();

        UserRepository userRepo = new UserRepository();
        double markupPercent = userRepo.getDefaultMarkup(userId);

        String sql =
            "(SELECT s.sale_date AS transaction_date, p.name AS product_name, 'STOCK-OUT' AS transaction_type, " +
            "-si.quantity_sold AS quantity, si.unit_price AS unitPrice, p.retail_price AS retailPrice, " +
            "p.cost_price AS costPrice, s.total_amount AS total " +
            "FROM sales s " +
            "JOIN sale_items si ON s.sale_id = si.sale_id " +
            "JOIN products p ON si.product_id = p.product_id AND s.user_id = p.user_id " +
            "WHERE s.user_id = ? AND s.sale_date BETWEEN ? AND ?) " +
            
            "UNION ALL " +

            "(SELECT sl.log_date AS transaction_date, p.name AS product_name, sl.log_type AS transaction_type, " +
            "sl.quantity_changed AS quantity, p.cost_price AS unitPrice, p.retail_price AS retailPrice, " +
            "p.cost_price AS costPrice, (p.cost_price * sl.quantity_changed) AS total " +
            "FROM stock_log sl " +
            "JOIN products p ON sl.product_id = p.product_id AND sl.user_id = p.user_id " +
            "WHERE sl.user_id = ? AND sl.log_date BETWEEN ? AND ? " +
            "AND sl.log_type IN ('STOCK-IN', 'REMOVAL', 'REJECT', 'REFUND', 'CUSTOMER-RETURN', 'DISPOSE', 'SALE', 'Manual product added', 'Manual stock update')) " +
            
            "ORDER BY transaction_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setDate(2, startDate);
            pstmt.setDate(3, endDate);
            pstmt.setInt(4, userId);
            pstmt.setDate(5, startDate);
            pstmt.setDate(6, endDate);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    double retailPrice = rs.getDouble("retailPrice");
                    double costPrice = rs.getDouble("costPrice");
                    
                    // Calculate retail price using markup if not set
                    if (retailPrice <= 0.0 && costPrice > 0.0) {
                        retailPrice = costPrice * (1 + markupPercent / 100.0);
                        retailPrice = Math.round(retailPrice * 100.0) / 100.0;
                    }
                    
                    history.add(new TransactionRecord(
                        rs.getTimestamp("transaction_date"),
                        rs.getString("product_name"),
                        rs.getString("transaction_type"),
                        rs.getInt("quantity"), 
                        rs.getDouble("unitPrice"),
                        retailPrice,
                        rs.getDouble("total")
                    ));
                }
            }
        }
        return history;
    }
}