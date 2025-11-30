package com.inventorysystem.gui;

import com.inventorysystem.data.StockRepository;
import com.inventorysystem.data.UserRepository;
import com.inventorysystem.model.StockRecord;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import java.awt.*;
import java.sql.SQLException;
import java.util.List;

// Shows stock movements (in, out, current) with date filtering
public class stockPanel extends JPanel {

    private DefaultTableModel model;
    private JTable stockTable;
    private final int userId;
    private final StockRepository stockRepository;
    private final UserRepository userRepository;
    private DateRangePanel dateRangePanel;

    public stockPanel(userFrame mainFrame) {
        int originalUserId = mainFrame.loggedInUserId;
        String userRole = mainFrame.loggedInUserRole;
        this.userRepository = new UserRepository();
        
        // Employee sees admin's data
        int effectiveUserId = originalUserId;
        if (userRole.equalsIgnoreCase("Employee")) {
            try {
                int adminId = userRepository.getAdminIdForEmployee(originalUserId);
                if (adminId > 0) {
                    effectiveUserId = adminId;
                }
            } catch (Exception e) {
                System.err.println("Error getting admin ID for employee: " + e.getMessage());
            }
        }
        this.userId = effectiveUserId;
        
        this.stockRepository = new StockRepository();

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(Color.WHITE);

        //Initialize Table FIRST so stockTable exists for the search bar
        JScrollPane tableScrollPane = createTablePanel();

        //Then create Top Panel (Search Bar depends on table)
        JPanel topPanel = createTopPanel();

        add(topPanel, BorderLayout.NORTH);
        add(tableScrollPane, BorderLayout.CENTER);

        // Reload data when this panel is shown
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                loadStockSummary();
            }
        });

        loadStockSummary();
    }

    // Title and date filter
    // createTopPanel with Search Bar
    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout(15, 0));
        topPanel.setBackground(Color.WHITE);
        topPanel.setBorder(new EmptyBorder(0, 0, 15, 0));

        // Left: Title
        JLabel titleLabel = new JLabel("STOCKS SUMMARY");
        titleLabel.setFont(UIConstants.TITLE_FONT);
        topPanel.add(titleLabel, BorderLayout.WEST);
        
        // Right: Date Range + Search
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightPanel.setBackground(Color.WHITE);

        // Date Filter
        dateRangePanel = new DateRangePanel();
        dateRangePanel.addDateRangeChangeListener(() -> loadStockSummary());
        rightPanel.add(dateRangePanel);
        
        // Search Field
        JTextField searchField = new JTextField(15);
        searchField.putClientProperty("JTextField.placeholderText", "Search stocks...");
        searchField.setFont(UIConstants.INPUT_FONT);
        searchField.setPreferredSize(new Dimension(180, 35));
        
        // Filter Logic
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        stockTable.setRowSorter(sorter);
        
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            private void filter() {
                String text = searchField.getText();
                if (text.trim().length() == 0) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
                }
            }
        });
        
        rightPanel.add(new JLabel("Search:"));
        rightPanel.add(searchField);
        
        topPanel.add(rightPanel, BorderLayout.EAST);
        return topPanel;
    }

    // Table showing stock movements
    private JScrollPane createTablePanel() {
        String[] columnNames = {"#", "ID", "Product Name", "Category", "Stock In", "Stock Out", "Available", "Status"};
        model = new DefaultTableModel(new Object[][]{}, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        stockTable = new JTable(model);
        stockTable.setFillsViewportHeight(true);
        stockTable.setFont(UIConstants.TABLE_FONT);
        stockTable.setRowHeight(35);
        stockTable.setShowVerticalLines(false);
        stockTable.setGridColor(UIConstants.BORDER_COLOR);
        stockTable.setIntercellSpacing(new Dimension(0, 0));
        stockTable.setSelectionBackground(UIConstants.PRIMARY_LIGHT);
        stockTable.setSelectionForeground(Color.WHITE);
        stockTable.setAutoCreateRowSorter(true);
        
        // Header styling
        javax.swing.table.JTableHeader header = stockTable.getTableHeader();
        header.setFont(UIConstants.TABLE_HEADER_FONT);
        header.setBackground(UIConstants.PRIMARY_COLOR);
        header.setForeground(Color.WHITE);
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(header.getWidth(), 40));
        
        header.setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                java.awt.Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(UIConstants.PRIMARY_COLOR);
                c.setForeground(Color.WHITE);
                c.setFont(UIConstants.TABLE_HEADER_FONT);
                ((javax.swing.JLabel) c).setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return c;
            }
        });
        
        // Custom cell renderer with striping and status colors
        stockTable.setDefaultRenderer(Object.class, new CustomTableRenderer());
        
        // Set column widths
        stockTable.getColumnModel().getColumn(0).setPreferredWidth(35);  // #
        stockTable.getColumnModel().getColumn(0).setMaxWidth(35);
        stockTable.getColumnModel().getColumn(1).setPreferredWidth(60);  // ID
        stockTable.getColumnModel().getColumn(2).setPreferredWidth(250); // Product Name
        stockTable.getColumnModel().getColumn(3).setPreferredWidth(120); // Category
        stockTable.getColumnModel().getColumn(4).setPreferredWidth(80);  // Stock In
        stockTable.getColumnModel().getColumn(5).setPreferredWidth(80);  // Stock Out
        stockTable.getColumnModel().getColumn(6).setPreferredWidth(80);  // Available
        stockTable.getColumnModel().getColumn(7).setPreferredWidth(100); // Status

        JScrollPane scrollPane = new JScrollPane(stockTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1));
        scrollPane.getViewport().setBackground(Color.WHITE);
        return scrollPane;
    }

    // Get data from database and put in table
    private void loadStockSummary() {
        model.setRowCount(0);
        try {
            java.sql.Date startDate = dateRangePanel.getSqlStartDate();
            java.sql.Date endDate = dateRangePanel.getSqlEndDate();
            List<StockRecord> summary = stockRepository.getStockSummaryWithDateRange(userId, startDate, endDate);
            
            int rowNum = 1;
            for (StockRecord record : summary) {
                int available = record.endingStock();
                String status;
                
                // Determine status based on stock level
                if (available == 0) {
                    status = "Out of Stock";
                } else if (available < 20) {
                    status = "Low Stock";;
                } else if (available < 40) {
                    status = "Normal";;
                } else {
                    status = "High Stock";
                }
                
                model.addRow(new Object[]{
                    rowNum++,
                    record.productId(),
                    record.productName(),
                    record.categoryName(),
                    record.stockIn(),
                    record.stockOut(),
                    available,
                    status
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Could not load stock summary: " + e.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    // Test method
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Stock Test");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1000, 600);
            userFrame mockFrame = new userFrame();
            mockFrame.loggedInUserId = 1;
            mockFrame.loggedInUserRole = "Admin";
            frame.add(new stockPanel(mockFrame));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}