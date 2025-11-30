package com.inventorysystem.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// Gets statistics for the dashboard (total products, sales, profit, etc.)
public class DashboardRepository {

    // Count how many different products user has
    public int getTotalProducts(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM products WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    // Count total stock (sum of all quantities)
    public int getTotalStock(int userId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(quantity_in_stock), 0) FROM products WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    // Count products with 0 or negative stock
    public int getOutOfStockCount(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM products WHERE quantity_in_stock <= 0 AND user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    // Calculate total value of inventory (cost price × quantity)
    public double getTotalInventoryCost(int userId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(cost_price * quantity_in_stock), 0) FROM products WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        return 0.0;
    }

    // Get total money earned from all sales
    public double getTotalIncome(int userId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(total_amount), 0) FROM sales WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        return 0.0;
    }

    // Get income for specific date range
    public double getTotalIncome(int userId, java.sql.Date startDate, java.sql.Date endDate) throws SQLException {
        String sql = "SELECT COALESCE(SUM(total_amount), 0) FROM sales " +
                     "WHERE user_id = ? AND DATE(sale_date) BETWEEN DATE(?) AND DATE(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setDate(2, startDate);
            pstmt.setDate(3, endDate);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        return 0.0;
    }

    // Calculate profit: (Selling Price - Saved Historical Cost) × Quantity Sold
    public double getTotalProfit(int userId) throws SQLException {
        // UPDATED SQL: Uses si.cost_price instead of p.cost_price
        String sql = "SELECT COALESCE(SUM(si.quantity_sold * (si.unit_price - si.cost_price)), 0) AS total_profit " +
                     "FROM sales s " +
                     "JOIN sale_items si ON s.sale_id = si.sale_id " +
                     "WHERE s.user_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total_profit");
                }
            }
        }
        return 0.0;
    }

    // Calculate profit for specific date range
    public double getTotalProfit(int userId, java.sql.Date startDate, java.sql.Date endDate) throws SQLException {
        // UPDATED SQL: Uses si.cost_price
        String sql = "SELECT COALESCE(SUM(si.quantity_sold * (si.unit_price - si.cost_price)), 0) AS total_profit " +
                     "FROM sales s " +
                     "JOIN sale_items si ON s.sale_id = si.sale_id " +
                     "WHERE s.user_id = ? AND DATE(s.sale_date) BETWEEN DATE(?) AND DATE(?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setDate(2, startDate);
            pstmt.setDate(3, endDate);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total_profit");
                }
            }
        }
        return 0.0;
    }
    
    // Get best selling products with sales data
    public java.util.List<Object[]> getBestSellers(int userId, java.sql.Date startDate, java.sql.Date endDate, int limit) throws SQLException {
        // UPDATED SQL: Uses si.cost_price for COGS and Margin calculations
        String sql = "SELECT p.name, " +
                     "COALESCE(SUM(si.subtotal), 0) AS sales_amount, " +
                     "COALESCE(SUM(si.quantity_sold * si.cost_price), 0) AS cogs, " +
                     "COALESCE(SUM(si.quantity_sold * (si.unit_price - si.cost_price)), 0) AS margin, " +
                     "COALESCE(SUM(si.quantity_sold), 0) AS qty_sold " +
                     "FROM sales s " +
                     "JOIN sale_items si ON s.sale_id = si.sale_id " +
                     "JOIN products p ON si.product_id = p.product_id " +
                     "WHERE s.user_id = ? " +
                     "AND DATE(s.sale_date) BETWEEN DATE(?) AND DATE(?) " +
                     "GROUP BY p.product_id, p.name " +
                     "ORDER BY sales_amount DESC " +
                     "LIMIT ?";
        
        java.util.List<Object[]> results = new java.util.ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setDate(2, startDate);
            pstmt.setDate(3, endDate);
            pstmt.setInt(4, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Object[] row = {
                        rs.getString("name"),
                        "₱" + String.format("%,.2f", rs.getDouble("sales_amount")),
                        "₱" + String.format("%,.2f", rs.getDouble("cogs")),
                        "₱" + String.format("%,.2f", rs.getDouble("margin")),
                        String.valueOf(rs.getInt("qty_sold"))
                    };
                    results.add(row);
                }
            }
        }
        return results;
    }
    
    // Get stock alerts - shows all products with status
    public java.util.List<Object[]> getStockAlerts(int userId) throws SQLException {
        String sql = "SELECT name, quantity_in_stock, " +
                     "CASE " +
                     "  WHEN quantity_in_stock = 0 THEN 'Out of Stock' " +
                     "  WHEN quantity_in_stock < 20 THEN 'Low Stock' " +
                     "  WHEN quantity_in_stock < 40 THEN 'Normal' " +
                     "  ELSE 'High Stock' " +
                     "END AS status " +
                     "FROM products " +
                     "WHERE user_id = ? " +
                     "ORDER BY quantity_in_stock ASC";
        
        java.util.List<Object[]> results = new java.util.ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int qty = rs.getInt("quantity_in_stock");
                    String status = rs.getString("status");
                    
                    Object[] row = {
                        rs.getString("name"),
                        String.valueOf(qty),
                        status
                    };
                    results.add(row);
                }
            }
        }
        return results;
    }
}