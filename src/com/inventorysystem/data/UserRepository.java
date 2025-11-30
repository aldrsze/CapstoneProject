package com.inventorysystem.data;

import com.inventorysystem.model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {

    public User login(String username, String password) throws SQLException {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            return null;
        }

        String sql = "SELECT user_id, username, user_role FROM users WHERE BINARY username = ? AND BINARY password = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("user_role")
                    );
                }
            }
        }
        return null;
    }

    public boolean signup(String username, String password, String role) throws SQLException {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Username and password cannot be empty.");
        }

        String checkSql = "SELECT COUNT(*) FROM users WHERE BINARY username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            
            checkStmt.setString(1, username);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new SQLException("Username already exists", "23000", 1062);
                }
            }
        }

        String sql = "INSERT INTO users (username, password, user_role) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, role);
            
            return pstmt.executeUpdate() > 0;
        }
    }

    public double getDefaultMarkup(int userId) throws SQLException {
        String sql = "SELECT default_markup_percent FROM users WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("default_markup_percent");
                }
            }
        }
        return 0.0;
    }

    public void updateDefaultMarkup(int userId, double newMarkupPercent) throws SQLException {
        String sql = "UPDATE users SET default_markup_percent = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, newMarkupPercent);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        }
    }

    public int getAdminIdForEmployee(int employeeId) throws SQLException {
        String sql = "SELECT admin_id FROM users WHERE user_id = ? AND user_role = 'Employee'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, employeeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("admin_id");
                }
            }
        }
        return -1;
    }

    public boolean addEmployeeUnderAdmin(int adminId, String username, String password) throws SQLException {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Username and password cannot be empty.");
        }

        String checkSql = "SELECT COUNT(*) FROM users WHERE BINARY username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            
            checkStmt.setString(1, username);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new SQLException("Username already exists", "23000", 1062);
                }
            }
        }

        String sql = "INSERT INTO users (username, password, user_role, admin_id) VALUES (?, ?, 'Employee', ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setInt(3, adminId);
            
            return pstmt.executeUpdate() > 0;
        }
    }

    public List<String> getEmployeesByAdmin(int adminId) throws SQLException {
        List<String> employees = new ArrayList<>();
        String sql = "SELECT username FROM users WHERE admin_id = ? AND user_role = 'Employee' ORDER BY username";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, adminId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    employees.add(rs.getString("username"));
                }
            }
        }
        return employees;
    }

    public boolean removeEmployee(String username) throws SQLException {
        String sql = "DELETE FROM users WHERE username = ? AND user_role = 'Employee'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            return pstmt.executeUpdate() > 0;
        }
    }
}