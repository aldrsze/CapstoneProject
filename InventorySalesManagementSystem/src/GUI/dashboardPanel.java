package GUI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import Database.DatabaseConnection;

public class dashboardPanel extends JPanel {

    private final int storeId; // To identify the current store
    private JLabel totalProductsValue;
    private JLabel totalStockValue;
    private JLabel outOfStockValue;
    private JLabel totalCostValue;
    private JLabel totalIncomeValue;
    private JLabel totalProfitValue;

    public dashboardPanel(int storeId) {
        this.storeId = storeId;

        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(10, 20, 20, 20));

        // --- TOP PANEL FOR CONTROLS ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topPanel.setBackground(Color.WHITE);
        JButton refreshButton = new JButton("Refresh");
        refreshButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        refreshButton.addActionListener(e -> refreshData());
        topPanel.add(refreshButton);

        // --- MAIN PANEL FOR STATS ---
        JPanel statsGridPanel = new JPanel(new GridLayout(2, 3, 15, 15));
        statsGridPanel.setBackground(Color.WHITE);

        // Initialize the value labels with a default state
        totalProductsValue = createValueLabel("0");
        totalStockValue = createValueLabel("0");
        outOfStockValue = createValueLabel("0");
        totalCostValue = createValueLabel("₱0.00"); // Default for currency
        totalIncomeValue = createValueLabel("₱0.00"); // Default for currency
        totalProfitValue = createValueLabel("₱0.00"); // Default for currency

        // Create and add each stat panel to the grid
        statsGridPanel.add(createStatPanel("TOTAL PRODUCTS", totalProductsValue, "products.png"));
        statsGridPanel.add(createStatPanel("TOTAL STOCKS", totalStockValue, "stock.png"));
        statsGridPanel.add(createStatPanel("ITEMS OUT OF STOCK", outOfStockValue, "out-of-stock.png"));
        statsGridPanel.add(createStatPanel("TOTAL COST", totalCostValue, "cost.png"));
        statsGridPanel.add(createStatPanel("TOTAL INCOME", totalIncomeValue, "income.png"));
        statsGridPanel.add(createStatPanel("TOTAL PROFIT", totalProfitValue, "profit.png"));

        add(topPanel, BorderLayout.NORTH);
        add(statsGridPanel, BorderLayout.CENTER);

        refreshData(); // Load initial data
    }

    // Overloaded method to create label with initial text
    private JLabel createValueLabel(String initialText) {
        JLabel label = new JLabel(initialText, SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 48));
        return label;
    }

    private JPanel createStatPanel(String title, JLabel valueLabel, String iconName) {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220)),
            new EmptyBorder(15, 15, 15, 15)
        ));
        panel.setBackground(new Color(245, 245, 245));
        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        try {
            // Ensure resource path is correct (should be relative to classpath root)
            ImageIcon icon = new ImageIcon(getClass().getResource("/resources/" + iconName));
            if (icon.getImageLoadStatus() == MediaTracker.COMPLETE) {
                Image scaledImage = icon.getImage().getScaledInstance(48, 48, Image.SCALE_SMOOTH);
                titleLabel.setIcon(new ImageIcon(scaledImage));
                titleLabel.setVerticalTextPosition(SwingConstants.BOTTOM);
                titleLabel.setHorizontalTextPosition(SwingConstants.CENTER);
            } else {
                 System.err.println("Icon image loading failed: " + iconName);
            }
        } catch (NullPointerException e) {
             System.err.println("Icon resource not found: /resources/" + iconName);
        } catch (Exception e) {
            System.err.println("Error loading icon: " + iconName + " - " + e.getMessage());
        }
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);
        return panel;
    }

    private void refreshData() {
        // Use SwingWorker or ExecutorService if loading becomes slow,
        // but for now, direct calls are okay as they happen on EDT.
        updateTotalProducts();
        updateTotalStock();
        updateOutOfStock();
        updateTotalCost();
        updateTotalIncome();
        updateTotalProfit();
    }

    // --- UPDATED DATA FETCHING METHODS ---

    private void updateTotalProducts() {
        String sql = "SELECT COUNT(*) FROM products WHERE store_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, this.storeId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                totalProductsValue.setText(String.valueOf(rs.getInt(1)));
            } else {
                 totalProductsValue.setText("0"); // Default if no rows
            }
        } catch (SQLException e) {
            System.err.println("Error updating total products: " + e.getMessage());
            totalProductsValue.setText("Error"); // Update UI on error
        }
    }

    private void updateTotalStock() {
        // Using COALESCE to handle case where there are no products (SUM would be NULL)
        String sql = "SELECT COALESCE(SUM(quantity_in_stock), 0) FROM products WHERE store_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, this.storeId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                totalStockValue.setText(String.valueOf(rs.getInt(1)));
            } else {
                 totalStockValue.setText("0"); // Should not happen with COALESCE, but safe default
            }
        } catch (SQLException e) {
             System.err.println("Error updating total stock: " + e.getMessage());
            totalStockValue.setText("Error"); // Update UI on error
        }
    }

    private void updateOutOfStock() {
        String sql = "SELECT COUNT(*) FROM products WHERE quantity_in_stock <= 0 AND store_id = ?"; // Use <= 0 to be safer
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, this.storeId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                outOfStockValue.setText(String.valueOf(rs.getInt(1)));
            } else {
                 outOfStockValue.setText("0");
            }
        } catch (SQLException e) {
             System.err.println("Error updating out of stock count: " + e.getMessage());
            outOfStockValue.setText("Error"); // Update UI on error
        }
    }

    private void updateTotalCost() {
        // FIX: Calculate total cost of current inventory (Cost Price * Stock Quantity)
        String sql = "SELECT COALESCE(SUM(COALESCE(price, 0) * quantity_in_stock), 0) " +
                     "FROM products WHERE store_id = ?"; // <-- Updated SQL
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, this.storeId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                // Format as currency
                totalCostValue.setText("₱" + String.format("%,.2f", rs.getDouble(1)));
            } else {
                 totalCostValue.setText("₱0.00"); // Default if no products
            }
        } catch (SQLException e) {
            System.err.println("Error updating total inventory cost: " + e.getMessage()); // Updated error message
            totalCostValue.setText("Error"); // Update UI on error
        }
    }

    private void updateTotalIncome() {
        // FIX: Use COALESCE to ensure SUM returns 0 if no sales
        String sql = "SELECT COALESCE(SUM(total_amount), 0) FROM sales WHERE store_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, this.storeId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                totalIncomeValue.setText("₱" + String.format("%,.2f", rs.getDouble(1)));
            } else {
                totalIncomeValue.setText("₱0.00"); // Should not happen with COALESCE
            }
        } catch (SQLException e) {
            System.err.println("Error updating total income: " + e.getMessage());
            totalIncomeValue.setText("Error"); // Update UI on error
        }
    }

    private void updateTotalProfit() {
        // FIX: Use COALESCE for prices and the outer SUM
        String sql = "SELECT COALESCE(SUM((COALESCE(si.unit_price, 0) - COALESCE(p.price, 0)) * si.quantity_sold), 0) AS total_profit " +
                     "FROM sale_items si " +
                     "JOIN products p ON si.product_id = p.product_id " +
                     "JOIN sales s ON si.sale_id = s.sale_id WHERE s.store_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, this.storeId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                totalProfitValue.setText("₱" + String.format("%,.2f", rs.getDouble("total_profit"))); // Use alias
            } else {
                 totalProfitValue.setText("₱0.00"); // Should not happen with COALESCE
            }
        } catch (SQLException e) {
            System.err.println("Error updating total profit: " + e.getMessage());
            totalProfitValue.setText("Error"); // Update UI on error
        }
    }
}