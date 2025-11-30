
package com.inventorysystem.gui;

import com.inventorysystem.data.*;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamException;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import org.json.JSONException;
import org.json.JSONObject;

import com.inventorysystem.model.*;
import com.inventorysystem.util.SoundUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.geom.Rectangle2D;
import java.text.AttributedString;
import javax.swing.border.LineBorder;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.filechooser.FileNameExtensionFilter;

public class productsPanel extends JPanel {

    // --- UI Components ---
    private DefaultTableModel tableModel;
    private JTable productsTable;
    private JPanel scannerDisplayPanel;
    private JRadioButton addUpdateModeRadio;
    private JRadioButton sellModeRadio;
    private JRadioButton deleteModeRadio;
    private ButtonGroup scanModeGroup;
    private JButton toggleScanButton;

    // --- Webcam & Scanner ---
    private WebcamPanel webcamPanel;
    private Webcam webcam;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean isScanning = new AtomicBoolean(false);

    // --- Data & State ---
    private final int userId;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final String userRole;
    private final userFrame mainFrame;

    // Add a dialog reference at class level
    private JDialog currentDetailsDialog = null;

    /**
     * Creates the products management panel.
     * @param mainFrame The main application frame providing user context.
     */
    public productsPanel(userFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.userId = mainFrame.loggedInUserId;
        this.userRole = mainFrame.loggedInUserRole;
        this.userRepository = new UserRepository();
        
        // For employees, use their admin's user_id to access admin's products
        int effectiveUserId = this.userId;
        if (userRole.equalsIgnoreCase("Employee")) {
            try {
                int adminId = userRepository.getAdminIdForEmployee(this.userId);
                if (adminId > 0) {
                    effectiveUserId = adminId;
                }
            } catch (SQLException e) {
                System.err.println("Error getting admin ID for employee: " + e.getMessage());
            }
        }
        
        this.productRepository = new ProductRepository(effectiveUserId);

        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JScrollPane tableScrollPane = createTablePanel();
        JPanel topPanel = createTopPanel();

        add(topPanel, BorderLayout.NORTH);
        add(tableScrollPane, BorderLayout.CENTER);

        JPanel sidePanel = createSidePanel();
        add(sidePanel, BorderLayout.EAST);

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                loadProductsFromDB();
            }
            
            @Override
            public void componentHidden(java.awt.event.ComponentEvent e) {
                if (isScanning.get()) {
                    stopScanner();
                }
            }
        });

        loadProductsFromDB();
    }

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout(15, 0));
        topPanel.setBackground(Color.WHITE);
        topPanel.setBorder(new EmptyBorder(0, 0, 10, 0)); // Padding bottom
        
        // title
        JLabel titleLabel = new JLabel("MANAGE PRODUCTS");
        titleLabel.setFont(UIConstants.TITLE_FONT);
        topPanel.add(titleLabel, BorderLayout.WEST);

        // right side container with search and refresh button
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setBackground(Color.WHITE);

        // search field
        JTextField searchField = new JTextField(20);
        searchField.putClientProperty("JTextField.placeHolderText", "Search products...");
        searchField.setFont(UIConstants.INPUT_FONT);

        // add filter logic
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        productsTable.setRowSorter(sorter);

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            private void filter() {
                String text = searchField.getText();
                if (text.trim().length() == 0) {
                    sorter.setRowFilter(null);
                } else {
                    // Case-insensitive search on all columns
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
                }
            }
        });

        rightPanel.add(new JLabel("Search:"));
        rightPanel.add(searchField);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.setFont(UIConstants.BUTTON_FONT);
        refreshButton.setFocusPainted(false);
        refreshButton.addActionListener(e -> {
            searchField.setText(""); //clear search
            loadProductsFromDB();
        });

        rightPanel.add(refreshButton);

        topPanel.add(rightPanel, BorderLayout.EAST);

        return topPanel;
    }

    /** Creates the scrollable table panel for displaying products. */
    private JScrollPane createTablePanel() {
        String[] columnNames = {"#", "ID", "Product Name", "Category", "Cost Price", "Total Cost", "Markup %", "Retail Price", "Total Retail", "Stock"};
        tableModel = new DefaultTableModel(new Object[][]{}, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        productsTable = new JTable(tableModel);
        productsTable.setAutoCreateRowSorter(true);
        
        // Enable multiple row selection
        productsTable.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        productsTable.setFont(UIConstants.TABLE_FONT);
        productsTable.setRowHeight(35); // Slightly taller rows for modern look
        productsTable.setShowVerticalLines(false);
        productsTable.setGridColor(UIConstants.BORDER_COLOR);
        productsTable.setIntercellSpacing(new Dimension(0, 0));
        
        // Header Styling
        javax.swing.table.JTableHeader header = productsTable.getTableHeader();
        header.setFont(UIConstants.TABLE_HEADER_FONT);
        header.setBackground(UIConstants.PRIMARY_COLOR);
        header.setForeground(Color.WHITE);
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(header.getWidth(), 40)); // Taller header
        
        // Custom Header Renderer (Centered & Padded)
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

        // Custom Cell Renderer for Rows (Striping, Alignment & Padding)
        productsTable.setDefaultRenderer(Object.class, new CustomTableRenderer());

        // Column widths
        productsTable.getColumnModel().getColumn(0).setPreferredWidth(35);  // #
        productsTable.getColumnModel().getColumn(0).setMaxWidth(35);
        productsTable.getColumnModel().getColumn(1).setPreferredWidth(60);  // Product ID
        productsTable.getColumnModel().getColumn(2).setPreferredWidth(150); // Name
        productsTable.getColumnModel().getColumn(3).setPreferredWidth(110); // Category
        productsTable.getColumnModel().getColumn(4).setPreferredWidth(90);  // Cost Price
        productsTable.getColumnModel().getColumn(5).setPreferredWidth(100); // Total Cost
        productsTable.getColumnModel().getColumn(6).setPreferredWidth(70);  // Markup
        productsTable.getColumnModel().getColumn(7).setPreferredWidth(90);  // Retail Price
        productsTable.getColumnModel().getColumn(8).setPreferredWidth(100); // Total Retail
        productsTable.getColumnModel().getColumn(9).setPreferredWidth(60);  // Stock

        JScrollPane scrollPane = new JScrollPane(productsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1));
        scrollPane.getViewport().setBackground(Color.WHITE); // Ensures empty table area is white
        return scrollPane;
    }

    /** Creates the side panel containing manual controls and the scanner display. */
    private JPanel createSidePanel() {
        JPanel mainSidePanel = new JPanel(new BorderLayout(UIConstants.COMPONENT_SPACING, UIConstants.COMPONENT_SPACING));
        mainSidePanel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(UIConstants.BORDER_COLOR, 1),
            new EmptyBorder(UIConstants.PANEL_PADDING, UIConstants.PANEL_PADDING, UIConstants.PANEL_PADDING, UIConstants.PANEL_PADDING)
        ));

        mainSidePanel.setPreferredSize(new Dimension(320, 0));
        mainSidePanel.setMinimumSize(new Dimension(300, 0));
        mainSidePanel.setBackground(UIConstants.FORM_COLOR);

        // Title Section
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(UIConstants.PRIMARY_COLOR);
        titlePanel.setBorder(new EmptyBorder(8, 12, 8, 12));
        JLabel titleLabel = new JLabel("SCANNER & CONTROLS", SwingConstants.CENTER);
        titleLabel.setFont(UIConstants.SUBTITLE_FONT);
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel);
        mainSidePanel.add(titlePanel, BorderLayout.NORTH);

        // Scrollable Content Panel for low resolution support
        JPanel contentPanel = new JPanel(new BorderLayout(UIConstants.COMPONENT_SPACING, UIConstants.COMPONENT_SPACING));
        contentPanel.setBackground(UIConstants.FORM_COLOR);
        contentPanel.setBorder(new EmptyBorder(UIConstants.COMPONENT_SPACING, 0, 0, 0));

        // Scanner Display Area (smaller for low res)
        scannerDisplayPanel = new JPanel(new BorderLayout());
        scannerDisplayPanel.setBorder(new LineBorder(Color.BLACK, 2, false)); // Plain black border
        scannerDisplayPanel.setPreferredSize(new Dimension(280, 200));

        JLabel offLabel = new JLabel("Scanner Inactive", SwingConstants.CENTER);
        offLabel.setFont(UIConstants.LABEL_FONT);
        offLabel.setForeground(UIConstants.TEXT_SECONDARY);
        scannerDisplayPanel.add(offLabel, BorderLayout.CENTER);
        contentPanel.add(scannerDisplayPanel, BorderLayout.NORTH);

        // Controls Section
        JPanel controlsContainer = new JPanel();
        controlsContainer.setLayout(new BoxLayout(controlsContainer, BoxLayout.Y_AXIS));
        controlsContainer.setBackground(UIConstants.FORM_COLOR);
        controlsContainer.setBorder(new EmptyBorder(UIConstants.COMPONENT_SPACING, 0, 0, 0));

        // Action Buttons - ALL SAME COLOR
        JButton addProductButton = createSideButton("Add / Edit Product", UIConstants.PRIMARY_COLOR);
        JButton setMarkupButton = createSideButton("Set Product Markup", UIConstants.PRIMARY_COLOR);
        JButton generateQRButton = createSideButton("Generate QR Code", UIConstants.SUCCESS_COLOR);
        JButton sellProductButton = createSideButton("Sell Product", UIConstants.PRIMARY_COLOR);
        JButton returnProductButton = createSideButton("Return Product", UIConstants.PRIMARY_COLOR);
        JButton removeStockButton = createSideButton("Remove Stock", UIConstants.PRIMARY_COLOR);
        JButton deleteProductButton = createSideButton("Delete Product", UIConstants.DANGER_COLOR);

        // Role-based configuration
        if (userRole.equalsIgnoreCase("Employee")) {
            addProductButton.setEnabled(false);
            addProductButton.setBackground(Color.GRAY);
            setMarkupButton.setEnabled(false);
            setMarkupButton.setBackground(Color.GRAY);
            generateQRButton.setEnabled(false);
            generateQRButton.setBackground(Color.GRAY);
            returnProductButton.setEnabled(false);
            returnProductButton.setBackground(Color.GRAY);
            removeStockButton.setEnabled(false);
            removeStockButton.setBackground(Color.GRAY);
            deleteProductButton.setEnabled(false);
            deleteProductButton.setBackground(Color.GRAY);
        } else {
            productsTable.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    int[] selectedRows = productsTable.getSelectedRows();
                    int selectedCount = selectedRows.length;
                    
                    if (selectedCount == 0) {
                        // No selection
                        addProductButton.setText("Add / Edit Product");
                        addProductButton.setEnabled(true);
                        setMarkupButton.setText("Set Product Markup");
                        setMarkupButton.setEnabled(true);
                        generateQRButton.setText("Generate QR Code");
                        generateQRButton.setEnabled(true);
                        sellProductButton.setEnabled(true);
                    } else if (selectedCount == 1) {
                        // Single selection - all buttons enabled
                        addProductButton.setText("Edit Product ID: " + tableModel.getValueAt(selectedRows[0], 1)); // ID column
                        addProductButton.setEnabled(true);
                        setMarkupButton.setText("Set Markup ID: " + tableModel.getValueAt(selectedRows[0], 1)); // ID column
                        setMarkupButton.setEnabled(true);
                        generateQRButton.setText("Generate QR ID: " + tableModel.getValueAt(selectedRows[0], 1)); // ID column
                        generateQRButton.setEnabled(true);
                        sellProductButton.setEnabled(true);
                    } else {
                        // Multiple selection - disable edit, QR, and sell
                        addProductButton.setText("Add / Edit Product");
                        addProductButton.setEnabled(false);
                        setMarkupButton.setText("Set Markup (" + selectedCount + " selected)");
                        setMarkupButton.setEnabled(true);
                        generateQRButton.setText("Generate QR Code");
                        generateQRButton.setEnabled(false);
                        sellProductButton.setEnabled(false);
                    }
                }
            });
        }

        controlsContainer.add(addProductButton);
        controlsContainer.add(Box.createRigidArea(new Dimension(0, 6)));
        controlsContainer.add(setMarkupButton);
        controlsContainer.add(Box.createRigidArea(new Dimension(0, 6)));
        controlsContainer.add(generateQRButton);
        controlsContainer.add(Box.createRigidArea(new Dimension(0, 6)));
        controlsContainer.add(sellProductButton);
        controlsContainer.add(Box.createRigidArea(new Dimension(0, 6)));
        controlsContainer.add(returnProductButton);
        controlsContainer.add(Box.createRigidArea(new Dimension(0, 6)));
        controlsContainer.add(removeStockButton);
        controlsContainer.add(Box.createRigidArea(new Dimension(0, 6)));
        controlsContainer.add(deleteProductButton);

        // Scanner Mode Section
        JPanel scannerSection = new JPanel();
        scannerSection.setLayout(new BoxLayout(scannerSection, BoxLayout.Y_AXIS));
        scannerSection.setBackground(UIConstants.FORM_COLOR);
        scannerSection.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(Color.BLACK, 1, false), // Plain black border
            new EmptyBorder(8, 8, 8, 8)
        ));

        JLabel modeLabel = new JLabel("Scanner Mode:");
        modeLabel.setFont(UIConstants.LABEL_BOLD_FONT);
        modeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        scannerSection.add(modeLabel);
        scannerSection.add(Box.createRigidArea(new Dimension(0, 6)));

        addUpdateModeRadio = new JRadioButton("Add/Update Products", true);
        sellModeRadio = new JRadioButton("Sell Products");
        deleteModeRadio = new JRadioButton("Delete Products");
        
        addUpdateModeRadio.setFont(UIConstants.LABEL_FONT);
        sellModeRadio.setFont(UIConstants.LABEL_FONT);
        deleteModeRadio.setFont(UIConstants.LABEL_FONT);
        
        addUpdateModeRadio.setBackground(UIConstants.FORM_COLOR);
        sellModeRadio.setBackground(UIConstants.FORM_COLOR);
        deleteModeRadio.setBackground(UIConstants.FORM_COLOR);
        
        if (userRole.equalsIgnoreCase("Employee")) {
            addUpdateModeRadio.setEnabled(false);
            deleteModeRadio.setEnabled(false);
            sellModeRadio.setSelected(true);
        }
        
        scanModeGroup = new ButtonGroup();
        scanModeGroup.add(addUpdateModeRadio);
        scanModeGroup.add(sellModeRadio);
        scanModeGroup.add(deleteModeRadio);

        scannerSection.add(addUpdateModeRadio);
        scannerSection.add(sellModeRadio);
        scannerSection.add(deleteModeRadio);

        controlsContainer.add(Box.createRigidArea(new Dimension(0, 8)));
        controlsContainer.add(scannerSection);

        // Scanner Buttons Panel
        JPanel scannerButtonsPanel = new JPanel();
        scannerButtonsPanel.setLayout(new BoxLayout(scannerButtonsPanel, BoxLayout.Y_AXIS));
        scannerButtonsPanel.setBackground(UIConstants.FORM_COLOR);

        toggleScanButton = createSideButton("Start Scanner", UIConstants.PRIMARY_COLOR);
        toggleScanButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        
        // Changed from WARNING_COLOR to ACCENT_COLOR for better alignment
        JButton scanImageButton = createSideButton("Scan QR from Image", UIConstants.ACCENT_COLOR);
        scanImageButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        
        JButton generateQRDialogButton = createSideButton("Generate QR", UIConstants.SUCCESS_COLOR);
        generateQRDialogButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        scannerButtonsPanel.add(toggleScanButton);
        scannerButtonsPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        scannerButtonsPanel.add(scanImageButton);
        scannerButtonsPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        scannerButtonsPanel.add(generateQRDialogButton);

        controlsContainer.add(Box.createRigidArea(new Dimension(0, 8)));
        controlsContainer.add(scannerButtonsPanel);
        
        // Button actions for scanner
        generateQRDialogButton.addActionListener(e -> showGenerateQRDialog());

        // Wrap controls in a scrollable panel for low resolution support
        JScrollPane scrollPane = new JScrollPane(controlsContainer);
        scrollPane.setBorder(null);
        scrollPane.setBackground(UIConstants.FORM_COLOR);
        scrollPane.getViewport().setBackground(UIConstants.FORM_COLOR);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        contentPanel.add(scrollPane, BorderLayout.CENTER);
        mainSidePanel.add(contentPanel, BorderLayout.CENTER);

        // Attach Button Actions
        addProductButton.addActionListener(e -> handleAddOrUpdateProductViaDialog());
        setMarkupButton.addActionListener(e -> handleSetProductMarkup());
        generateQRButton.addActionListener(e -> handleGenerateQRCode());
        sellProductButton.addActionListener(e -> handleSellProductManual());
        returnProductButton.addActionListener(e -> handleReturnProduct());
        removeStockButton.addActionListener(e -> handleRemoveStock());
        deleteProductButton.addActionListener(e -> handleDeleteProduct());
        toggleScanButton.addActionListener(e -> toggleScanner());
        scanImageButton.addActionListener(e -> handleScanQRFromImage());

        return mainSidePanel;
    }

    private JButton createSideButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(UIConstants.BUTTON_FONT);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMargin(new Insets(5, 15, 5, 5));
        button.setPreferredSize(new Dimension(280, UIConstants.INPUT_HEIGHT));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, UIConstants.INPUT_HEIGHT));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(bgColor.darker());
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });
        
        return button;
    }

    // --- Data Loading & Table Methods ---

    /** Loads product data from the repository and populates the JTable. */
    private void loadProductsFromDB() {
        tableModel.setRowCount(0);
        
        new Thread(() -> {
            try {
                List<Product> products = productRepository.getAllProducts();
                double defaultMarkup = userRepository.getDefaultMarkup(userId);
                
                SwingUtilities.invokeLater(() -> {
                    for (Product product : products) {
                        // Get the stored retail price from database
                        double storedRetailPrice = product.retailPrice();
                        double displayRetailPrice;
                        
                        // Get product-specific markup from database
                        String markupDisplay = "";
                        Double productMarkup = null;
                        
                        try {
                            productMarkup = productRepository.getProductMarkup(product.id());
                        } catch (SQLException e) {
                            markupDisplay = "N/A";
                        }

                        // Calculate display retail price based on priority:
                        // 1. Use stored retail price if exists (> 0)
                        // 2. Otherwise calculate from markup (product-specific or default)
                        if (storedRetailPrice > 0.0) {
                            displayRetailPrice = storedRetailPrice;
                        } else {
                            // Calculate from markup
                            double costPrice = product.costPrice();
                            double markupToUse = (productMarkup != null) ? productMarkup : defaultMarkup;
                            displayRetailPrice = costPrice * (1 + markupToUse / 100.0);
                            displayRetailPrice = Math.round(displayRetailPrice * 100.0) / 100.0;
                        }

                        // Set markup display text
                        if (!markupDisplay.equals("N/A")) {
                            if (productMarkup != null) {
                                markupDisplay = String.format("%.1f%%", productMarkup);
                            } else {
                                markupDisplay = String.format("%.1f%%", defaultMarkup);
                            }
                        }

                        // Calculate total retail price
                        double totalRetailPrice = displayRetailPrice * product.stock();
                        
                        tableModel.addRow(new Object[]{
                            tableModel.getRowCount() + 1, // Row number
                            product.id(),
                            product.name(),
                            product.categoryName(),
                            String.format("₱%,.2f", product.costPrice()),
                            String.format("₱%,.2f", product.totalCost()),
                            markupDisplay,
                            String.format("₱%,.2f", displayRetailPrice),
                            String.format("₱%,.2f", totalRetailPrice),
                            product.stock()
                        });
                    }
                });
            } catch (SQLException e) {
                SwingUtilities.invokeLater(() -> {
                    showError("Error loading products: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();
    }

    /** Finds a product by ID in the table, selects it, and scrolls to it. */
    private void findAndSelectProduct(String id) {
        SwingUtilities.invokeLater(() -> {
            try {
                int productId = Integer.parseInt(id);
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    if (tableModel.getValueAt(i, 1) != null && productId == (int) tableModel.getValueAt(i, 1)) {
                        productsTable.setRowSelectionInterval(i, i);
                        productsTable.scrollRectToVisible(productsTable.getCellRect(i, 0, true));
                        return;
                    }
                }
            } catch (NumberFormatException e) {
            } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
                System.err.println("Error selecting row: " + e.getMessage());
            }
        });
    }

    /** Loads categories into a JComboBox and populates a Map to link names to IDs. */
    public void loadCategoriesIntoComboBox(JComboBox<String> comboBox, Map<String, Integer> categoryIdMap) {
        comboBox.removeAllItems();
        categoryIdMap.clear();
        try {
            List<Category> categories = productRepository.getCategories();
            if (categories.isEmpty()) {
                comboBox.addItem("<No categories available>");
                comboBox.setEnabled(false);
            } else {
                comboBox.setEnabled(true);
                for (Category category : categories) {
                    categoryIdMap.put(category.name(), category.id());
                    comboBox.addItem(category.name());
                }
            }
        } catch (SQLException e) {
            showError("Could not load categories: " + e.getMessage());
            comboBox.addItem("<Error loading categories>");
            comboBox.setEnabled(false);
        }
    }

    // --- Manual Action Handlers ---

    /** Handles the "Add / Update Product (Manual)" button click WITHOUT markup and retail price fields. */
    private void handleAddOrUpdateProductViaDialog() {
        if (userRole.equalsIgnoreCase("Employee")) {
            showError("Access Denied: Only Admins can add/update products.");
            return;
        }

        int selectedRow = productsTable.getSelectedRow();
        boolean isEditMode = (selectedRow != -1);
        String dialogTitle = isEditMode ? "Edit Product" : "Add New Product";

        // Create dialog
        JDialog productDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), dialogTitle, true);
        productDialog.setLayout(new BorderLayout());
        productDialog.setSize(550, 550);
        productDialog.setLocationRelativeTo(this);
        productDialog.setResizable(false);

        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(isEditMode ? UIConstants.PRIMARY_COLOR : UIConstants.SUCCESS_COLOR);
        headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel headerLabel = new JLabel(dialogTitle, SwingConstants.CENTER);
        headerLabel.setFont(UIConstants.SUBTITLE_FONT);
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel);
        productDialog.add(headerPanel, BorderLayout.NORTH);

        // Main content panel
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.weightx = 1.0;
        
        // Fields
        JTextField idField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField costPriceField = new JTextField();
        JTextField stockField = new JTextField();
        JComboBox<String> categoryComboBox = new JComboBox<>();
        Map<String, Integer> categoryIdMap = new HashMap<>();

        // Load categories
        loadCategoriesIntoComboBox(categoryComboBox, categoryIdMap);

        if (isEditMode) {
            try {
                idField.setText(tableModel.getValueAt(selectedRow, 1).toString());
                nameField.setText(tableModel.getValueAt(selectedRow, 2).toString());
                
                // Get cost price - remove formatting
                String costPriceStr = tableModel.getValueAt(selectedRow, 4).toString();
                costPriceStr = costPriceStr.replace("₱", "").replace(",", "").trim();
                costPriceField.setText(costPriceStr);
                
                stockField.setText(tableModel.getValueAt(selectedRow, 9).toString());
                
                String categoryName = tableModel.getValueAt(selectedRow, 3).toString();
                categoryComboBox.setSelectedItem(categoryName);
                
                idField.setEditable(false);
                idField.setBackground(new Color(240, 240, 240));
            } catch (Exception e) {
                showError("Error loading product data: " + e.getMessage());
                productDialog.dispose();
                return;
            }
        } else {
            idField.setText("(Auto-generated)");
            idField.setEditable(false);
            idField.setBackground(new Color(240, 240, 240));
        }

        // Product ID
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel idLabel = new JLabel(isEditMode ? "Product ID (Editing):" : "Product ID:");
        idLabel.setFont(UIConstants.LABEL_BOLD_FONT);
        if (isEditMode) {
            idLabel.setForeground(UIConstants.PRIMARY_COLOR);
        }
        mainPanel.add(idLabel, gbc);
        
        gbc.gridx = 1;
        idField.setFont(UIConstants.INPUT_FONT);
        idField.setPreferredSize(new Dimension(0, UIConstants.INPUT_HEIGHT));
        mainPanel.add(idField, gbc);

        // Product Name
        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel nameLabel = new JLabel("Product Name:");
        nameLabel.setFont(UIConstants.LABEL_BOLD_FONT);
        mainPanel.add(nameLabel, gbc);
        
        gbc.gridx = 1;
        nameField.setFont(UIConstants.INPUT_FONT);
        nameField.setPreferredSize(new Dimension(0, UIConstants.INPUT_HEIGHT));
        mainPanel.add(nameField, gbc);

        // Category
        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel categoryLabel = new JLabel("Category:");
        categoryLabel.setFont(UIConstants.LABEL_BOLD_FONT);
        mainPanel.add(categoryLabel, gbc);
        
        gbc.gridx = 1;
        JPanel categoryPanel = new JPanel(new BorderLayout(5, 0));
        categoryPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        categoryComboBox.setFont(UIConstants.INPUT_FONT);
        categoryComboBox.setPreferredSize(new Dimension(0, UIConstants.INPUT_HEIGHT));
        categoryPanel.add(categoryComboBox, BorderLayout.CENTER);
        
        JButton addCategoryButton = new JButton("+");
        addCategoryButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        addCategoryButton.setPreferredSize(new Dimension(40, UIConstants.INPUT_HEIGHT));
        addCategoryButton.setBackground(UIConstants.PRIMARY_COLOR);
        addCategoryButton.setForeground(Color.WHITE);
        addCategoryButton.setFocusPainted(false);
        addCategoryButton.setToolTipText("Add new category");
        addCategoryButton.addActionListener(e -> {
            handleAddNewCategoryDialog(categoryComboBox, categoryIdMap);
        });
        categoryPanel.add(addCategoryButton, BorderLayout.EAST);
        mainPanel.add(categoryPanel, gbc);

        // Cost Price
        gbc.gridx = 0;
        gbc.gridy = 3;
        JLabel costLabel = new JLabel("Cost Price:");
        costLabel.setFont(UIConstants.LABEL_BOLD_FONT);
        mainPanel.add(costLabel, gbc);
        
        gbc.gridx = 1;
        costPriceField.setFont(UIConstants.INPUT_FONT);
        costPriceField.setPreferredSize(new Dimension(0, UIConstants.INPUT_HEIGHT));
        mainPanel.add(costPriceField, gbc);

        // Stock
        gbc.gridx = 0;
        gbc.gridy = 4;
        JLabel stockLabel = new JLabel("Stock Quantity:");
        stockLabel.setFont(UIConstants.LABEL_BOLD_FONT);
        mainPanel.add(stockLabel, gbc);
        
        gbc.gridx = 1;
        stockField.setFont(UIConstants.INPUT_FONT);
        stockField.setPreferredSize(new Dimension(0, UIConstants.INPUT_HEIGHT));
        mainPanel.add(stockField, gbc);

        // Info label
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        JLabel infoLabel = new JLabel(
            "<html><i><font color='#7f8c8d' size='3'>" +
            "Use 'Set Markup' button to configure pricing after saving" +
            "</font></i></html>"
        );
        mainPanel.add(infoLabel, gbc);

        productDialog.add(mainPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(UIConstants.BUTTON_FONT);
        cancelButton.setPreferredSize(new Dimension(100, 35));
        cancelButton.addActionListener(e -> productDialog.dispose());
        
        JButton saveButton = new JButton(isEditMode ? "Update" : "Save");
        saveButton.setFont(UIConstants.BUTTON_FONT);
        saveButton.setBackground(isEditMode ? UIConstants.PRIMARY_COLOR : UIConstants.SUCCESS_COLOR);
        saveButton.setForeground(Color.WHITE);
        saveButton.setPreferredSize(new Dimension(100, 35));
        saveButton.setFocusPainted(false);
        saveButton.setBorderPainted(false);
        saveButton.setContentAreaFilled(false);
        saveButton.setOpaque(true);
        saveButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveButton.addActionListener(e -> {
            try {
                // For new products, use 0 to trigger auto-increment
                int id = isEditMode ? Integer.parseInt(idField.getText()) : 0;
                String name = nameField.getText();
                double costPrice = Double.parseDouble(costPriceField.getText());
                int stock = Integer.parseInt(stockField.getText());
                String selectedCategory = (String) categoryComboBox.getSelectedItem();

                if (name.trim().isEmpty() || selectedCategory == null || selectedCategory.startsWith("<")) {
                    showError("Name and a valid Category are required.");
                    return;
                }
                int categoryId = categoryIdMap.getOrDefault(selectedCategory, -1);
                if (categoryId == -1) {
                    showError("Invalid category selected.");
                    return;
                }
                if (stock < 0 || costPrice < 0) {
                    showError("Cost Price and Stock cannot be negative.");
                    return;
                }

                // No retail price or markup in this dialog - pass nulls
                int newProductId = productRepository.manualUpsertProduct(id, name, categoryId, costPrice, null, null, stock);
                if (newProductId > 0) {
                    showSuccess(isEditMode ? "Product updated successfully!\nUse 'Set Markup' to adjust pricing." : 
                        "Product added successfully! (ID: " + newProductId + ")\nUse 'Set Markup' to set pricing.");
                    loadProductsFromDB();
                    findAndSelectProduct(String.valueOf(newProductId));
                    productDialog.dispose();
                } else {
                    showError("Error: Failed to save product.");
                }
            } catch (NumberFormatException ex) {
                showError("Cost Price and Stock must be valid numbers.");
            } catch (SQLException ex) {
                if (ex.getMessage() != null && ex.getMessage().contains("Duplicate entry")) {
                    showError("Error adding product: Product ID " + idField.getText() + " already exists.");
                } else {
                    showError("Error saving product: " + (ex.getMessage() != null ? ex.getMessage() : "Unknown SQL error"));
                }
            }
        });
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        productDialog.add(buttonPanel, BorderLayout.SOUTH);
        
        productDialog.setVisible(true);
    }

    /** Handles adding a new category from the dialog shown by handleAddOrUpdateProductViaDialog. */
    private void handleAddNewCategoryDialog(JComboBox<String> categoryComboBox, Map<String, Integer> categoryIdMap) {
        String newCategory = JOptionPane.showInputDialog(this, "Enter new category name:", "Add Category", JOptionPane.PLAIN_MESSAGE);
        if (newCategory != null && !newCategory.trim().isEmpty()) {
            try {
                productRepository.addNewCategory(newCategory);
                showSuccess("Category '" + newCategory + "' added.");
                loadCategoriesIntoComboBox(categoryComboBox, categoryIdMap);
                categoryComboBox.setSelectedItem(newCategory);
            } catch (SQLException ex) {
                showError("Error adding category: " + ex.getMessage());
            }
        }
    }


    /** Handles the "Sell Selected Product (Manual)" button click. */
    private void handleSellProductManual() {
        int selectedRow = productsTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("Please select a product from the table first.");
            return;
        }

        // Get product details from the selected table row
        String productId = tableModel.getValueAt(selectedRow, 1).toString();
        String productName = tableModel.getValueAt(selectedRow, 2).toString();
        int currentStock = (int) tableModel.getValueAt(selectedRow, 9);
        
        // Get selling price - remove formatting
        String sellingPriceStr = tableModel.getValueAt(selectedRow, 5).toString();
        double sellingPrice = Double.parseDouble(sellingPriceStr.replace("₱", "").replace(",", "").trim());

        // Create sell dialog
        JDialog sellDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Sell Product", true);
        sellDialog.setLayout(new BorderLayout());
        sellDialog.setSize(450, 350);
        sellDialog.setLocationRelativeTo(this);
        sellDialog.setResizable(false);

        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UIConstants.SUCCESS_COLOR);
        headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel headerLabel = new JLabel("Sell Product: " + productName, SwingConstants.CENTER);
        headerLabel.setFont(UIConstants.SUBTITLE_FONT);
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel);
        sellDialog.add(headerPanel, BorderLayout.NORTH);

        // Main content panel
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.weightx = 1.0;
        
        // Product Info
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel productLabel = new JLabel("Product:");
        productLabel.setFont(UIConstants.LABEL_BOLD_FONT);
        mainPanel.add(productLabel, gbc);
        
        gbc.gridx = 1;
        JLabel productValueLabel = new JLabel(productName);
        productValueLabel.setFont(UIConstants.LABEL_FONT);
        productValueLabel.setForeground(UIConstants.PRIMARY_COLOR);
        mainPanel.add(productValueLabel, gbc);

        // Current Stock
        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel stockLabel = new JLabel("Available Stock:");
        stockLabel.setFont(UIConstants.LABEL_BOLD_FONT);
        mainPanel.add(stockLabel, gbc);
        
        gbc.gridx = 1;
        JLabel stockValueLabel = new JLabel(String.valueOf(currentStock));
        stockValueLabel.setFont(UIConstants.LABEL_FONT);
        mainPanel.add(stockValueLabel, gbc);

        // Selling Price
        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel priceLabel = new JLabel("Unit Price:");
        priceLabel.setFont(UIConstants.LABEL_BOLD_FONT);
        mainPanel.add(priceLabel, gbc);
        
        gbc.gridx = 1;
        JLabel priceValueLabel = new JLabel(String.format("₱%,.2f", sellingPrice));
        priceValueLabel.setFont(UIConstants.LABEL_FONT);
        mainPanel.add(priceValueLabel, gbc);

        // Quantity to Sell
        gbc.gridx = 0;
        gbc.gridy = 3;
        JLabel qtyLabel = new JLabel("Quantity to Sell:");
        qtyLabel.setFont(UIConstants.LABEL_BOLD_FONT);
        mainPanel.add(qtyLabel, gbc);
        
        gbc.gridx = 1;
        JTextField quantityField = new JTextField("1");
        quantityField.setFont(UIConstants.INPUT_FONT);
        quantityField.setPreferredSize(new Dimension(0, UIConstants.INPUT_HEIGHT));
        mainPanel.add(quantityField, gbc);

        sellDialog.add(mainPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(UIConstants.BUTTON_FONT);
        cancelButton.setPreferredSize(new Dimension(100, 35));
        cancelButton.addActionListener(e -> sellDialog.dispose());
        
        JButton sellButton = new JButton("Sell");
        sellButton.setFont(UIConstants.BUTTON_FONT);
        sellButton.setBackground(UIConstants.SUCCESS_COLOR);
        sellButton.setForeground(Color.WHITE);
        sellButton.setPreferredSize(new Dimension(100, 35));
        sellButton.setFocusPainted(false);
        sellButton.setBorderPainted(false);
        sellButton.setContentAreaFilled(false);
        sellButton.setOpaque(true);
        sellButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sellButton.addActionListener(e -> {
            try {
                // Parse quantity input
                int quantity = Integer.parseInt(quantityField.getText());

                // Validate quantity
                if (quantity > 0) {
                    processSale(productId, quantity, true);
                    sellDialog.dispose();
                } else {
                    showError("Quantity must be positive.");
                }
            } catch (NumberFormatException ex) {
                showError("Invalid quantity entered.");
            }
            // processSale handles its own SQLException and table refresh
        });
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(sellButton);
        sellDialog.add(buttonPanel, BorderLayout.SOUTH);
        
        sellDialog.setVisible(true);
    }

    /** Handles the "Return Product" button click for returning/rejecting merchandise. */
    private void handleReturnProduct() {
        if (userRole.equalsIgnoreCase("Employee")) {
            showError("Access Denied: Only Admins can process product returns.");
            return;
        }

        int[] selectedRows = productsTable.getSelectedRows();
        if (selectedRows.length == 0) {
            showError("Please select a product from the table first.");
            return;
        }

        // Multiple selection - use bulk dialog
        if (selectedRows.length > 1) {
            showBulkOperationDialog("RETURN", selectedRows);
            return;
        }

        // Single selection - use original dialog
        int selectedRow = selectedRows[0];
        // Get product details from the selected table row
        int productId = (int) tableModel.getValueAt(selectedRow, 1);
        String productName = (String) tableModel.getValueAt(selectedRow, 2);
        int currentStock = (int) tableModel.getValueAt(selectedRow, 9);
        
        // Get cost price - remove formatting
        String costPriceStr = tableModel.getValueAt(selectedRow, 4).toString();
        double costPrice = Double.parseDouble(costPriceStr.replace("₱", "").replace(",", "").trim());

        // Create compact return dialog
        JDialog returnDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Return Product", true);
        returnDialog.setLayout(new BorderLayout());
        returnDialog.setSize(550, 480);
        returnDialog.setLocationRelativeTo(this);
        returnDialog.setResizable(false);

        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UIConstants.PRIMARY_COLOR);
        headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel headerLabel = new JLabel("Return Product: " + productName, SwingConstants.CENTER);
        headerLabel.setFont(UIConstants.SUBTITLE_FONT);
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel);
        returnDialog.add(headerPanel, BorderLayout.NORTH);

        // Main content panel
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 1.0;
        
        // Stock info label
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel stockInfo = new JLabel("Current Stock: " + currentStock + " | Cost Price: " + String.format("₱%,.2f", costPrice));
        stockInfo.setFont(UIConstants.LABEL_FONT);
        stockInfo.setForeground(UIConstants.TEXT_SECONDARY);
        mainPanel.add(stockInfo, gbc);
        
        // Return Type label
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        JLabel typeLabel = new JLabel("Return Type:");
        typeLabel.setFont(UIConstants.LABEL_BOLD_FONT);
        mainPanel.add(typeLabel, gbc);
        
        // Return Type dropdown
        gbc.gridx = 1;
        JComboBox<String> returnTypeCombo = new JComboBox<>(new String[]{
            "Customer Return",
            "Reject/Damaged",
            "Refund to Supplier"
        });
        returnTypeCombo.setFont(UIConstants.INPUT_FONT);
        returnTypeCombo.setPreferredSize(new Dimension(0, UIConstants.INPUT_HEIGHT));
        mainPanel.add(returnTypeCombo, gbc);
        
        // Quantity label
        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel qtyLabel = new JLabel("Quantity:");
        qtyLabel.setFont(UIConstants.LABEL_BOLD_FONT);
        mainPanel.add(qtyLabel, gbc);
        
        // Quantity field
        gbc.gridx = 1;
        JTextField quantityField = new JTextField("1");
        quantityField.setFont(UIConstants.INPUT_FONT);
        quantityField.setPreferredSize(new Dimension(0, UIConstants.INPUT_HEIGHT));
        mainPanel.add(quantityField, gbc);
        
        // Notes label
        gbc.gridx = 0;
        gbc.gridy = 3;
        JLabel notesLabel = new JLabel("Notes:");
        notesLabel.setFont(UIConstants.LABEL_BOLD_FONT);
        mainPanel.add(notesLabel, gbc);
        
        // Notes field
        gbc.gridx = 1;
        JTextField reasonField = new JTextField();
        reasonField.setFont(UIConstants.INPUT_FONT);
        reasonField.setPreferredSize(new Dimension(0, UIConstants.INPUT_HEIGHT));
        mainPanel.add(reasonField, gbc);
        
        // Info text
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        JLabel infoLabel = new JLabel(
            "<html><i><font color='#7f8c8d' size='3'>" +
            "Customer Return: Adds to stock | Reject: Marks damaged | Refund: Returns to supplier" +
            "</font></i></html>"
        );
        mainPanel.add(infoLabel, gbc);

        returnDialog.add(mainPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        buttonPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        buttonPanel.setBorder(new EmptyBorder(0, 20, 10, 20));
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(UIConstants.BUTTON_FONT);
        cancelButton.setPreferredSize(new Dimension(120, 38));
        cancelButton.setBackground(Color.GRAY);
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setFocusPainted(false);
        cancelButton.setBorderPainted(false);
        cancelButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JButton confirmButton = new JButton("Process Return");
        confirmButton.setFont(UIConstants.BUTTON_FONT);
        confirmButton.setPreferredSize(new Dimension(160, 38));
        confirmButton.setBackground(UIConstants.PRIMARY_COLOR);
        confirmButton.setForeground(Color.WHITE);
        confirmButton.setFocusPainted(false);
        confirmButton.setBorderPainted(false);
        confirmButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        cancelButton.addActionListener(e -> returnDialog.dispose());
        
        confirmButton.addActionListener(e -> {
            try {
                int quantity = Integer.parseInt(quantityField.getText());
                String reason = reasonField.getText().trim();
                String returnType = (String) returnTypeCombo.getSelectedItem();
                
                if (quantity <= 0) {
                    showError("Quantity must be positive.");
                    return;
                }
                
                // For refund, check if there's enough stock
                if (returnType.equals("Refund to Supplier") && quantity > currentStock) {
                    showError("Cannot refund more than current stock (" + currentStock + ").");
                    return;
                }

                if (reason.isEmpty()) {
                    if (returnType.equals("Customer Return")) {
                        reason = "Customer return - item resellable";
                    } else if (returnType.equals("Reject/Damaged")) {
                        reason = "Rejected/Damaged product";
                    } else {
                        reason = "Refund to supplier";
                    }
                }

                // Process the return based on type
                if (returnType.equals("Customer Return")) {
                    productRepository.customerReturn(productId, quantity, reason);
                    showSuccess("Successfully processed customer return!\n" + 
                               quantity + " unit(s) of '" + productName + "' added back to stock.");
                } else if (returnType.equals("Reject/Damaged")) {
                    productRepository.rejectProduct(productId, quantity, reason);
                    showSuccess("Successfully processed rejection!\n" + 
                               quantity + " unit(s) of '" + productName + "' marked as DAMAGED.");
                } else {
                    productRepository.refundProduct(productId, quantity, reason);
                    showSuccess("Successfully processed refund!\n" + 
                               quantity + " unit(s) of '" + productName + "' returned to supplier.\n" +
                               "Refund amount: " + String.format("₱%,.2f", costPrice * quantity));
                }
                
                returnDialog.dispose();
            } catch (NumberFormatException ex) {
                showError("Invalid quantity entered.");
            } catch (SQLException ex) {
                showError("Error processing return: " + ex.getMessage());
            } finally {
                loadProductsFromDB();
                findAndSelectProduct(String.valueOf(productId));
            }
        });
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(confirmButton);
        returnDialog.add(buttonPanel, BorderLayout.SOUTH);
        
        returnDialog.setVisible(true);
    }

    /** Handles the "Remove Stock (Manual)" button click. */
    private void handleRemoveStock() {
        if (userRole.equalsIgnoreCase("Employee")) {
            showError("Access Denied: Only Admins can remove stock.");
            return;
        }
       
        int[] selectedRows = productsTable.getSelectedRows();
        if (selectedRows.length == 0) {
            showError("Please select at least one product from the table first.");
            return;
        }

        // Multiple selection - use bulk dialog
        if (selectedRows.length > 1) {
            showBulkOperationDialog("REMOVE_STOCK", selectedRows);
            return;
        }

        // Single selection - show individual dialog
        showIndividualRemoveStockDialog(selectedRows[0]);
    }

    /** Shows individual remove stock dialog for a single product */
    private void showIndividualRemoveStockDialog(int selectedRow) {
        // Get product details from the selected table row
        int productId = (int) tableModel.getValueAt(selectedRow, 1);
        String productName = (String) tableModel.getValueAt(selectedRow, 2);
        int currentStock = (int) tableModel.getValueAt(selectedRow, 9);

        // Create dialog
        JDialog removeDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Remove Stock", true);
        removeDialog.setLayout(new BorderLayout());
        removeDialog.setSize(450, 300);
        removeDialog.setLocationRelativeTo(this);
        removeDialog.setResizable(false);

        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UIConstants.DANGER_COLOR);
        headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel headerLabel = new JLabel("Remove Stock: " + productName, SwingConstants.CENTER);
        headerLabel.setFont(UIConstants.SUBTITLE_FONT);
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel);
        removeDialog.add(headerPanel, BorderLayout.NORTH);

        // Main content panel
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 1.0;
        
        // Stock info label
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel stockLabel = new JLabel("Current Stock: " + currentStock + " units");
        stockLabel.setFont(UIConstants.LABEL_FONT);
        mainPanel.add(stockLabel, gbc);
        
        // Quantity label
        gbc.gridy = 1; gbc.gridwidth = 1;
        JLabel quantityLabel = new JLabel("Quantity to Remove:");
        quantityLabel.setFont(UIConstants.LABEL_FONT);
        mainPanel.add(quantityLabel, gbc);
        
        // Quantity field
        gbc.gridx = 1;
        JTextField quantityField = new JTextField(10);
        quantityField.setFont(UIConstants.INPUT_FONT);
        mainPanel.add(quantityField, gbc);
        
        // Reason label
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel reasonLabel = new JLabel("Reason:");
        reasonLabel.setFont(UIConstants.LABEL_FONT);
        mainPanel.add(reasonLabel, gbc);
        
        // Reason field
        gbc.gridx = 1;
        JTextField reasonField = new JTextField();
        reasonField.setFont(UIConstants.INPUT_FONT);
        mainPanel.add(reasonField, gbc);

        removeDialog.add(mainPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBackground(UIConstants.BACKGROUND_COLOR);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(UIConstants.BUTTON_FONT);
        cancelButton.setPreferredSize(new Dimension(100, 35));
        cancelButton.addActionListener(e -> removeDialog.dispose());

        JButton confirmButton = new JButton("Remove");
        confirmButton.setFont(UIConstants.BUTTON_FONT);
        confirmButton.setBackground(UIConstants.DANGER_COLOR);
        confirmButton.setForeground(Color.WHITE);
        confirmButton.setPreferredSize(new Dimension(100, 35));
        confirmButton.addActionListener(e -> {
            try {
                int quantity = Integer.parseInt(quantityField.getText().trim());
                String reason = reasonField.getText().trim();

                if (quantity <= 0) {
                    showError("Quantity must be greater than zero.");
                    return;
                }
                if (quantity > currentStock) {
                    showError("Cannot remove more than current stock (" + currentStock + ").");
                    return;
                }

                // Process the stock removal
                productRepository.removeStock(productId, quantity, reason);
                showSuccess("Successfully removed " + quantity + " unit(s) from '" + productName + "'");
                
                removeDialog.dispose();
            } catch (NumberFormatException ex) {
                showError("Invalid quantity entered.");
            } catch (SQLException ex) {
                showError("Error removing stock: " + ex.getMessage());
            } finally {
                loadProductsFromDB();
                findAndSelectProduct(String.valueOf(productId));
            }
        });
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(confirmButton);
        removeDialog.add(buttonPanel, BorderLayout.SOUTH);
        
        removeDialog.setVisible(true);
    }

    /** Handles the "Delete Product" button click. */
    private void handleDeleteProduct() {
        if (userRole.equalsIgnoreCase("Employee")) {
            showError("Access Denied: Only Admins can delete products.");
            return;
        }
       
        int[] selectedRows = productsTable.getSelectedRows();
        if (selectedRows.length == 0) {
            showError("Please select at least one product from the table first.");
            return;
        }

        // Multiple selection - use bulk dialog
        if (selectedRows.length > 1) {
            showBulkOperationDialog("DELETE", selectedRows);
            return;
        }

        // Single selection - show individual dialog
        showIndividualDeleteDialog(selectedRows[0]);
    }

    /** Shows individual delete confirmation dialog for a single product */
    private void showIndividualDeleteDialog(int selectedRow) {
        int productId = (int) tableModel.getValueAt(selectedRow, 1); // ID column
        String productName = (String) tableModel.getValueAt(selectedRow, 2); // Product Name column

        JDialog deleteDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Delete Product", true);
        deleteDialog.setLayout(new BorderLayout());
        deleteDialog.setSize(500, 280);
        deleteDialog.setLocationRelativeTo(this);
        deleteDialog.setResizable(false);

        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UIConstants.DANGER_COLOR);
        headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel headerLabel = new JLabel("Delete Product", SwingConstants.CENTER);
        headerLabel.setFont(UIConstants.SUBTITLE_FONT);
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel);
        deleteDialog.add(headerPanel, BorderLayout.NORTH);

        // Main content panel
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 5, 10, 5);
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        
        JLabel warningLabel = new JLabel("Warning: This action cannot be undone!");
        warningLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        warningLabel.setForeground(UIConstants.DANGER_COLOR);
        warningLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(warningLabel, gbc);

        gbc.gridy = 1;
        JLabel messageLabel = new JLabel("<html><center>Are you sure you want to delete:<br><b>" + productName + "</b> (ID: " + productId + ")?</center></html>");
        messageLabel.setFont(UIConstants.LABEL_FONT);
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(messageLabel, gbc);

        deleteDialog.add(mainPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(UIConstants.BUTTON_FONT);
        cancelButton.setPreferredSize(new Dimension(100, 35));
        cancelButton.setFocusPainted(false);
        cancelButton.setContentAreaFilled(false);
        cancelButton.setOpaque(true);
        cancelButton.setBackground(Color.LIGHT_GRAY);
        cancelButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelButton.addActionListener(e -> deleteDialog.dispose());
        
        JButton deleteButton = new JButton("Delete");
        deleteButton.setFont(UIConstants.BUTTON_FONT);
        deleteButton.setBackground(UIConstants.DANGER_COLOR);
        deleteButton.setForeground(Color.WHITE);
        deleteButton.setPreferredSize(new Dimension(100, 35));
        deleteButton.setFocusPainted(false);
        deleteButton.setBorderPainted(false);
        deleteButton.setContentAreaFilled(false);
        deleteButton.setOpaque(true);
        deleteButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        deleteButton.addActionListener(e -> {
            try {
                if (productRepository.deleteProduct(productId)) {
                    showSuccess("Product '" + productName + "' deleted successfully!");
                    deleteDialog.dispose();
                } else {
                    showError("Failed to delete product. It may not exist.");
                }
            } catch (Exception ex) {
                showError("Error deleting product: " + ex.getMessage());
            } finally {
                loadProductsFromDB();
            }
        });
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(deleteButton);
        deleteDialog.add(buttonPanel, BorderLayout.SOUTH);
        
        deleteDialog.setVisible(true);
    }

    // --- QR Code & Sale Processing ---

    /** Processes a sale transaction, updating stock via the repository. */
    private void processSale(String productId, int quantityToSell, boolean showSuccessPopup) {
        try {
            productRepository.sellProduct(productId, quantityToSell);
            if (showSuccessPopup) {
                showSuccess("Sale recorded successfully!");
            }
        } catch (SQLException e) {
            showError("Transaction Error: " + e.getMessage());
        } finally {
            loadProductsFromDB();
            findAndSelectProduct(productId);
        }
    }

    /**
     * Processes QR code commands using direct DB lookup for 100% accuracy.
     */
    private void processQRCodeCommand(String qrText) {
        try {
            JSONObject mainObject = new JSONObject(qrText);
            String action = mainObject.getString("action");
            JSONObject data = mainObject.getJSONObject("data");

            if ("create_product".equals(action)) {
                String qrProductName = data.optString("name", "Unknown Product");
                double qrCostPrice = data.optDouble("cost_price", 0.0);
                
                int originalQrId = data.has("id") ? data.getInt("id") : 0;
                boolean isNewProduct = (originalQrId == 0);
                int targetProductId = originalQrId;
                
                String resolvedCategoryName = "Processing...";

                if (addUpdateModeRadio.isSelected()) {
                    if (currentDetailsDialog != null && currentDetailsDialog.isVisible()) {
                        currentDetailsDialog.dispose();
                    }

                    // 1. Save to DB
                    int savedId = productRepository.upsertProductFromQR(data);
                    
                    if (savedId > 0) {
                        loadProductsFromDB();
                        findAndSelectProduct(String.valueOf(savedId));
                        targetProductId = savedId;
                        
                        // 2. Fetch the ACTUAL category name from DB
                        try {
                            String dbCat = productRepository.getProductCategoryName(savedId);
                            if (dbCat != null && !dbCat.equals("Unknown")) {
                                resolvedCategoryName = dbCat;
                            }
                        } catch (Exception ex) {
                            resolvedCategoryName = "Unknown";
                        }
                    }

                    if (originalQrId == 0) isNewProduct = true;
                    String dialogTitle = isNewProduct ? "Product Added" : "Product Updated";

                    showProductDetailsDialog(
                        dialogTitle, targetProductId, qrProductName, resolvedCategoryName, qrCostPrice, null, UIConstants.SUCCESS_COLOR
                    );
                    
                } else {
                    // SELL / DELETE MODE
                    Object[] tableProduct = getProductFromTable(targetProductId, qrProductName);

                    if (tableProduct != null) {
                        int confirmedId = (int) tableProduct[0];
                        String confirmedName = (String) tableProduct[1];
                        String confirmedCategory = (String) tableProduct[2];
                        double confirmedCost = (double) tableProduct[3];

                        if (sellModeRadio.isSelected()) {
                            if (currentDetailsDialog != null && currentDetailsDialog.isVisible()) currentDetailsDialog.dispose();
                            
                            String retailPriceStr = "N/A";
                            try {
                                double rp = productRepository.getProductRetailPrice(String.valueOf(confirmedId));
                                if (rp <= 0) {
                                    Double m = productRepository.getProductMarkup(confirmedId);
                                    double mv = (m != null) ? m : userRepository.getDefaultMarkup(userId);
                                    rp = confirmedCost * (1 + mv / 100.0);
                                }
                                retailPriceStr = String.format("₱%,.2f", rp);
                            } catch (Exception ex) {}

                            productRepository.sellProduct(String.valueOf(confirmedId), 1);
                            loadProductsFromDB();
                            findAndSelectProduct(String.valueOf(confirmedId));

                            showProductDetailsDialog("Product Sold", confirmedId, confirmedName, confirmedCategory, confirmedCost, retailPriceStr, UIConstants.SUCCESS_COLOR);
                        } else if (deleteModeRadio.isSelected()) {
                            boolean confirmed = showQRDeleteDialog(confirmedId, confirmedName, confirmedCategory, confirmedCost);
                            if (confirmed) {
                                productRepository.deleteProduct(confirmedId);
                                showSuccess("Product deleted.");
                                loadProductsFromDB();
                            }
                        }
                    } else {
                        showError("Product not found in inventory. Please add it first.");
                    }
                }
            } else {
                showError("Unknown QR action: " + action);
            }
        } catch (Throwable e) {
            // Catches all errors (including NoSuchMethodError) to prevent silent crashes
            showError("Scanning Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Helper to find a product in the JTable model by ID or Name.
     * Returns Object array: {ID (int), Name (String), Category (String), Cost (double), Stock (int)}
     * Returns null if not found.
     */
    private Object[] getProductFromTable(int searchId, String searchName) {
        // 1. Try to find by ID first
        if (searchId > 0) {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                int idInTable = (int) tableModel.getValueAt(i, 1); // Col 1: ID
                if (idInTable == searchId) {
                    return extractRowData(i);
                }
            }
        }

        // 2. Fallback: Try to find by Name
        if (searchName != null && !searchName.isEmpty()) {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String nameInTable = (String) tableModel.getValueAt(i, 2); // Col 2: Name
                if (nameInTable != null && nameInTable.equalsIgnoreCase(searchName)) {
                    return extractRowData(i);
                }
            }
        }

        return null; // Not found
    }

    /**
     * Helper to extract clean data from a table row.
     */
    private Object[] extractRowData(int row) {
        int id = (int) tableModel.getValueAt(row, 1);
        String name = (String) tableModel.getValueAt(row, 2);
        String category = (String) tableModel.getValueAt(row, 3);
        
        // Parse Cost Price (remove ₱ and ,)
        String costStr = tableModel.getValueAt(row, 4).toString().replace("₱", "").replace(",", "").trim();
        double cost = 0.0;
        try { cost = Double.parseDouble(costStr); } catch (NumberFormatException e) {}

        int stock = (int) tableModel.getValueAt(row, 9);

        return new Object[]{id, name, category, cost, stock};
    }

    /**
     * Shows a clean "Receipt Style" details dialog without emojis or stock counts.
     */
    private void showProductDetailsDialog(String title, int productId, String productName, 
                                          String categoryName, double costPrice,
                                          String retailPrice, Color headerColor) {
        
        currentDetailsDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), title, false);
        currentDetailsDialog.setLayout(new BorderLayout());
        currentDetailsDialog.setSize(400, 400);
        currentDetailsDialog.setLocationRelativeTo(this);
        currentDetailsDialog.setResizable(false);
        currentDetailsDialog.setAlwaysOnTop(true); // Fixes dialog hiding behind other windows
        
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(headerColor);
        headerPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        
        currentDetailsDialog.add(headerPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(new EmptyBorder(20, 30, 20, 30));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 10, 0); 
        
        int row = 0;
        String safeName = (productName != null) ? productName : "-";
        String safeCategory = (categoryName != null) ? categoryName : "-";

        addStyledDetailRow(contentPanel, gbc, row++, "Product Name", safeName);
        addStyledDetailRow(contentPanel, gbc, row++, "Category", safeCategory);
        addStyledDetailRow(contentPanel, gbc, row++, "Product ID", String.valueOf(productId));
        
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(230, 230, 230));
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        contentPanel.add(sep, gbc);
        gbc.gridwidth = 1;

        if (retailPrice != null) {
            addStyledDetailRow(contentPanel, gbc, row++, "Selling Price", retailPrice);
        } else {
            addStyledDetailRow(contentPanel, gbc, row++, "Cost Price", String.format("₱%,.2f", costPrice));
        }

        currentDetailsDialog.add(contentPanel, BorderLayout.CENTER);

        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footerPanel.setBackground(Color.WHITE);
        footerPanel.setBorder(new EmptyBorder(0, 0, 20, 0));
        
        JButton closeButton = new JButton("OK");
        closeButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        closeButton.setPreferredSize(new Dimension(140, 40));
        closeButton.setBackground(headerColor);
        closeButton.setForeground(Color.WHITE);
        closeButton.setFocusPainted(false);
        closeButton.setBorderPainted(false);
        closeButton.addActionListener(e -> currentDetailsDialog.dispose());
        
        footerPanel.add(closeButton);
        currentDetailsDialog.add(footerPanel, BorderLayout.SOUTH);

        currentDetailsDialog.setVisible(true);
    }

    /**
     * Helper to add a clean 2-column row: "Label" (Left) ..... "Value" (Right)
     */
    private void addStyledDetailRow(JPanel panel, GridBagConstraints gbc, int row, String label, String value) {
        gbc.gridy = row;
        
        // Label Column (Left)
        gbc.gridx = 0;
        gbc.weightx = 0.4;
        gbc.anchor = GridBagConstraints.WEST;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lbl.setForeground(Color.GRAY);
        panel.add(lbl, gbc);
        
        // Value Column (Right)
        gbc.gridx = 1;
        gbc.weightx = 0.6;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel val = new JLabel(value != null ? value : "-");
        val.setFont(new Font("Segoe UI", Font.BOLD, 15));
        val.setForeground(Color.BLACK);
        val.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(val, gbc);
    }

    /**
     * Shows a clean modal confirmation dialog for QR deletion without emojis.
     */
    private boolean showQRDeleteDialog(int productId, String productName, String categoryName, double costPrice) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Confirm Delete", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(400, 420);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);
        dialog.setAlwaysOnTop(true);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UIConstants.DANGER_COLOR);
        headerPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel titleLabel = new JLabel("Delete Product?", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        
        dialog.add(headerPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(new EmptyBorder(20, 30, 20, 30));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 10, 0);
        
        int row = 0;
        JLabel warning = new JLabel("<html><center>This action cannot be undone.</center></html>", SwingConstants.CENTER);
        warning.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        warning.setForeground(Color.GRAY);
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        contentPanel.add(warning, gbc);
        gbc.gridwidth = 1;

        gbc.gridy = row++;
        contentPanel.add(Box.createVerticalStrut(10), gbc);

        addStyledDetailRow(contentPanel, gbc, row++, "Product Name", productName);
        addStyledDetailRow(contentPanel, gbc, row++, "Category", categoryName);
        addStyledDetailRow(contentPanel, gbc, row++, "Product ID", String.valueOf(productId));
        
        dialog.add(contentPanel, BorderLayout.CENTER);

        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        footerPanel.setBackground(Color.WHITE);
        footerPanel.setBorder(new EmptyBorder(0, 0, 25, 0));
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        cancelButton.setPreferredSize(new Dimension(110, 40));
        cancelButton.setBackground(Color.LIGHT_GRAY);
        cancelButton.setForeground(Color.BLACK);
        cancelButton.setBorderPainted(false);
        cancelButton.setFocusPainted(false);
        cancelButton.addActionListener(e -> dialog.dispose());
        
        JButton deleteButton = new JButton("Delete");
        deleteButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        deleteButton.setPreferredSize(new Dimension(110, 40));
        deleteButton.setBackground(UIConstants.DANGER_COLOR);
        deleteButton.setForeground(Color.WHITE);
        deleteButton.setBorderPainted(false);
        deleteButton.setFocusPainted(false);
        
        final boolean[] result = {false};
        deleteButton.addActionListener(e -> {
            result[0] = true;
            dialog.dispose();
        });
        
        footerPanel.add(cancelButton);
        footerPanel.add(deleteButton);
        dialog.add(footerPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
        return result[0];
    }

    // --- Scanner Control Methods ---

    /** Toggles the webcam scanner on or off. */
    private void toggleScanner() {
        if (!isScanning.get()) {
            startScanner();
        } else {
            stopScanner();
        }
    }

    /** Starts the webcam discovery and scanning process. */
    private void startScanner() {
        if (isScanning.get()) {
            showError("Scanner is already running.");
            return;
        }
        toggleScanButton.setEnabled(false); toggleScanButton.setText("Starting...");
        addUpdateModeRadio.setEnabled(false);
        sellModeRadio.setEnabled(false);
        deleteModeRadio.setEnabled(false);

        executor.execute(() -> {
            Webcam discoveredWebcam = null;
            WebcamPanel createdWebcamPanel = null;
            boolean initSuccess = false;
            try {
                discoveredWebcam = Webcam.getDefault();
                if (discoveredWebcam == null) throw new WebcamException("No webcam found.");
                Dimension size = WebcamResolution.QVGA.getSize();
                boolean supported = false;
                for(Dimension d : discoveredWebcam.getViewSizes()){ if(d.equals(size)){supported=true; break;}}
                if(!supported) { size = discoveredWebcam.getViewSize(); }
                discoveredWebcam.setViewSize(size);
                discoveredWebcam.open(true);
                createdWebcamPanel = new WebcamPanel(discoveredWebcam);
                createdWebcamPanel.setPreferredSize(size);
                createdWebcamPanel.setMirrored(false);
                initSuccess = true;
            } catch (Exception ex) {
                final String errorMessage = (ex instanceof WebcamException) ?
                    ("Failed to start webcam: " + ex.getMessage()) : "Webcam may be already in use or locked.";
                SwingUtilities.invokeLater(() -> handleScannerStartError(errorMessage));
            } finally {
                if (!initSuccess && discoveredWebcam != null && discoveredWebcam.isOpen()) {
                    try { discoveredWebcam.close(); } catch (Exception e) { }
                }
                final boolean finalSuccess = initSuccess;
                final WebcamPanel finalPanel = createdWebcamPanel;
                final Webcam finalWebcam = discoveredWebcam;
                SwingUtilities.invokeLater(() -> {
                    if (finalSuccess) {
                        webcam = finalWebcam; webcamPanel = finalPanel;
                        isScanning.set(true);
                        scannerDisplayPanel.removeAll();
                        scannerDisplayPanel.add(webcamPanel, BorderLayout.CENTER);
                        scannerDisplayPanel.revalidate(); scannerDisplayPanel.repaint();
                        toggleScanButton.setText("Stop Scan"); toggleScanButton.setEnabled(true);
                        executor.execute(this::scanLoop);
                    } else {
                        isScanning.set(false);
                        toggleScanButton.setEnabled(true); toggleScanButton.setText("Start Scan");
                        // Only re-enable buttons for Admin role
                        if (userRole.equalsIgnoreCase("Admin")) {
                            addUpdateModeRadio.setEnabled(true);
                            deleteModeRadio.setEnabled(true);
                        }
                        sellModeRadio.setEnabled(true);
                    }
                });
            }
        });
    }

    /** Stops the scanning loop and closes the webcam. */
    private void stopScanner() {
        if (!isScanning.compareAndSet(true, false)) { return; }
        toggleScanButton.setEnabled(false); toggleScanButton.setText("Stopping...");

        executor.execute(() -> {
            WebcamPanel panelToStop = this.webcamPanel; Webcam camToClose = this.webcam;
            if (panelToStop != null) try { panelToStop.stop(); } catch (Exception e) { e.printStackTrace(); }
            if (camToClose != null && camToClose.isOpen()) try { camToClose.close(); } catch (Exception e) { e.printStackTrace(); }
            this.webcamPanel = null; this.webcam = null;

            SwingUtilities.invokeLater(() -> {
                toggleScanButton.setText("Start Scan"); toggleScanButton.setEnabled(true);
                // Only re-enable buttons for Admin role
                if (userRole.equalsIgnoreCase("Admin")) {
                    addUpdateModeRadio.setEnabled(true);
                    deleteModeRadio.setEnabled(true);
                }
                sellModeRadio.setEnabled(true);
                if (scannerDisplayPanel != null) {
                    scannerDisplayPanel.removeAll();
                    scannerDisplayPanel.add(new JLabel("Scanner Off", SwingConstants.CENTER), BorderLayout.CENTER);
                    scannerDisplayPanel.revalidate(); scannerDisplayPanel.repaint();
                }
            });
        });
    }

    /** Handles errors during scanner startup, updating the UI. */
    private void handleScannerStartError(String errorMessage) {
        showError(errorMessage);
        isScanning.set(false); webcam = null; webcamPanel = null;
        toggleScanButton.setEnabled(true); toggleScanButton.setText("Start Scan");
        // Only re-enable buttons for Admin role
        if (userRole.equalsIgnoreCase("Admin")) {
            addUpdateModeRadio.setEnabled(true);
            deleteModeRadio.setEnabled(true);
        }
        sellModeRadio.setEnabled(true);
        scannerDisplayPanel.removeAll();
        scannerDisplayPanel.add(new JLabel("Scanner Error", SwingConstants.CENTER), BorderLayout.CENTER);
        scannerDisplayPanel.revalidate(); scannerDisplayPanel.repaint();
    }

    /**
     * Main scanning loop that captures images and decodes QR codes.
     */
    private void scanLoop() {
        do {
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            Result result = null; BufferedImage image = null; Webcam currentWebcam = this.webcam;
            if (currentWebcam == null || !currentWebcam.isOpen()) { break; }
            try { image = currentWebcam.getImage(); } catch (Exception e) { System.err.println("scanLoop: Img Err: " + e.getMessage()); continue; }
            if (image != null) {
                LuminanceSource source = new BufferedImageLuminanceSource(image);
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                try { result = new MultiFormatReader().decode(bitmap); } catch (NotFoundException e) { } catch (Exception e) { System.err.println("scanLoop: Decode Err: " + e.getMessage()); }
            }
            if (result != null && isScanning.get()) {
                SoundUtil.play("beep.wav"); 
                final String qrText = result.getText();
                SwingUtilities.invokeLater(() -> { if (isScanning.get()) { processQRCodeCommand(qrText); } });
                try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            }
        } while (isScanning.get());
        if (isScanning.get()) { SwingUtilities.invokeLater(this::stopScanner); }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    /** NEW METHOD: Handles setting product-specific markup percentage. */
    private void handleSetProductMarkup() {
        if (userRole.equalsIgnoreCase("Employee")) {
            showError("Access Denied: Only Admins can set product markup.");
            return;
        }

        int[] selectedRows = productsTable.getSelectedRows();
        if (selectedRows.length == 0) {
            showError("Please select at least one product from the table first.");
            return;
        }

        if (selectedRows.length == 1) {
            // Single product markup setting
            try {
                int productId = (int) tableModel.getValueAt(selectedRows[0], 1); // ID column
                String productName = (String) tableModel.getValueAt(selectedRows[0], 2); // Product Name column
                // Get cost price - remove formatting
                String costPriceStr = tableModel.getValueAt(selectedRows[0], 4).toString(); // Cost Price column
                double costPrice = Double.parseDouble(costPriceStr.replace("₱", "").replace(",", "").trim());
                // Get current markup if exists
                Double currentMarkup = productRepository.getProductMarkup(productId);
                double defaultMarkup = userRepository.getDefaultMarkup(userId);
                // Create markup dialog
                JDialog markupDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Set Product Markup", true);
                markupDialog.setLayout(new BorderLayout());
                markupDialog.setSize(500, 450);
                markupDialog.setLocationRelativeTo(this);
                markupDialog.setResizable(false);

                // Header panel
                JPanel headerPanel = new JPanel(new BorderLayout());
                headerPanel.setBackground(UIConstants.PRIMARY_COLOR);
                headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

                JLabel headerLabel = new JLabel("Set Markup: " + productName, SwingConstants.CENTER);
                headerLabel.setFont(UIConstants.SUBTITLE_FONT);
                headerLabel.setForeground(Color.WHITE);
                headerPanel.add(headerLabel);
                markupDialog.add(headerPanel, BorderLayout.NORTH);

                // Main content panel
                JPanel mainPanel = new JPanel(new GridBagLayout());
                mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
                mainPanel.setBackground(UIConstants.BACKGROUND_COLOR);
                
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(8, 5, 8, 5);
                gbc.weightx = 1.0;
                
                // Product Name
                gbc.gridx = 0;
                gbc.gridy = 0;
                JLabel productLabel = new JLabel("Product:");
                productLabel.setFont(UIConstants.LABEL_BOLD_FONT);
                mainPanel.add(productLabel, gbc);
                
                gbc.gridx = 1;
                JLabel productValueLabel = new JLabel(productName);
                productValueLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
                productValueLabel.setForeground(UIConstants.PRIMARY_COLOR);
                mainPanel.add(productValueLabel, gbc);

                // Cost Price
                gbc.gridx = 0;
                gbc.gridy = 1;
                JLabel costLabel = new JLabel("Cost Price:");
                costLabel.setFont(UIConstants.LABEL_BOLD_FONT);
                mainPanel.add(costLabel, gbc);
                
                gbc.gridx = 1;
                JLabel costValueLabel = new JLabel(String.format("₱%,.2f", costPrice));
                costValueLabel.setFont(UIConstants.LABEL_FONT);
                mainPanel.add(costValueLabel, gbc);

                // Current Markup
                gbc.gridx = 0;
                gbc.gridy = 2;
                JLabel currentLabel = new JLabel("Current Markup:");
                currentLabel.setFont(UIConstants.LABEL_BOLD_FONT);
                mainPanel.add(currentLabel, gbc);
                
                gbc.gridx = 1;
                String currentMarkupText = currentMarkup != null ? 
                    String.format("%.1f%% (Product-specific)", currentMarkup) :
                    String.format("%.1f%% (Using default)", defaultMarkup);
                JLabel currentValueLabel = new JLabel(currentMarkupText);
                currentValueLabel.setFont(UIConstants.LABEL_FONT);
                currentValueLabel.setForeground(UIConstants.TEXT_SECONDARY);
                mainPanel.add(currentValueLabel, gbc);

                // New Markup
                gbc.gridx = 0;
                gbc.gridy = 3;
                JLabel newMarkupLabel = new JLabel("New Markup %:");
                newMarkupLabel.setFont(UIConstants.LABEL_BOLD_FONT);
                mainPanel.add(newMarkupLabel, gbc);
                
                gbc.gridx = 1;
                JTextField markupField = new JTextField(currentMarkup != null ? String.valueOf(currentMarkup) : "");
                markupField.setFont(UIConstants.INPUT_FONT);
                markupField.setPreferredSize(new Dimension(0, UIConstants.INPUT_HEIGHT));
                mainPanel.add(markupField, gbc);

                // Helper text
                gbc.gridx = 0;
                gbc.gridy = 4;
                gbc.gridwidth = 2;
                JLabel helperLabel = new JLabel(
                    "<html><i><font color='#7f8c8d' size='3'>Leave blank to use default markup</font></i></html>"
                );
                mainPanel.add(helperLabel, gbc);

                // Preview
                gbc.gridx = 0;
                gbc.gridy = 5;
                gbc.gridwidth = 1;
                JLabel previewTitleLabel = new JLabel("Selling Price Preview:");
                previewTitleLabel.setFont(UIConstants.LABEL_BOLD_FONT);
                mainPanel.add(previewTitleLabel, gbc);
                
                gbc.gridx = 1;
                JLabel previewLabel = new JLabel("");
                previewLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
                previewLabel.setForeground(new Color(46, 204, 113));
                
                // Initial preview
                if (currentMarkup != null) {
                    double sellingPrice = costPrice * (1 + currentMarkup / 100.0);
                    previewLabel.setText(String.format("₱%,.2f", sellingPrice));
                } else {
                    double sellingPrice = costPrice * (1 + defaultMarkup / 100.0);
                    previewLabel.setText(String.format("₱%,.2f (default)", sellingPrice));
                }
                mainPanel.add(previewLabel, gbc);
                
                // Update preview on text change
                markupField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                    public void changedUpdate(javax.swing.event.DocumentEvent e) { updatePreview(); }
                    public void removeUpdate(javax.swing.event.DocumentEvent e) { updatePreview(); }
                    public void insertUpdate(javax.swing.event.DocumentEvent e) { updatePreview(); }
                    
                    public void updatePreview() {
                        try {
                            String text = markupField.getText().trim();
                            double markup = text.isEmpty() ? defaultMarkup : Double.parseDouble(text);
                            double sellingPrice = costPrice * (1 + markup / 100.0);
                            previewLabel.setText(String.format("₱%,.2f", sellingPrice));
                            previewLabel.setForeground(new Color(46, 204, 113));
                        } catch (NumberFormatException ex) {
                            previewLabel.setText("Invalid input");
                            previewLabel.setForeground(Color.RED);
                        }
                    }
                });

                markupDialog.add(mainPanel, BorderLayout.CENTER);

                // Button panel
                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
                buttonPanel.setBackground(UIConstants.BACKGROUND_COLOR);
                
                JButton cancelButton = new JButton("Cancel");
                cancelButton.setFont(UIConstants.BUTTON_FONT);
                cancelButton.setPreferredSize(new Dimension(100, 35));
                cancelButton.addActionListener(e -> markupDialog.dispose());
                
                JButton applyButton = new JButton("Apply");
                applyButton.setFont(UIConstants.BUTTON_FONT);
                applyButton.setBackground(UIConstants.PRIMARY_COLOR);
                applyButton.setForeground(Color.WHITE);
                applyButton.setPreferredSize(new Dimension(100, 35));
                applyButton.setFocusPainted(false);
                applyButton.setBorderPainted(false);
                applyButton.setContentAreaFilled(false);
                applyButton.setOpaque(true);
                applyButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
                applyButton.addActionListener(e -> {
                    try {
                        String markupText = markupField.getText().trim();
                        Double newMarkup = markupText.isEmpty() ? null : Double.parseDouble(markupText);
                        
                        if (newMarkup != null && newMarkup < 0) {
                            showError("Markup percentage cannot be negative.");
                            return;
                        }
                        
                        // Clear retail price when setting markup to ensure markup is used
                        productRepository.updateProductMarkup(productId, newMarkup);
                        
                        // Also clear any stored retail price to ensure markup calculation is used
                        productRepository.clearRetailPrice(productId);
                        
                        String message = newMarkup != null ? 
                            String.format("Markup set to %.1f%% for %s!", newMarkup, productName) :
                            String.format("Markup cleared for %s! Using default (%.1f%%).", productName, defaultMarkup);
                            
                        showSuccess(message);
                        loadProductsFromDB();
                        findAndSelectProduct(String.valueOf(productId));
                        markupDialog.dispose();
                    } catch (NumberFormatException ex) {
                        showError("Invalid markup percentage entered.");
                    } catch (SQLException ex) {
                        showError("Error setting markup: " + ex.getMessage());
                    }
                });
                
                buttonPanel.add(cancelButton);
                buttonPanel.add(applyButton);
                markupDialog.add(buttonPanel, BorderLayout.SOUTH);
                
                markupDialog.setVisible(true);
                
            } catch (NumberFormatException ex) {
                showError("Invalid product data.");
            } catch (SQLException ex) {
                showError("Error loading markup data: " + ex.getMessage());
            }
        } else {
            // Multiple products markup setting
            showBulkOperationDialog("SET_MARKUP", selectedRows);
        }
    }

    /**
     * Generates QR code from selected product in table
     */
    private void handleGenerateQRCode() {
        if (userRole.equalsIgnoreCase("Employee")) {
            showError("Access Denied: Only Admins can generate QR codes.");
            return;
        }

        int selectedRow = productsTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("Please select a product from the table first.");
            return;
        }

        try {
            // Get product data from table (corrected indices)
            int productId = (int) tableModel.getValueAt(selectedRow, 1); // ID column
            String productName = (String) tableModel.getValueAt(selectedRow, 2); // Product Name column
            String categoryName = (String) tableModel.getValueAt(selectedRow, 3); // Category column
            String costPriceStr = tableModel.getValueAt(selectedRow, 4).toString(); // Cost Price column
            double costPrice = Double.parseDouble(costPriceStr.replace("₱", "").replace(",", "").trim());
            int stock = (int) tableModel.getValueAt(selectedRow, 9); // Stock column

            // Get category_id from database
            List<Category> categories = productRepository.getCategories();
            int categoryId = 1; // Default
            for (Category cat : categories) {
                if (cat.name().equals(categoryName)) {
                    categoryId = cat.id();
                    break;
                }
            }

            // Build JSON for QR code (matching QRCodePanel format)
            JSONObject qrData = new JSONObject();
            qrData.put("action", "create_product");

            JSONObject productData = new JSONObject();
            productData.put("id", productId);
            productData.put("name", productName);
            productData.put("category_id", categoryId);
            productData.put("cost_price", costPrice);
            productData.put("stock", stock);

            qrData.put("data", productData);
            String qrText = qrData.toString();

            // Generate QR code image
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            final int qrCodeSize = 280; // Match QRCodePanel size
            BitMatrix bitMatrix = qrCodeWriter.encode(qrText, BarcodeFormat.QR_CODE, qrCodeSize, qrCodeSize);
            BufferedImage qrImage = toBufferedImage(bitMatrix);

            // Add product details below QR code
            int textHeight = 80;
            int combinedHeight = qrCodeSize + textHeight;
            BufferedImage combinedImage = new BufferedImage(qrCodeSize, combinedHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = combinedImage.createGraphics();

            // Background
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, qrCodeSize, combinedHeight);

            // Draw QR code
            g2d.drawImage(qrImage, 0, 0, null);

            // Draw product info text - MATCH QRCodePanel EXACTLY
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Monospaced", Font.PLAIN, 12)); // Changed from BOLD to PLAIN
            FontMetrics fm = g2d.getFontMetrics();
            int lineHeight = fm.getHeight();
            int startY = qrCodeSize + 20; // Match QRCodePanel padding
            int currentY = startY;
            int paddingX = 10;

            // Use same format as QRCodePanel with label:value pairs
            drawDetailLine(g2d, "Name:", productName, paddingX, currentY);
            currentY += lineHeight;
            drawDetailLine(g2d, "Category:", categoryName, paddingX, currentY);
            currentY += lineHeight;
            drawDetailLine(g2d, "Cost:", "₱" + String.format("%,.2f", costPrice), paddingX, currentY);

            g2d.dispose();
            
            // Display in dialog with save option
            displayQRCodeDialog(combinedImage, productName, productId);
            
        } catch (NumberFormatException ex) {
            showError("Error parsing product data: " + ex.getMessage());
        } catch (WriterException ex) {
            showError("Could not generate QR code: " + ex.getMessage());
        } catch (SQLException ex) {
            showError("Error loading categories: " + ex.getMessage());
        } catch (JSONException ex) {
            showError("Error building QR data: " + ex.getMessage());
        }
    }
    
    /** Helper method to convert BitMatrix to BufferedImage */
    private BufferedImage toBufferedImage(BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        return image;
    }
    
    /** Helper method to draw one line of product info with bold label (matches QRCodePanel) */
    private void drawDetailLine(Graphics2D g2d, String label, String value, int x, int y) {
        AttributedString asLabel = new AttributedString(String.format("%-10s", label));
        asLabel.addAttribute(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD, 0, label.length());
        g2d.drawString(asLabel.getIterator(), x, y);

        FontMetrics fm = g2d.getFontMetrics();
        Rectangle2D labelBounds = fm.getStringBounds(String.format("%-10s", label), g2d);
        g2d.drawString(value, x + (int)labelBounds.getWidth() + 5, y);
    }
    
    /** Display generated QR code in a dialog with save option */
    private void displayQRCodeDialog(BufferedImage qrImage, String productName, int productId) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "QR Code - " + productName, true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(450, 550);
        dialog.setLocationRelativeTo(mainFrame);
        dialog.setResizable(false);

        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UIConstants.SUCCESS_COLOR);
        headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel headerLabel = new JLabel("QR Code: " + productName, SwingConstants.CENTER);
        headerLabel.setFont(UIConstants.SUBTITLE_FONT);
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel);
        dialog.add(headerPanel, BorderLayout.NORTH);

        // Main content panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        
        // QR Image
        JLabel imageLabel = new JLabel(new ImageIcon(qrImage));
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(imageLabel, BorderLayout.CENTER);
        
        dialog.add(mainPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        
        JButton cancelButton = new JButton("Close");
        cancelButton.setFont(UIConstants.BUTTON_FONT);
        cancelButton.setPreferredSize(new Dimension(100, 35));
        cancelButton.addActionListener(e -> dialog.dispose());
        
        JButton saveButton = new JButton("Save QR Code");
        saveButton.setFont(UIConstants.BUTTON_FONT);
        saveButton.setBackground(UIConstants.SUCCESS_COLOR);
        saveButton.setForeground(Color.WHITE);
        saveButton.setPreferredSize(new Dimension(130, 35));
        saveButton.setFocusPainted(false);
        saveButton.setBorderPainted(false);
        saveButton.setContentAreaFilled(false);
        saveButton.setOpaque(true);
        saveButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        saveButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save QR Code As");
            
            // Use same naming convention as QRCodePanel
            String sanitizedName = productName.trim().replaceAll("[^a-zA-Z0-9.-]", "_");
            String defaultFileName = sanitizedName.isEmpty() 
                ? "PRODUCT_ID_" + productId + ".png"
                : sanitizedName + "_ID_" + productId + ".png";
            
            fileChooser.setSelectedFile(new File(defaultFileName));
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PNG Images (*.png)", "png"));
            
            if (fileChooser.showSaveDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                try {
                    File outputFile = fileChooser.getSelectedFile();
                    if (!outputFile.getName().toLowerCase().endsWith(".png")) {
                        outputFile = new File(outputFile.getParentFile(), outputFile.getName() + ".png");
                    }
                    ImageIO.write(qrImage, "PNG", outputFile);
                    showSuccess("QR Code saved successfully to:\n" + outputFile.getAbsolutePath());
                } catch (IOException ex) {
                    showError("Failed to save QR code: " + ex.getMessage());
                }
            }
        });
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setVisible(true);
    }

    /**
     * NEW METHOD: Handles scanning QR codes from image files in products panel
     */
    private void handleScanQRFromImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select QR Code Image");
        fileChooser.setFileFilter(new FileNameExtensionFilter(
            "Image Files (PNG, JPG, JPEG, BMP, GIF)", "png", "jpg", "jpeg", "bmp", "gif"));

        int result = fileChooser.showOpenDialog(this);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File imageFile = fileChooser.getSelectedFile();
            
            try {
                // Read the image
                BufferedImage bufferedImage = ImageIO.read(imageFile);
                
                if (bufferedImage == null) {
                    showError("Could not read image file. Please select a valid image.");
                    return;
                }

                // Decode QR code from image
                LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                com.google.zxing.Result qrResult = new MultiFormatReader().decode(bitmap);

                String qrText = qrResult.getText();
                
                // Display scanned image in scanner panel
                ImageIcon scaledIcon = new ImageIcon(bufferedImage.getScaledInstance(
                    280, 240, Image.SCALE_SMOOTH));
                scannerDisplayPanel.removeAll();
                JLabel imageLabel = new JLabel(scaledIcon);
                imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
                scannerDisplayPanel.add(imageLabel, BorderLayout.CENTER);
                scannerDisplayPanel.revalidate();
                scannerDisplayPanel.repaint();

                // Process the QR code based on mode
                processQRCodeCommand(qrText);

            } catch (NotFoundException e) {
                showError("No QR code found in the selected image.\nPlease select an image containing a QR code.");
            } catch (IOException e) {
                showError("Error reading image file: " + e.getMessage());
            } catch (Exception e) {
                showError("Error processing QR code: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

// Bulk operation dialog with individual product editing
    private void showBulkOperationDialog(String operationType, int[] selectedRows) {
        SwingUtilities.invokeLater(() -> {
            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), getDialogTitle(operationType), true);
            dialog.setSize(950, 650);
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new BorderLayout());
            dialog.getContentPane().setBackground(UIConstants.BACKGROUND_COLOR);

            // --- Header ---
            JPanel headerPanel = new JPanel(new BorderLayout());
            headerPanel.setBackground(UIConstants.PRIMARY_COLOR);
            headerPanel.setBorder(new EmptyBorder(15, 25, 15, 25));
            
            JLabel titleLabel = new JLabel(getDialogTitle(operationType));
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
            titleLabel.setForeground(Color.WHITE);
            headerPanel.add(titleLabel, BorderLayout.WEST);
            
            dialog.add(headerPanel, BorderLayout.NORTH);

            // --- Data Setup ---
            String[] columns = getTableColumns(operationType);
            Object[][] data = new Object[selectedRows.length][columns.length];
            for (int i = 0; i < selectedRows.length; i++) {
                data[i] = getRowData(operationType, selectedRows[i]);
            }

            // --- Table Model ---
            DefaultTableModel dialogTableModel = new DefaultTableModel(data, columns) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    if (column == 0 || column == 2 || column == 3) return false; // #, ID, Name fixed
                    if (column == 1) return true; // Checkbox editable
                    
                    if (operationType.equals("DELETE")) return false; // Nothing else editable in Delete mode
                    
                    if (operationType.equals("RETURN")) return column >= 4;
                    if (operationType.equals("REMOVE_STOCK")) return column >= 4;
                    return column == getColumnCount() - 1; 
                }
                
                @Override
                public Class<?> getColumnClass(int columnIndex) {
                    if (columnIndex == 1) return Boolean.class;
                    return String.class;
                }
            };

            // --- Table Configuration ---
            JTable dialogTable = new JTable(dialogTableModel);
            dialogTable.setFont(UIConstants.TABLE_FONT);
            dialogTable.setRowHeight(40);
            dialogTable.setShowVerticalLines(false);
            dialogTable.setShowHorizontalLines(true);
            dialogTable.setGridColor(UIConstants.BORDER_COLOR);
            dialogTable.setIntercellSpacing(new Dimension(0, 0)); 
            dialogTable.setSelectionBackground(new Color(235, 245, 255)); 
            dialogTable.setSelectionForeground(Color.BLACK);
            dialogTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            
            JTableHeader header = dialogTable.getTableHeader();
            header.setFont(UIConstants.TABLE_HEADER_FONT);
            header.setBackground(Color.WHITE);
            header.setForeground(UIConstants.TEXT_PRIMARY);
            header.setPreferredSize(new Dimension(0, 40));
            header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, UIConstants.PRIMARY_COLOR));

            // Hide # Column
            dialogTable.getColumnModel().getColumn(0).setMinWidth(0);
            dialogTable.getColumnModel().getColumn(0).setMaxWidth(0);
            dialogTable.getColumnModel().getColumn(0).setWidth(0);
            
            dialogTable.getColumnModel().getColumn(1).setPreferredWidth(50);  // Checkbox
            dialogTable.getColumnModel().getColumn(1).setMaxWidth(50);
            dialogTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // ID
            dialogTable.getColumnModel().getColumn(3).setPreferredWidth(250); // Name

            // --- Editors ---
            JTextField editorField = new JTextField();
            editorField.setFont(UIConstants.INPUT_FONT);
            editorField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UIConstants.PRIMARY_COLOR, 1), 
                new EmptyBorder(0, 5, 0, 5)
            ));
            
            DefaultCellEditor styledEditor = new DefaultCellEditor(editorField);
            
            if (operationType.equals("RETURN")) {
                JComboBox<String> returnTypeCombo = new JComboBox<>(new String[]{
                    "Customer Return", "Reject/Damaged", "Refund to Supplier"
                });
                returnTypeCombo.setFont(UIConstants.INPUT_FONT);
                dialogTable.getColumnModel().getColumn(5).setCellEditor(new DefaultCellEditor(returnTypeCombo));
                dialogTable.getColumnModel().getColumn(4).setCellEditor(styledEditor);
                dialogTable.getColumnModel().getColumn(6).setCellEditor(styledEditor);
            } else if (operationType.equals("REMOVE_STOCK")) {
                dialogTable.getColumnModel().getColumn(4).setCellEditor(styledEditor);
                dialogTable.getColumnModel().getColumn(5).setCellEditor(styledEditor);
            } else if (!operationType.equals("DELETE")) {
                dialogTable.getColumnModel().getColumn(dialogTable.getColumnCount() - 1).setCellEditor(styledEditor);
            }

            // --- Renderer ---
            dialogTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
                final Font idFont = new Font("Monospaced", Font.BOLD, 12);
                final Color readOnlyBg = new Color(250, 250, 250); 
                final Color inputBg = Color.WHITE;
                
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value,
                        boolean isSelected, boolean hasFocus, int row, int column) {
                    
                    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    
                    // Text
                    if (column == 2) { 
                        setHorizontalAlignment(SwingConstants.CENTER);
                        setFont(idFont);
                        setForeground(Color.GRAY);
                    } else {
                        setHorizontalAlignment(SwingConstants.LEFT);
                        setFont(UIConstants.TABLE_FONT);
                        setForeground(UIConstants.TEXT_PRIMARY);
                    }

                    // Background & Borders
                    boolean isEditable = table.isCellEditable(row, column);
                    
                    if (column == 1) { 
                        c.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
                        ((JComponent)c).setBorder(null);
                    } else if (isEditable) {
                        c.setBackground(inputBg);
                        c.setForeground(Color.BLACK);
                        ((JComponent)c).setBorder(BorderFactory.createCompoundBorder(
                             new LineBorder(new Color(220, 220, 220)), 
                             new EmptyBorder(0, 5, 0, 5)
                        ));
                    } else {
                        c.setBackground(isSelected ? table.getSelectionBackground() : readOnlyBg);
                        ((JComponent)c).setBorder(new EmptyBorder(0, 5, 0, 5));
                    }
                    
                    return c;
                }
            });

            // --- Layout & Controls ---
            JPanel contentPanel = new JPanel(new BorderLayout(0, 10));
            contentPanel.setBackground(UIConstants.BACKGROUND_COLOR);
            contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

            JPanel controlsPanel = new JPanel(new BorderLayout());
            controlsPanel.setBackground(UIConstants.BACKGROUND_COLOR);
            
            JCheckBox selectAllBox = new JCheckBox("Select All");
            selectAllBox.setFont(UIConstants.LABEL_BOLD_FONT);
            selectAllBox.setBackground(UIConstants.BACKGROUND_COLOR);
            selectAllBox.setFocusPainted(false);
            selectAllBox.setSelected(true);
            selectAllBox.addActionListener(e -> {
                boolean selected = selectAllBox.isSelected();
                for (int i = 0; i < dialogTableModel.getRowCount(); i++) {
                    dialogTableModel.setValueAt(selected, i, 1);
                }
            });
            
            JLabel infoLabel = new JLabel("<html>" + getInfoText(operationType, selectedRows.length) + "</html>");
            infoLabel.setFont(UIConstants.LABEL_FONT);
            infoLabel.setForeground(UIConstants.TEXT_SECONDARY);
            
            controlsPanel.add(selectAllBox, BorderLayout.WEST);
            controlsPanel.add(infoLabel, BorderLayout.EAST);
            contentPanel.add(controlsPanel, BorderLayout.NORTH);

            JScrollPane scrollPane = new JScrollPane(dialogTable);
            scrollPane.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR));
            scrollPane.getViewport().setBackground(Color.WHITE);
            contentPanel.add(scrollPane, BorderLayout.CENTER);

            // --- Buttons ---
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
            buttonPanel.setBackground(UIConstants.BACKGROUND_COLOR);

            JButton cancelButton = new JButton("Cancel");
            cancelButton.setFont(UIConstants.BUTTON_FONT);
            cancelButton.setPreferredSize(new Dimension(130, 40));
            cancelButton.setBackground(Color.WHITE);
            cancelButton.setForeground(Color.GRAY);
            cancelButton.setBorder(new LineBorder(Color.GRAY));
            cancelButton.setFocusPainted(false);
            cancelButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            cancelButton.addActionListener(e -> dialog.dispose());

            JButton confirmButton = new JButton(getConfirmButtonText(operationType));
            confirmButton.setFont(UIConstants.BUTTON_FONT);
            confirmButton.setPreferredSize(new Dimension(180, 40));
            confirmButton.setBackground(getConfirmButtonColor(operationType));
            confirmButton.setForeground(Color.WHITE);
            confirmButton.setFocusPainted(false);
            confirmButton.setBorderPainted(false);
            confirmButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            confirmButton.addActionListener(e -> {
                // 1. Check if ANY row is checked
                boolean hasSelection = false;
                for (int i = 0; i < dialogTableModel.getRowCount(); i++) {
                    Boolean isChecked = (Boolean) dialogTableModel.getValueAt(i, 1);
                    if (isChecked != null && isChecked) {
                        hasSelection = true;
                        break;
                    }
                }

                // 2. If nothing is selected, show warning and STOP
                if (!hasSelection) {
                    JOptionPane.showMessageDialog(dialog, 
                        "Please select at least one item to proceed.", 
                        "No Selection", 
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // 3. If selected, proceed with operation
                dialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                SwingUtilities.invokeLater(() -> {
                    processBulkOperation(operationType, dialogTableModel, dialog);
                    dialog.setCursor(Cursor.getDefaultCursor());
                });
            });

            buttonPanel.add(cancelButton);
            buttonPanel.add(confirmButton);
            contentPanel.add(buttonPanel, BorderLayout.SOUTH);

            dialog.add(contentPanel, BorderLayout.CENTER);
            dialog.setVisible(true);
        });
    }

    // Get dialog title based on operation
    private String getDialogTitle(String operationType) {
        switch (operationType) {
            case "SET_MARKUP": return "Set Markup - Bulk Operation";
            case "REMOVE_STOCK": return "Remove Stock - Bulk Operation";
            case "DELETE": return "Delete Products - Bulk Operation";
            case "RETURN": return "Return Products - Bulk Operation";
            default: return "Bulk Operation";
        }
    }

    // Get table columns based on operation
    private String[] getTableColumns(String operationType) {
        switch (operationType) {
            case "SET_MARKUP":
                return new String[]{"#", "☑", "ID", "Product Name", "New Markup %"};
            case "REMOVE_STOCK":
                return new String[]{"#", "☑", "ID", "Product Name", "Remove Qty", "Reason"};
            case "DELETE":
                return new String[]{"#", "☑", "ID", "Product Name"};
            case "RETURN":
                return new String[]{"#", "☑", "ID", "Product Name", "Return Qty", "Return Type", "Notes"};
            default:
                return new String[]{"#", "☑", "ID", "Product Name", "Value"};
        }
    }

    // Get row data for each operation type
    private Object[] getRowData(String operationType, int tableRow) {
        int productId = (int) tableModel.getValueAt(tableRow, 1); // ID is now in column 1
        String productName = (String) tableModel.getValueAt(tableRow, 2); // Name is now in column 2
        
        switch (operationType) {
            case "SET_MARKUP":
                return new Object[]{tableRow + 1, true, productId, productName, ""};
                
            case "REMOVE_STOCK":
                return new Object[]{tableRow + 1, true, productId, productName, 0, ""};
                
            case "DELETE":
                return new Object[]{tableRow + 1, true, productId, productName,};
                
            case "RETURN":
                return new Object[]{tableRow + 1, true, productId, productName, 0, "Customer Return", ""};
                
            default:
                return new Object[]{tableRow + 1, true, productId, productName, ""};
        }
    }

    // Get info text
    private String getInfoText(String operationType, int count) {
        switch (operationType) {
            case "SET_MARKUP": return "Edit markup % for each product individually";
            case "REMOVE_STOCK": return "Specify quantity to remove for each product";
            case "DELETE": return "Select products to delete permanently";
            case "RETURN": return "Specify return quantity and reason for each product";
            default: return count + " products selected";
        }
    }

    // Get confirm button text
    private String getConfirmButtonText(String operationType) {
        switch (operationType) {
            case "SET_MARKUP": return "Apply Markup";
            case "REMOVE_STOCK": return "Remove Stock";
            case "DELETE": return "Delete Selected";
            case "RETURN": return "Process Returns";
            default: return "Confirm";
        }
    }

    // Get confirm button color
    private Color getConfirmButtonColor(String operationType) {
        switch (operationType) {
            case "DELETE": return UIConstants.DANGER_COLOR;
            case "RETURN": return UIConstants.WARNING_COLOR;
            default: return UIConstants.PRIMARY_COLOR;
        }
    }

    // Process bulk operation
    private void processBulkOperation(String operationType, DefaultTableModel dialogModel, JDialog dialog) {
        int successCount = 0;
        int skipCount = 0;
        StringBuilder errors = new StringBuilder();

        for (int i = 0; i < dialogModel.getRowCount(); i++) {
            boolean selected = (Boolean) dialogModel.getValueAt(i, 1); // Checkbox is now column 1
            if (!selected) {
                skipCount++;
                continue;
            }

            int productId = (Integer) dialogModel.getValueAt(i, 2); // Product ID is now column 2
            String productName = (String) dialogModel.getValueAt(i, 3); // Product Name is now column 3

            try {
                switch (operationType) {
                    case "SET_MARKUP":
                        String markupStr = dialogModel.getValueAt(i, 4).toString().trim(); // Column 4
                        if (!markupStr.isEmpty()) {
                            double markup = Double.parseDouble(markupStr);
                            if (markup < 0) {
                                errors.append("- ").append(productName).append(": Negative markup\n");
                                skipCount++;
                                continue;
                            }
                            productRepository.updateProductMarkup(productId, markup);
                            productRepository.clearRetailPrice(productId);
                            successCount++;
                        } else {
                            skipCount++;
                        }
                        break;

                    case "REMOVE_STOCK":
                        String removeQtyStr = dialogModel.getValueAt(i, 4).toString().trim(); // Column 4
                        if (removeQtyStr.isEmpty()) {
                            skipCount++;
                            continue;
                        }
                        int removeQty = Integer.parseInt(removeQtyStr);
                        String removeReason = dialogModel.getValueAt(i, 5).toString().trim(); // Column 5
                        // Get current stock from main table
                        int mainTableRow = -1;
                        for (int j = 0; j < tableModel.getRowCount(); j++) {
                            if ((int)tableModel.getValueAt(j, 1) == productId) { // ID is now column 1
                                mainTableRow = j;
                                break;
                            }
                        }
                        if (mainTableRow == -1) {
                            errors.append("- ").append(productName).append(": Product not found\n");
                            skipCount++;
                            continue;
                        }
                        int currentStock = (int) tableModel.getValueAt(mainTableRow, 9); // Stock is now column 9
                        if (removeQty <= 0) {
                            errors.append("- ").append(productName).append(": Quantity must be positive\n");
                            skipCount++;
                            continue;
                        }
                        if (removeQty > currentStock) {
                            errors.append("- ").append(productName).append(": Insufficient stock\n");
                            skipCount++;
                            continue;
                        }
                        productRepository.removeStock(productId, removeQty, removeReason);
                        successCount++;
                        break;

                    case "DELETE":
                        if (productRepository.deleteProduct(productId)) {
                            successCount++;
                        } else {
                            errors.append("- ").append(productName).append(": Not found\n");
                            skipCount++;
                        }
                        break;

                    case "RETURN":
                        String returnQtyStr = dialogModel.getValueAt(i, 4).toString().trim(); // Column 4
                        String returnType = dialogModel.getValueAt(i, 5).toString().trim(); // Column 5
                        String notes = dialogModel.getValueAt(i, 6).toString().trim(); // Column 6
                        
                        if (returnQtyStr.isEmpty() || returnType.isEmpty()) {
                            errors.append("- ").append(productName).append(": Missing quantity or return type\n");
                            skipCount++;
                            continue;
                        }
                        
                        int returnQty = Integer.parseInt(returnQtyStr);
                        if (returnQty <= 0) {
                            errors.append("- ").append(productName).append(": Invalid quantity\n");
                            skipCount++;
                            continue;
                        }
                        
                        // Map return type to processReturn keywords
                        String processReturnType;
                        if (returnType.equals("Customer Return")) {
                            processReturnType = "CUSTOMER-RETURN";
                        } else if (returnType.equals("Reject/Damaged")) {
                            processReturnType = "REJECT";
                        } else if (returnType.equals("Refund to Supplier")) {
                            processReturnType = "REFUND";
                        } else {
                            processReturnType = "CUSTOMER-RETURN"; // default
                        }
                        
                        productRepository.processReturn(productId, returnQty, processReturnType, notes);
                        successCount++;
                        break;
                }
            } catch (NumberFormatException ex) {
                errors.append("- ").append(productName).append(": Invalid number format\n");
                skipCount++;
            } catch (SQLException ex) {
                errors.append("- ").append(productName).append(": ").append(ex.getMessage()).append("\n");
                skipCount++;
            }
        }

        dialog.dispose();

        String message = getResultMessage(operationType, successCount, skipCount);
        if (skipCount > 0 && errors.length() > 0) {
            message += "\n\nErrors:\n" + errors.toString();
            JOptionPane.showMessageDialog(this, message, "Operation Results", JOptionPane.WARNING_MESSAGE);
        } else if (successCount > 0) {
            showSuccess(message);
        } else {
            showError("No products were processed.");
        }

        loadProductsFromDB();
    }

    // Get result message
    private String getResultMessage(String operationType, int successCount, int skipCount) {
        switch (operationType) {
            case "SET_MARKUP":
                return String.format("Markup updated for %d product(s). Skipped: %d", successCount, skipCount);
            case "REMOVE_STOCK":
                return String.format("Stock removed from %d product(s). Skipped: %d", successCount, skipCount);
            case "DELETE":
                return String.format("Deleted %d product(s). Skipped: %d", successCount, skipCount);
            case "RETURN":
                return String.format("Processed %d return(s). Skipped: %d", successCount, skipCount);
            default:
                return String.format("Processed %d item(s). Skipped: %d", successCount, skipCount);
        }
    }

    // Show Generate QR Dialog
    private void showGenerateQRDialog() {
        if (userRole.equalsIgnoreCase("Employee")) {
            showError("Access Denied: Only Admins can generate QR codes.");
            return;
        }

        JDialog qrDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Generate Product QR Code", true);
        qrDialog.setLayout(new BorderLayout());
        qrDialog.setSize(1000, 750);
        qrDialog.setLocationRelativeTo(this);
        qrDialog.setResizable(true);

        // Create QRCodePanel
        QRCodePanel qrPanel = new QRCodePanel(mainFrame);
        
        // Create wrapper with minimal padding to fit all content
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(UIConstants.BACKGROUND_COLOR);
        wrapper.setBorder(new EmptyBorder(5, 10, 5, 10));
        wrapper.add(qrPanel, BorderLayout.CENTER);
        
        qrDialog.add(wrapper, BorderLayout.CENTER);

        // Add close button at bottom
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 8));
        buttonPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        
        JButton closeButton = new JButton("Close");
        closeButton.setFont(UIConstants.BUTTON_FONT);
        closeButton.setPreferredSize(new Dimension(120, 38));
        closeButton.setBackground(UIConstants.TEXT_SECONDARY);
        closeButton.setForeground(Color.WHITE);
        closeButton.setFocusPainted(false);
        closeButton.setBorderPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setOpaque(true);
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> qrDialog.dispose());
        
        buttonPanel.add(closeButton);
        qrDialog.add(buttonPanel, BorderLayout.SOUTH);

        qrDialog.setVisible(true);
    }

    // Test method
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Products Test");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1400, 800);
            userFrame mockFrame = new userFrame();
            mockFrame.loggedInUserId = 1;
            mockFrame.loggedInUserRole = "Admin";
            frame.add(new productsPanel(mockFrame));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}