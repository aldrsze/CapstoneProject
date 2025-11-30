package com.inventorysystem.gui;

import com.inventorysystem.data.DashboardRepository;
import com.inventorysystem.data.UserRepository;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;

// Dashboard with compact stats and detailed sections
public class dashboardPanel extends JPanel {

    private JLabel totalProductsValue;
    private JLabel totalStockValue;
    private JLabel outOfStockValue;
    private JLabel totalCostValue;
    private JLabel totalIncomeValue;
    private JLabel totalProfitValue;
    
    // Inventory overview labels
    private JLabel totalItemsLabel;
    private JLabel currentValueLabel;
    
    // Table models
    private DefaultTableModel bestSellersModel;
    private DefaultTableModel stockAlertModel;

    private final int userId;
    private final DashboardRepository dashboardRepository;
    private final UserRepository userRepository;
    private DateRangePanel dateRangePanel;

    public dashboardPanel(userFrame mainFrame) {
        int originalUserId = mainFrame.loggedInUserId;
        String userRole = mainFrame.loggedInUserRole;
        this.userRepository = new UserRepository();
        
        // For employees, use their admin's user_id to see admin's data
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
        
        this.dashboardRepository = new DashboardRepository();

        setLayout(new BorderLayout(15, 15));
        setBackground(UIConstants.BACKGROUND_COLOR);
        setBorder(new EmptyBorder(15, 15, 15, 15));

        // Top panel with title and date range
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        
        JLabel titleLabel = new JLabel("Dashboard Overview");
        titleLabel.setFont(UIConstants.TITLE_FONT);
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        topPanel.add(titleLabel, BorderLayout.WEST);
        
        dateRangePanel = new DateRangePanel();
        dateRangePanel.addDateRangeChangeListener(() -> refreshData());
        topPanel.add(dateRangePanel, BorderLayout.EAST);
        
        add(topPanel, BorderLayout.NORTH);

        // Main content with scrolling
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBackground(UIConstants.BACKGROUND_COLOR);
        
        // Compact stats grid (smaller cards)
        JPanel statsGridPanel = new JPanel(new GridLayout(1, 6, 10, 10));
        statsGridPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        statsGridPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        totalProductsValue = createCompactValueLabel("0");
        totalStockValue = createCompactValueLabel("0");
        outOfStockValue = createCompactValueLabel("0");
        totalCostValue = createCompactValueLabel("₱0");
        totalIncomeValue = createCompactValueLabel("₱0");
        totalProfitValue = createCompactValueLabel("₱0");

        statsGridPanel.add(createCompactStatCard("Total Products", totalProductsValue, UIConstants.PRIMARY_COLOR));
        statsGridPanel.add(createCompactStatCard("Total Stocks", totalStockValue, UIConstants.PRIMARY_COLOR));
        statsGridPanel.add(createCompactStatCard("Out of Stock", outOfStockValue, UIConstants.DANGER_COLOR));
        statsGridPanel.add(createCompactStatCard("Total Cost", totalCostValue, UIConstants.TEXT_SECONDARY));
        statsGridPanel.add(createCompactStatCard("Total Income", totalIncomeValue, UIConstants.SUCCESS_COLOR));
        statsGridPanel.add(createCompactStatCard("Total Profit", totalProfitValue, UIConstants.WARNING_COLOR));
        
        mainContent.add(statsGridPanel);
        mainContent.add(Box.createRigidArea(new Dimension(0, 15)));
        
        // Best Sellers section
        mainContent.add(createBestSellersSection());
        mainContent.add(Box.createRigidArea(new Dimension(0, 15)));
        
        // Inventory Overview section
        mainContent.add(createInventoryOverviewSection());
        mainContent.add(Box.createRigidArea(new Dimension(0, 15)));
        
        // Stock Alert section
        mainContent.add(createStockAlertSection());
        
        JScrollPane scrollPane = new JScrollPane(mainContent);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                refreshData();
            }
        });

        refreshData();
    }

    private JLabel createCompactValueLabel(String initialText) {
        JLabel label = new JLabel(initialText, SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 24));
        label.setForeground(UIConstants.TEXT_PRIMARY);
        return label;
    }

    private JPanel createCompactStatCard(String title, JLabel valueLabel, Color accentColor) {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1),
            new EmptyBorder(15, 10, 15, 10)
        ));
        panel.setBackground(Color.WHITE);
        
        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        titleLabel.setForeground(UIConstants.TEXT_SECONDARY);
        
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createBestSellersSection() {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(Color.WHITE);
        section.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1),
            new EmptyBorder(15, 15, 15, 15)
        ));
        
        JLabel titleLabel = new JLabel("Best Sellers");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(new Color(22, 160, 133));
        section.add(titleLabel, BorderLayout.NORTH);
        
        String[] columns = {"#", "Product", "Sales Amount", "COGS", "Margin", "Qty Sold"};
        bestSellersModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JTable table = new JTable(bestSellersModel);
        table.setFont(UIConstants.TABLE_FONT);
        table.setRowHeight(35);
        table.setShowVerticalLines(false);
        table.setGridColor(UIConstants.BORDER_COLOR);
        table.setIntercellSpacing(new Dimension(0, 0));
        
        // Header styling
        javax.swing.table.JTableHeader header = table.getTableHeader();
        header.setFont(UIConstants.TABLE_HEADER_FONT);
        header.setBackground(UIConstants.PRIMARY_COLOR);
        header.setForeground(Color.WHITE);
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(header.getWidth(), 40));
        
        // Custom header renderer
        header.setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(UIConstants.PRIMARY_COLOR);
                c.setForeground(Color.WHITE);
                c.setFont(UIConstants.TABLE_HEADER_FONT);
                c.setHorizontalAlignment(JLabel.CENTER);
                c.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return c;
            }
        });
        
        // Custom cell renderer with striping
        javax.swing.table.DefaultTableCellRenderer cellRenderer = new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                // Smaller font for # column
                if (column == 0) {
                    setFont(new Font("Segoe UI", Font.PLAIN, 10));
                } else {
                    setFont(UIConstants.TABLE_FONT);
                }
                
                // Alignment: # centered
                if (column == 0) {
                    setHorizontalAlignment(JLabel.CENTER);
                } else if (column == 1) {
                    setHorizontalAlignment(JLabel.CENTER);
                } else {
                    setHorizontalAlignment(JLabel.CENTER);
                }
                
                // Row striping
                if (isSelected) {
                    c.setBackground(UIConstants.PRIMARY_LIGHT);
                    c.setForeground(Color.WHITE);
                } else {
                    if (row % 2 == 0) {
                        c.setBackground(Color.WHITE);
                    } else {
                        c.setBackground(UIConstants.BACKGROUND_COLOR);
                    }
                    c.setForeground(UIConstants.TEXT_PRIMARY);
                }
                
                // Padding
                if (c instanceof JLabel) {
                    ((JLabel) c).setBorder(new EmptyBorder(0, 10, 0, 10));
                }
                
                return c;
            }
        };
        
        // Apply renderer to all columns
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }
        
        // Set column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(35);  // #
        table.getColumnModel().getColumn(0).setMaxWidth(35);
        table.getColumnModel().getColumn(1).setPreferredWidth(250); // Product
        table.getColumnModel().getColumn(2).setPreferredWidth(120); // Sales Amount
        table.getColumnModel().getColumn(3).setPreferredWidth(100); // COGS
        table.getColumnModel().getColumn(4).setPreferredWidth(100); // Margin
        table.getColumnModel().getColumn(5).setPreferredWidth(80);  // Qty Sold
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1));
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        // Set preferred viewport height for exactly 5 rows (5 rows * 35px row height + 40px header)
        scrollPane.setPreferredSize(new Dimension(scrollPane.getPreferredSize().width, 40 + (5 * 35)));
        
        section.add(scrollPane, BorderLayout.CENTER);
        
        return section;
    }
    
    private JPanel createInventoryOverviewSection() {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(Color.WHITE);
        section.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1),
            new EmptyBorder(15, 15, 15, 15)
        ));
        
        JLabel titleLabel = new JLabel("Inventory Overview");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(new Color(22, 160, 133));
        section.add(titleLabel, BorderLayout.NORTH);
        
        JPanel infoPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        infoPanel.setBackground(Color.WHITE);
        infoPanel.setBorder(new EmptyBorder(15, 0, 0, 0));
        
        totalItemsLabel = new JLabel("0", SwingConstants.CENTER);
        totalItemsLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        
        currentValueLabel = new JLabel("₱0", SwingConstants.CENTER);
        currentValueLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        
        JPanel itemsPanel = new JPanel(new BorderLayout());
        itemsPanel.setBackground(Color.WHITE);
        JLabel itemsTitle = new JLabel("Total Items", SwingConstants.CENTER);
        itemsTitle.setFont(UIConstants.LABEL_FONT);
        itemsTitle.setForeground(UIConstants.TEXT_SECONDARY);
        itemsPanel.add(itemsTitle, BorderLayout.NORTH);
        itemsPanel.add(totalItemsLabel, BorderLayout.CENTER);
        
        JPanel valuePanel = new JPanel(new BorderLayout());
        valuePanel.setBackground(Color.WHITE);
        JLabel valueTitle = new JLabel("Current Value", SwingConstants.CENTER);
        valueTitle.setFont(UIConstants.LABEL_FONT);
        valueTitle.setForeground(UIConstants.TEXT_SECONDARY);
        valuePanel.add(valueTitle, BorderLayout.NORTH);
        valuePanel.add(currentValueLabel, BorderLayout.CENTER);
        
        infoPanel.add(itemsPanel);
        infoPanel.add(valuePanel);
        
        section.add(infoPanel, BorderLayout.CENTER);
        
        return section;
    }
    
    private JPanel createStockAlertSection() {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(Color.WHITE);
        section.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1),
            new EmptyBorder(15, 15, 15, 15)
        ));
        
        JLabel titleLabel = new JLabel("Stock Alert");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(new Color(22, 160, 133));
        section.add(titleLabel, BorderLayout.NORTH);
        
        String[] columns = {"#", "Description", "Quantity", "Status"};
        stockAlertModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JTable table = new JTable(stockAlertModel);
        table.setFont(UIConstants.TABLE_FONT);
        table.setRowHeight(35);
        table.setShowVerticalLines(false);
        table.setGridColor(UIConstants.BORDER_COLOR);
        table.setIntercellSpacing(new Dimension(0, 0));
        
        // Header styling
        javax.swing.table.JTableHeader header = table.getTableHeader();
        header.setFont(UIConstants.TABLE_HEADER_FONT);
        header.setBackground(UIConstants.PRIMARY_COLOR);
        header.setForeground(Color.WHITE);
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(header.getWidth(), 40));
        
        // Custom header renderer
        header.setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(UIConstants.PRIMARY_COLOR);
                c.setForeground(Color.WHITE);
                c.setFont(UIConstants.TABLE_HEADER_FONT);
                c.setHorizontalAlignment(JLabel.CENTER);
                c.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return c;
            }
        });
        
        // Custom cell renderer with striping
        javax.swing.table.DefaultTableCellRenderer cellRenderer = new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                // Font & Alignment Logic
                if (column == 0) {
                    setFont(new Font("Segoe UI", Font.PLAIN, 10));
                } else if (column == 3) { // Status Column - Bold
                    setFont(new Font("Segoe UI", Font.BOLD, 12));
                } else {
                    setFont(UIConstants.TABLE_FONT);
                }
                
                // Alignment
                if (column == 1) { // Description
                    setHorizontalAlignment(JLabel.LEFT);
                } else {
                    setHorizontalAlignment(JLabel.CENTER);
                }
                
                // Color Logic
                if (isSelected) {
                    c.setBackground(UIConstants.PRIMARY_LIGHT);
                    c.setForeground(Color.WHITE);
                } else {
                    // Background Striping
                    if (row % 2 == 0) {
                        c.setBackground(Color.WHITE);
                    } else {
                        c.setBackground(UIConstants.BACKGROUND_COLOR);
                    }
                    
                    // --- NEW: Custom Text Colors for Status (Column 3) ---
                    if (column == 3) {
                        String status = value != null ? value.toString() : "";
                        if (status.equals("Out of Stock")) {
                            c.setForeground(new Color(255, 0, 0)); // Bright Red
                        } else if (status.equals("Low Stock")) {
                            c.setForeground(new Color(255, 165, 0)); // Orange
                        } else if (status.equals("Normal")) {
                            c.setForeground(new Color(0, 128, 0)); // Medium Green
                        } else if (status.equals("High Stock")) {
                            c.setForeground(new Color(0, 0, 255)); // Blue
                        } else {
                            c.setForeground(UIConstants.TEXT_PRIMARY);
                        }
                    } else {
                        c.setForeground(UIConstants.TEXT_PRIMARY);
                    }
                }
                
                // Padding
                if (c instanceof JLabel) {
                    ((JLabel) c).setBorder(new EmptyBorder(0, 10, 0, 10));
                }
                
                return c;
            }
        };
        
        // Apply renderer to all columns
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }
        
        // Set column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(35);  // #
        table.getColumnModel().getColumn(0).setMaxWidth(35);
        table.getColumnModel().getColumn(1).setPreferredWidth(300); // Description
        table.getColumnModel().getColumn(2).setPreferredWidth(100); // Quantity
        table.getColumnModel().getColumn(3).setPreferredWidth(120); // Status
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1));
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        section.add(scrollPane, BorderLayout.CENTER);
        
        return section;
    }

    
    private void refreshData() {
        updateTotalProducts();
        updateTotalStock();
        updateOutOfStock();
        updateTotalCost();
        updateTotalIncomeWithDateRange();
        updateTotalProfitWithDateRange();
        updateInventoryOverview();
        updateBestSellers();
        updateStockAlerts();
    }

    private void updateTotalProducts() {
        try {
            int count = dashboardRepository.getTotalProducts(userId);
            totalProductsValue.setText(String.valueOf(count));
        } catch (SQLException e) {
            System.err.println("Error updating total products: " + e.getMessage());
            totalProductsValue.setText("Error");
        }
    }

    private void updateTotalStock() {
        try {
            int totalStock = dashboardRepository.getTotalStock(userId);
            totalStockValue.setText(String.valueOf(totalStock));
        } catch (SQLException e) {
             System.err.println("Error updating total stock: " + e.getMessage());
            totalStockValue.setText("Error");
        }
    }

    private void updateOutOfStock() {
        try {
            int outOfStock = dashboardRepository.getOutOfStockCount(userId);
            outOfStockValue.setText(String.valueOf(outOfStock));
        } catch (SQLException e) {
             System.err.println("Error updating out of stock count: " + e.getMessage());
            outOfStockValue.setText("Error");
        }
    }

    private void updateTotalCost() {
        try {
            double totalCost = dashboardRepository.getTotalInventoryCost(userId);
            totalCostValue.setText("₱" + String.format("%,.0f", totalCost));
        } catch (SQLException e) {
            System.err.println("Error updating total inventory cost: " + e.getMessage());
            totalCostValue.setText("Error");
        }
    }

    private void updateTotalIncomeWithDateRange() {
        try {
            java.sql.Date startDate = dateRangePanel.getSqlStartDate();
            java.sql.Date endDate = dateRangePanel.getSqlEndDate();
            double totalIncome = dashboardRepository.getTotalIncome(userId, startDate, endDate);
            totalIncomeValue.setText("₱" + String.format("%,.0f", totalIncome));
        } catch (SQLException e) {
            System.err.println("Error updating total income: " + e.getMessage());
            totalIncomeValue.setText("Error");
        }
    }

    private void updateTotalProfitWithDateRange() {
        try {
            java.sql.Date startDate = dateRangePanel.getSqlStartDate();
            java.sql.Date endDate = dateRangePanel.getSqlEndDate();
            double totalProfit = dashboardRepository.getTotalProfit(userId, startDate, endDate);
            totalProfitValue.setText("₱" + String.format("%,.0f", totalProfit));
        } catch (SQLException e) {
            System.err.println("Error updating total profit: " + e.getMessage());
            totalProfitValue.setText("Error");
        }
    }
    
    private void updateInventoryOverview() {
        try {
            int totalItems = dashboardRepository.getTotalProducts(userId);
            double currentValue = dashboardRepository.getTotalInventoryCost(userId);
            totalItemsLabel.setText(String.valueOf(totalItems));
            currentValueLabel.setText("₱" + String.format("%,.0f", currentValue));
        } catch (SQLException e) {
            System.err.println("Error updating inventory overview: " + e.getMessage());
        }
    }
    
    private void updateBestSellers() {
        try {
            bestSellersModel.setRowCount(0);
            
            // Get dates from the panel
            java.sql.Date startDate = dateRangePanel.getSqlStartDate();
            java.sql.Date endDate = dateRangePanel.getSqlEndDate();
            
            // Call the date-aware repository method
            java.util.List<Object[]> bestSellers = dashboardRepository.getBestSellers(userId, startDate, endDate, 5);
            
            if (bestSellers.isEmpty()) {
                // Updated placeholder message
                bestSellersModel.addRow(new Object[]{"", "No sales in this period", "", "", "", ""});
            } else {
                int rowNum = 1;
                for (Object[] row : bestSellers) {
                    Object[] rowWithNumber = new Object[row.length + 1];
                    rowWithNumber[0] = rowNum++;
                    System.arraycopy(row, 0, rowWithNumber, 1, row.length);
                    bestSellersModel.addRow(rowWithNumber);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error updating best sellers: " + e.getMessage());
            bestSellersModel.setRowCount(0);
            bestSellersModel.addRow(new Object[]{"", "Error loading data", "", "", "", ""});
        }
    }
    
    private void updateStockAlerts() {
        try {
            stockAlertModel.setRowCount(0);
            java.util.List<Object[]> alerts = dashboardRepository.getStockAlerts(userId);
            int rowNum = 1;
            for (Object[] row : alerts) {
                Object[] rowWithNumber = new Object[row.length + 1];
                rowWithNumber[0] = rowNum++;
                System.arraycopy(row, 0, rowWithNumber, 1, row.length);
                stockAlertModel.addRow(rowWithNumber);
            }
        } catch (SQLException e) {
            System.err.println("Error updating stock alerts: " + e.getMessage());
            stockAlertModel.setRowCount(0);
            stockAlertModel.addRow(new Object[]{"", "Error loading data", "", ""});
        }
    }

    // Test method
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Dashboard Test");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1400, 900);
            userFrame mockFrame = new userFrame();
            mockFrame.loggedInUserId = 1;
            mockFrame.loggedInUserRole = "Admin";
            frame.add(new dashboardPanel(mockFrame));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}

