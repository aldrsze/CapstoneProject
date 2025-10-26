package GUI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import Database.DatabaseConnection;

public class recordsPanel extends JPanel {

    private final int storeId; // Add storeId
    private DefaultTableModel model;

    public recordsPanel(int storeId) { // Update constructor
        this.storeId = storeId;
        
        // --- UI Setup (Unchanged) ---
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(Color.WHITE);
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.WHITE);
        JLabel titleLabel = new JLabel("TRANSACTION HISTORY");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        topPanel.add(titleLabel, BorderLayout.WEST);
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadTransactionHistory());
        topPanel.add(refreshButton, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);
        String[] columnNames = {"Date", "Product Name", "Transaction Type", "Quantity", "Price", "Total"};
        model = new DefaultTableModel(new Object[][]{}, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable recordsTable = new JTable(model);
        recordsTable.setFillsViewportHeight(true);
        recordsTable.setFont(new Font("Arial", Font.PLAIN, 14));
        recordsTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));

        recordsTable.setRowHeight(25);
        recordsTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        recordsTable.getColumnModel().getColumn(1).setPreferredWidth(250);
        recordsTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        JScrollPane tableScrollPane = new JScrollPane(recordsTable);
        add(tableScrollPane, BorderLayout.CENTER);

        loadTransactionHistory();
    }

    private void loadTransactionHistory() {
        model.setRowCount(0);
        
        // UPDATED: Query now filters both parts of the UNION by store_id
        String sql = 
            "(SELECT s.sale_date AS transaction_date, p.name AS product_name, 'Sale' AS transaction_type, " +
            "-si.quantity_sold AS quantity, si.unit_price AS price, s.total_amount AS total " +
            "FROM sales s " +
            "JOIN sale_items si ON s.sale_id = si.sale_id " +
            "JOIN products p ON si.product_id = p.product_id WHERE s.store_id = ?) " +
            "UNION ALL " +
            "(SELECT sl.log_date AS transaction_date, p.name AS product_name, 'Stock-In' AS transaction_type, " +
            "sl.quantity_changed AS quantity, p.price AS price, (p.price * sl.quantity_changed) AS total " +
            "FROM stock_log sl " +
            "JOIN products p ON sl.product_id = p.product_id " +
            "WHERE sl.log_type = 'STOCK-IN' AND sl.store_id = ?) " + // Assuming stock_log also has a store_id
            "ORDER BY transaction_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, this.storeId); // Matches the first '?' (in the sales query)
            pstmt.setInt(2, this.storeId); // Matches the second '?' (in the stock-in query)

            ResultSet rs = pstmt.executeQuery();

            while(rs.next()) {
                model.addRow(new Object[]{
                    rs.getTimestamp("transaction_date"),
                    rs.getString("product_name"),
                    rs.getString("transaction_type"),
                    rs.getInt("quantity"),
                    rs.getDouble("price"),
                    rs.getDouble("total")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Could not load transaction history: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}