package com.inventorysystem.data;

import com.inventorysystem.model.StockRecord;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Stock movement summary with date filtering
public class StockRepository {

    // Get stock summary for date range
    public List<StockRecord> getStockSummaryWithDateRange(int userId, java.sql.Date startDate, java.sql.Date endDate) throws SQLException {
        List<StockRecord> records = new ArrayList<>();

        String sql =
            "SELECT " +
            "    p.product_id, " +
            "    p.name AS product_name, " +
            "    c.category_name, " +
            "    COALESCE(SUM(CASE WHEN sl.log_type IN ('STOCK-IN', 'CUSTOMER-RETURN') " +
            "                      AND sl.log_date BETWEEN ? AND ? THEN ABS(sl.quantity_changed) ELSE 0 END), 0) AS stock_in, " +
            "    COALESCE(SUM(CASE WHEN sl.log_type IN ('SALE', 'REMOVAL', 'REJECT', 'REFUND', 'DISPOSE') " +
            "                      AND sl.log_date BETWEEN ? AND ? THEN ABS(sl.quantity_changed) ELSE 0 END), 0) AS stock_out, " +
            "    p.quantity_in_stock AS current_stock " +
            "FROM products p " +
            "JOIN categories c ON p.category_id = c.category_id " +
            "LEFT JOIN stock_log sl ON p.product_id = sl.product_id AND p.user_id = sl.user_id " +
            "WHERE p.user_id = ? " +
            "GROUP BY p.product_id, p.name, c.category_name, p.quantity_in_stock " +
            "ORDER BY p.product_id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, startDate);
            pstmt.setDate(2, endDate);
            pstmt.setDate(3, startDate);
            pstmt.setDate(4, endDate);
            pstmt.setInt(5, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    records.add(new StockRecord(
                        rs.getInt("product_id"),
                        rs.getString("product_name"),
                        rs.getString("category_name"),
                        rs.getInt("stock_in"),
                        rs.getInt("stock_out"),
                        rs.getInt("current_stock")
                    ));
                }
            }
        }
        return records;
    }
}