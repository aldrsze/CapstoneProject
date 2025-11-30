package com.inventorysystem.gui;

import com.inventorysystem.data.RecordsRepository;
import com.inventorysystem.data.UserRepository;
import com.inventorysystem.model.TransactionRecord;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

// Transaction history with date filtering and search
public class recordsPanel extends JPanel {

    private final DefaultTableModel tableModel;
    private JTable recordsTable; // Class-level reference for the search bar
    private final RecordsRepository recordsRepository;
    private final UserRepository userRepository;
    private final int userId;
    private DateRangePanel dateRangePanel;

    public recordsPanel(userFrame mainFrame) {
        int originalUserId = mainFrame.loggedInUserId;
        String userRole = mainFrame.loggedInUserRole;
        this.userRepository = new UserRepository();
        
        // For employees, use their admin's user_id
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
        
        this.recordsRepository = new RecordsRepository();
        
        setLayout(new BorderLayout(10, 10));
        setBackground(UIConstants.BACKGROUND_COLOR);
        setBorder(new CompoundBorder(
            new LineBorder(UIConstants.BORDER_COLOR, 1),
            new EmptyBorder(20, 20, 20, 20)));

        // --- 1. Initialize Model FIRST ---
        tableModel = new DefaultTableModel(new String[]{
            "#", "Date", "Product", "Type", "Qty", "Retail", "Cost/Unit", "Total"
        }, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        // --- 2. Initialize Table Panel SECOND (This creates recordsTable) ---
        JScrollPane tableScrollPane = createTablePanel();
        add(tableScrollPane, BorderLayout.CENTER);

        // --- 3. Initialize Top Panel THIRD (Search Bar needs recordsTable) ---
        add(createTopPanel(), BorderLayout.NORTH);

        // Load data when panel is shown
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent e) {
                loadData();
            }
        });

        loadData();
    }

    // Title, Date Filter, and Search Bar
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 0));
        panel.setBackground(UIConstants.BACKGROUND_COLOR);
        panel.setBorder(new EmptyBorder(0, 0, 15, 0));

        // Left: Title
        JLabel titleLabel = new JLabel("TRANSACTION HISTORY");
        titleLabel.setFont(UIConstants.TITLE_FONT.deriveFont(Font.BOLD, 20f));
        titleLabel.setBorder(new EmptyBorder(5, 10, 5, 20));
        panel.add(titleLabel, BorderLayout.WEST);
        
        // Right: Date Range + Search
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        
        // Date Filter
        dateRangePanel = new DateRangePanel();
        dateRangePanel.addDateRangeChangeListener(() -> loadData());
        rightPanel.add(dateRangePanel);

        // Search Field
        JTextField searchField = new JTextField(15);
        searchField.setFont(UIConstants.INPUT_FONT);
        searchField.setPreferredSize(new Dimension(180, 35));
        
        // Filter Logic - recordsTable MUST be initialized before this
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        recordsTable.setRowSorter(sorter);
        
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

        panel.add(rightPanel, BorderLayout.EAST);
        return panel;
    }

    // Transaction table
    private JScrollPane createTablePanel() {
        // Assign to class variable 'recordsTable' (NOT a local variable)
        recordsTable = new JTable(tableModel);
        
        recordsTable.setAutoCreateRowSorter(true);
        recordsTable.setFont(UIConstants.TABLE_FONT);
        recordsTable.setRowHeight(35);
        recordsTable.setShowVerticalLines(false);
        recordsTable.setGridColor(UIConstants.BORDER_COLOR);
        recordsTable.setIntercellSpacing(new Dimension(0, 0));
        recordsTable.setSelectionBackground(UIConstants.PRIMARY_LIGHT);
        recordsTable.setSelectionForeground(Color.WHITE);
        
        // Header styling
        javax.swing.table.JTableHeader header = recordsTable.getTableHeader();
        header.setFont(UIConstants.TABLE_HEADER_FONT);
        header.setBackground(UIConstants.PRIMARY_COLOR);
        header.setForeground(Color.WHITE);
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(header.getWidth(), 40));
        header.setOpaque(true);
        
        // Force header colors with custom renderer
        header.setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                javax.swing.JLabel label = (javax.swing.JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setBackground(UIConstants.PRIMARY_COLOR);
                label.setForeground(Color.WHITE);
                label.setFont(UIConstants.TABLE_HEADER_FONT);
                label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                label.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 10));
                label.setOpaque(true);
                return label;
            }
        });
        
        // Custom cell renderer with striping
        recordsTable.setDefaultRenderer(Object.class, new CustomTableRenderer());
        
        recordsTable.getColumnModel().getColumn(0).setPreferredWidth(35);
        recordsTable.getColumnModel().getColumn(0).setMaxWidth(35);

        JScrollPane scrollPane = new JScrollPane(recordsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1));
        scrollPane.getViewport().setBackground(Color.WHITE);
        return scrollPane;
    }
    
    // Load transactions from database
    public void loadData() {
        tableModel.setRowCount(0);
        
        new Thread(() -> {
            try {
                java.sql.Date startDate = dateRangePanel.getSqlStartDate();
                java.sql.Date endDate = dateRangePanel.getSqlEndDate();
                List<TransactionRecord> history = recordsRepository.getTransactionHistoryWithDateRange(
                    userId, startDate, endDate);
                
                SwingUtilities.invokeLater(() -> {
                    int rowNum = 1;
                    for (TransactionRecord record : history) {
                        tableModel.addRow(new Object[]{
                            rowNum++,
                            record.transactionDate(),
                            record.productName(),
                            record.transactionType(),
                            record.quantity(),
                            record.retailPrice() == 0.0 ? "" : String.format("₱%,.2f", record.retailPrice()),
                            String.format("₱%,.2f", record.unitPrice()),
                            String.format("₱%,.2f", record.total())
                        });
                    }
                });
            } catch (SQLException e) {
                SwingUtilities.invokeLater(() -> 
                    JOptionPane.showMessageDialog(this, 
                        "Error loading transaction history: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    // Test method
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Records Test");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1000, 600);
            userFrame mockFrame = new userFrame();
            mockFrame.loggedInUserId = 1;
            mockFrame.loggedInUserRole = "Admin";
            frame.add(new recordsPanel(mockFrame));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}