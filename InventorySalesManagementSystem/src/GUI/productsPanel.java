package GUI;

import Database.DatabaseConnection; // Keep this if DatabaseConnection is here
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamException;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.json.JSONException;
import org.json.JSONObject;

import GUI.Category;
import GUI.Product;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class productsPanel extends JPanel {

    private DefaultTableModel tableModel;
    private JTable productsTable;
    private WebcamPanel webcamPanel;
    private Webcam webcam;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean isScanning = new AtomicBoolean(false);
    private JPanel scannerDisplayPanel;

    private final int storeId;
    private final ProductRepository productRepository; // <-- NEW: Repository

    public enum ScannerMode {
        OFF,
        ADD_UPDATE_STOCK,
        SELL
    }
    private ScannerMode currentScannerMode = ScannerMode.OFF;
    private JButton scanAddStockButton;
    private JButton scanSellButton;

    public productsPanel(int storeId) {
        this.storeId = storeId;
        this.productRepository = new ProductRepository(storeId); // <-- NEW: Initialize repository

        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // --- Top Panel ---
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.WHITE);
        JLabel titleLabel = new JLabel("MANAGE PRODUCTS AND STOCKS");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        topPanel.add(titleLabel, BorderLayout.WEST);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadProductsFromDB());
        topPanel.add(refreshButton, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // --- Table ---
        String[] columnNames = {"Product ID", "Name", "Category", "Cost Price", "Selling Price", "Stock", "Total Cost"};
        tableModel = new DefaultTableModel(new Object[][]{}, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        productsTable = new JTable(tableModel);

        productsTable.setFont(new Font("Arial", Font.PLAIN, 14));
        productsTable.setRowHeight(25);
        productsTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));

        productsTable.getColumnModel().getColumn(0).setPreferredWidth(80);  // Product ID
        productsTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Name
        productsTable.getColumnModel().getColumn(2).setPreferredWidth(100); // Category
        productsTable.getColumnModel().getColumn(3).setPreferredWidth(90); // Cost Price
        productsTable.getColumnModel().getColumn(4).setPreferredWidth(90); // Selling Price
        productsTable.getColumnModel().getColumn(5).setPreferredWidth(60);  // Stock
        productsTable.getColumnModel().getColumn(6).setPreferredWidth(100); // Total Cost

        JScrollPane tableScrollPane = new JScrollPane(productsTable);
        add(tableScrollPane, BorderLayout.CENTER);

        // --- Side Panel ---
        JPanel sidePanel = createSidePanel();
        add(sidePanel, BorderLayout.EAST);

        loadProductsFromDB();
    }

    /**
     * Loads product data from the repository and populates the JTable.
     */
    private void loadProductsFromDB() {
        tableModel.setRowCount(0); // Clear existing rows
        try {
            List<Product> products = productRepository.getAllProducts();
            for (Product product : products) {
                tableModel.addRow(new Object[]{
                    product.id(),
                    product.name(),
                    product.categoryName(),
                    product.costPrice(),
                    product.sellingPrice() == 0.0 ? null : product.sellingPrice(), // Show null if 0
                    product.stock(),
                    product.totalCost()
                });
            }
        } catch (SQLException e) {
            showError("Error loading products: " + e.getMessage());
            e.printStackTrace(); // Also print stack trace for debugging
        }
    }

    /**
     * Creates the side panel with buttons and the scanner display.
     */
    private JPanel createSidePanel() {
        JPanel mainSidePanel = new JPanel(new BorderLayout(10, 10));
        mainSidePanel.setBorder(BorderFactory.createTitledBorder("Controls & Scanner"));
        mainSidePanel.setPreferredSize(new Dimension(340, 0));
        mainSidePanel.setBackground(Color.WHITE);

        scannerDisplayPanel = new JPanel(new BorderLayout());
        scannerDisplayPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        scannerDisplayPanel.add(new JLabel("Scanner Off", SwingConstants.CENTER), BorderLayout.CENTER);

        JPanel controlsContainer = new JPanel();
        controlsContainer.setLayout(new BoxLayout(controlsContainer, BoxLayout.Y_AXIS));
        controlsContainer.setBackground(Color.WHITE);
        controlsContainer.setBorder(new EmptyBorder(5, 5, 5, 5));

        JButton addProductButton = new JButton("Add / Update Product (Manual)");
        JButton sellProductButton = new JButton("Sell Selected Product (Manual)");
        JButton removeStockButton = new JButton("Remove Stock (Manual)");
        JButton deleteProductButton = new JButton("Delete Product");
        scanAddStockButton = new JButton("Scan to Add/Update Stock");
        scanSellButton = new JButton("Scan to Sell");

        Component[] buttons = {addProductButton, sellProductButton, removeStockButton, deleteProductButton, scanAddStockButton, scanSellButton};
        for (Component comp : buttons) {
            if (comp instanceof JButton) {
                ((JButton) comp).setAlignmentX(Component.CENTER_ALIGNMENT);
                ((JButton) comp).setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
                ((JButton) comp).setMargin(new Insets(5, 5, 5, 5));
            }
        }

        controlsContainer.add(addProductButton);
        controlsContainer.add(Box.createRigidArea(new Dimension(0, 10)));
        controlsContainer.add(sellProductButton);
        controlsContainer.add(Box.createRigidArea(new Dimension(0, 10)));
        controlsContainer.add(removeStockButton);
        controlsContainer.add(Box.createRigidArea(new Dimension(0, 10)));
        controlsContainer.add(deleteProductButton);
        controlsContainer.add(Box.createRigidArea(new Dimension(0, 15)));
        controlsContainer.add(scanAddStockButton);
        controlsContainer.add(Box.createRigidArea(new Dimension(0, 10)));
        controlsContainer.add(scanSellButton);

        // --- Button Actions ---
        addProductButton.addActionListener(e -> handleAddOrUpdateProductViaDialog());
        sellProductButton.addActionListener(e -> handleSellProductManual());
        removeStockButton.addActionListener(e -> handleRemoveStock());
        deleteProductButton.addActionListener(e -> handleDeleteProduct());

        scanAddStockButton.addActionListener(e -> toggleScanner(ScannerMode.ADD_UPDATE_STOCK));
        scanSellButton.addActionListener(e -> toggleScanner(ScannerMode.SELL));
        
        mainSidePanel.add(scannerDisplayPanel, BorderLayout.CENTER);
        mainSidePanel.add(controlsContainer, BorderLayout.SOUTH);

        return mainSidePanel;
    }
    
    // --- Helper UI Methods ---
    
    /** Shows a standardized error message dialog. */
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /** Shows a standardized success message dialog. */
    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Finds and selects a product in the table by its ID.
     * Runs on the Event Dispatch Thread.
     */
    private void findAndSelectProduct(String id) {
        SwingUtilities.invokeLater(() -> {
            try {
                int productId = Integer.parseInt(id);
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    if (productId == (int) tableModel.getValueAt(i, 0)) {
                        productsTable.setRowSelectionInterval(i, i);
                        productsTable.scrollRectToVisible(productsTable.getCellRect(i, 0, true));
                        return;
                    }
                }
            } catch (NumberFormatException e) {
                // Ignore if ID is not a number
            }
        });
    }

    // --- Core Logic Methods (Button Handlers) ---

    /**
     * Handles the "Add / Update Product" button click.
     * Shows a dialog to get product info and calls the repository.
     */
    private void handleAddOrUpdateProductViaDialog() {
        JTextField idField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField priceField = new JTextField(); // Cost Price
        JTextField stockField = new JTextField(); // Stock (to set)
        JTextField sellingPriceField = new JTextField();
        JComboBox<String> categoryComboBox = new JComboBox<>();
        
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        panel.add(new JLabel("Product ID:"));
        panel.add(idField);
        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        
        JPanel categoryPanel = new JPanel(new BorderLayout(5, 0));
        categoryPanel.add(categoryComboBox, BorderLayout.CENTER);
        JButton addCategoryButton = new JButton("+");
        addCategoryButton.setMargin(new Insets(1, 2, 1, 2));
        addCategoryButton.setToolTipText("Add a new category");
        categoryPanel.add(addCategoryButton, BorderLayout.EAST);
        
        panel.add(new JLabel("Category:"));
        panel.add(categoryPanel);
        
        panel.add(new JLabel("Price (Cost):"));
        panel.add(priceField);
        panel.add(new JLabel("Selling Price:"));
        panel.add(sellingPriceField);
        panel.add(new JLabel("Stock (Set Total):")); // Clarified label
        panel.add(stockField);

        Map<String, Integer> categoryIdMap = new HashMap<>();
        loadCategoriesIntoComboBox(categoryComboBox, categoryIdMap);

        addCategoryButton.addActionListener(e -> {
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
        });
    
        int result = JOptionPane.showConfirmDialog(this, panel, "Add / Update Product", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    
        if (result == JOptionPane.OK_OPTION) {
            try {
                int id = Integer.parseInt(idField.getText());
                String name = nameField.getText();
                double costPrice = Double.parseDouble(priceField.getText());
                double sellingPrice = Double.parseDouble(sellingPriceField.getText());
                int stock = Integer.parseInt(stockField.getText());
                String selectedCategory = (String) categoryComboBox.getSelectedItem();
                
                if (name.trim().isEmpty() || selectedCategory == null) {
                    showError("All fields are required.");
                    return;
                }
                int categoryId = categoryIdMap.get(selectedCategory);
                
                // Use the repository to add/update
                if (productRepository.manualUpsertProduct(id, name, categoryId, costPrice, sellingPrice, stock)) {
                    showSuccess("Product saved successfully!");
                } else {
                    showError("Product with ID " + id + " not found or not saved.");
                }

            } catch (NumberFormatException ex) {
                showError("ID, Prices, and Stock must be valid numbers.");
            } catch (SQLException ex) {
                 if (ex.getMessage().contains("Duplicate entry")) {
                    showError("Error adding product: Product ID " + idField.getText() + " already exists.");
                 } else {
                    showError("Error saving product: " + ex.getMessage());
                 }
            } finally {
                loadProductsFromDB();
                findAndSelectProduct(idField.getText());
            }
        }
    }

    /**
     * Handles the "Sell Selected Product" button click.
     */
    private void handleSellProductManual() {
        int selectedRow = productsTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("Please select a product from the table first.");
            return;
        }
    
        String productId = tableModel.getValueAt(selectedRow, 0).toString();
        String productName = tableModel.getValueAt(selectedRow, 1).toString();
        Object sellingPriceObj = tableModel.getValueAt(selectedRow, 4);
        String currentSellingPrice = (sellingPriceObj != null) ? sellingPriceObj.toString() : "0.0";
        
        JTextField quantityField = new JTextField("1");
        JTextField priceField = new JTextField(currentSellingPrice);
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        panel.add(new JLabel("Quantity to sell for " + productName + ":"));
        panel.add(quantityField);
        panel.add(new JLabel("Price per item:"));
        panel.add(priceField);
    
        int result = JOptionPane.showConfirmDialog(this, panel, "Sell Product", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    
        if (result == JOptionPane.OK_OPTION) {
            try {
                int quantity = Integer.parseInt(quantityField.getText());
                double sellingPrice = Double.parseDouble(priceField.getText());
    
                if (quantity > 0 && sellingPrice >= 0) {
                    processSale(productId, quantity, sellingPrice); // Use centralized method
                } else {
                    showError("Quantity and price must be positive numbers.");
                }
            } catch (NumberFormatException ex) {
                showError("Invalid quantity or price entered.");
            }
        }
    }

    /**
     * Handles the "Remove Stock" button click.
     */
    private void handleRemoveStock() {
        int selectedRow = productsTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("Please select a product from the table first.");
            return;
        }

        int productId = (int) tableModel.getValueAt(selectedRow, 0);
        String productName = (String) tableModel.getValueAt(selectedRow, 1);
        int currentStock = (int) tableModel.getValueAt(selectedRow, 5); // Index 5 for stock

        String quantityStr = JOptionPane.showInputDialog(this,
                "Product: " + productName + "\nCurrent Stock: " + currentStock + "\n\nEnter quantity to remove:",
                "Remove Stock", JOptionPane.PLAIN_MESSAGE);

        if (quantityStr != null && !quantityStr.trim().isEmpty()) {
            try {
                int quantityToRemove = Integer.parseInt(quantityStr);

                if (quantityToRemove <= 0) {
                    showError("Quantity to remove must be a positive number.");
                    return;
                }
                if (quantityToRemove > currentStock) {
                    showError("Cannot remove more stock than is available.");
                    return;
                }

                // Use repository to remove stock
                productRepository.removeStock(productId, quantityToRemove);
                showSuccess("Successfully removed " + quantityToRemove + " items from stock.");

            } catch (NumberFormatException ex) {
                showError("Invalid quantity entered. Please enter a valid number.");
            } catch (SQLException ex) {
                showError("Error removing stock: " + ex.getMessage());
            } finally {
                loadProductsFromDB();
                findAndSelectProduct(String.valueOf(productId));
            }
        }
    }
    
    /**
     * Handles the "Delete Product" button click.
     */
    private void handleDeleteProduct() {
        int selectedRow = productsTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("Please select a product from the table first.");
            return;
        }

        int productId = (int) tableModel.getValueAt(selectedRow, 0);
        String productName = (String) tableModel.getValueAt(selectedRow, 1);
        int currentStock = (int) tableModel.getValueAt(selectedRow, 5);

        String confirmationMessage = String.format(
            "Are you sure you want to delete the product '%s' (ID: %d)?\n" +
            "Current Stock in this store: %d\n\n" +
            "WARNING: This will permanently delete all associated sales and stock records for this specific store.",
            productName, productId, currentStock);

        int confirm = JOptionPane.showConfirmDialog(this,
                confirmationMessage,
                "Confirm Product Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                if (productRepository.deleteProduct(productId)) {
                    showSuccess("Product '" + productName + "' deleted successfully.");
                } else {
                    showError("Product '" + productName + "' not found in this store.");
                }
            } catch (SQLException ex) {
                showError("Error deleting product: " + ex.getMessage());
            } finally {
                loadProductsFromDB();
            }
        }
    }
    
    /**
     * Loads categories from the repository into the combo box.
     */
    public void loadCategoriesIntoComboBox(JComboBox<String> comboBox, Map<String, Integer> categoryIdMap) {
        comboBox.removeAllItems();
        categoryIdMap.clear();
        try {
            List<Category> categories = productRepository.getCategories();
            for (Category category : categories) {
                categoryIdMap.put(category.name(), category.id());
                comboBox.addItem(category.name());
            }
        } catch (SQLException e) {
            showError("Could not load categories: " + e.getMessage());
        }
    }

    // --- Business Logic Methods (Called by handlers) ---

    /**
     * Processes a sale by calling the repository and handling UI updates.
     * @param productId The product ID to sell.
     * @param quantityToSell The quantity to sell.
     * @param sellingPrice The price per item.
     */
    private void processSale(String productId, int quantityToSell, double sellingPrice) {
        try {
            productRepository.sellProduct(productId, quantityToSell, sellingPrice);
            showSuccess("Sale recorded successfully!");
        } catch (SQLException e) {
            // Use the specific message from the exception
            showError("Transaction Error: " + e.getMessage());
        } finally {
            loadProductsFromDB();
            findAndSelectProduct(productId);
        }
    }

    /**
     * Processes a QR code string.
     * Differentiates between JSON commands and plain text (product IDs).
     */
    /**
     * Processes a QR code string.
     * Differentiates between JSON commands and plain text (product IDs).
     */
    private void processQRCodeCommand(String qrText) {
        try {
            JSONObject json = new JSONObject(qrText);
            String action = json.getString("action");
            JSONObject data = json.getJSONObject("data");

            switch (action) {
                case "create_product":
                    if (currentScannerMode == ScannerMode.ADD_UPDATE_STOCK) {
                        upsertProductFromJSON(data);
                    } else {
                        showError("Scanner is not in Add/Update mode.");
                    }
                    break;
               case "sell_product":
                    if (currentScannerMode == ScannerMode.SELL) {
                        int productIdToSell = data.getInt("id");
                        // FIX: Call getProductSellingPrice instead of getProductCostPrice
                        double sellingPrice = productRepository.getProductSellingPrice(String.valueOf(productIdToSell));
                        // Optional: Add check if sellingPrice is 0 or negative
                        if (sellingPrice <= 0) {
                            showError("Product ID " + productIdToSell + " has no valid selling price set. Cannot sell.");
                            return; // Stop the sale
                        }
                        processSale(String.valueOf(productIdToSell), 1, sellingPrice);
                    } else {
                        showError("Scanner is not in Sell mode.");
                    }
                    break;
                case "delete_product":
                    handleDeleteProductFromQR(data.getInt("id"));
                    break;
                default:
                    showError("Unknown QR action: " + action);
            }
        } catch (JSONException e) {
            // Not a JSON command, treat as plain product ID
            if (currentScannerMode == ScannerMode.SELL) {
                // Sell 1 item at its default selling price
                try {
                     // FIX: Call getProductSellingPrice instead of getProductCostPrice
                    double sellingPrice = productRepository.getProductSellingPrice(qrText);
                     // Optional: Add check if sellingPrice is 0 or negative
                    if (sellingPrice <= 0) {
                        showError("Product ID " + qrText + " has no valid selling price set. Cannot sell.");
                        return; // Stop the sale
                    }
                    processSale(qrText, 1, sellingPrice);
                } catch (SQLException ex) {
                    showError("Error processing sale: " + ex.getMessage());
                }
            } else if (currentScannerMode == ScannerMode.ADD_UPDATE_STOCK) {
                findAndSelectProduct(qrText);
            }
        } catch (SQLException e) {
            showError("Database error during QR processing: " + e.getMessage());
        }
    }

    /**
     * Handles product creation/update from a JSON QR code.
     */
    private void upsertProductFromJSON(JSONObject data) {
        if (data == null) {
            showError("QR code data is empty.");
            return;
        }
        
        String productIdStr = "";
        try {
            productIdStr = String.valueOf(data.getInt("id"));
            int affectedRows = productRepository.upsertProductFromQR(data);
            
            // rowsAffected == 1 is an INSERT, > 1 is an UPDATE
            if (affectedRows == 1) {
                showSuccess("New product added successfully!");
            }
            // No popup for updates, just the beep sound.
            
        } catch (JSONException | SQLException ex) {
            showError("Error saving product via QR: " + ex.getMessage());
        } finally {
             // Refresh and select on the EDT
             final String finalProductIdStr = productIdStr;
             SwingUtilities.invokeLater(() -> {
                loadProductsFromDB();
                if (!finalProductIdStr.isEmpty()) {
                    findAndSelectProduct(finalProductIdStr);
                }
             });
        }
    }

    /**
     * Handles product deletion from a JSON QR code.
     */
    private void handleDeleteProductFromQR(int productId) {
        // This is a dangerous operation, so we add an extra confirmation
        String message = String.format(
            "A QR code requested to DELETE product ID: %d.\n" +
            "Are you sure you want to proceed?\n\n" +
            "This will delete all stock and sales data for this product in this store.",
            productId);
            
        int confirm = JOptionPane.showConfirmDialog(this,
                message,
                "Confirm QR Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
             try {
                if (productRepository.deleteProduct(productId)) {
                    showSuccess("Product ID " + productId + " deleted successfully via QR.");
                } else {
                    showError("Product ID " + productId + " not found in this store.");
                }
            } catch (SQLException ex) {
                showError("Error deleting product via QR: " + ex.getMessage());
            } finally {
                loadProductsFromDB();
            }
        }
    }

    // --- Scanner Control Methods ---

    /**
     * Toggles the scanner on or off for the requested mode.
     */
    private void toggleScanner(ScannerMode requestedMode) {
        // If we clicked the button for the mode that is already running, stop it.
        if (currentScannerMode != ScannerMode.OFF && currentScannerMode == requestedMode) {
            stopScanner();
        }
        // If the scanner is OFF, start it in the requested mode.
        else if (currentScannerMode == ScannerMode.OFF) {
            startScanner(requestedMode);
        }
        // If it's on, but in a *different* mode, show a message.
        else {
            JOptionPane.showMessageDialog(this,
                "Scanner is already active in " + currentScannerMode + " mode.\nPlease stop it first.",
                "Scanner Busy", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Starts the webcam and scanning loop for a specific mode.
     */
    private void startScanner(ScannerMode mode) {
        if (currentScannerMode != ScannerMode.OFF) {
            showError("Scanner is already running.");
            return;
        }
        currentScannerMode = mode;

        // Update UI to show "Starting..."
        scanAddStockButton.setEnabled(false);
        scanSellButton.setEnabled(false);
        if (mode == ScannerMode.ADD_UPDATE_STOCK) {
            scanAddStockButton.setText("Starting...");
        } else if (mode == ScannerMode.SELL) {
            scanSellButton.setText("Starting...");
        }

        // Run webcam initialization on the executor thread
        executor.execute(() -> {
            Webcam newWebcam = null;
            WebcamPanel newWebcamPanel = null;
            boolean initSuccess = false;

            try {
                newWebcam = Webcam.getDefault();
                if (newWebcam == null) throw new WebcamException("No webcam found.");

                Dimension size = WebcamResolution.QVGA.getSize();
                boolean supported = false;
                for(Dimension d : newWebcam.getViewSizes()){ if(d.equals(size)){supported=true; break;}}
                if(!supported) size = newWebcam.getViewSize();

                newWebcam.setViewSize(size);
                newWebcam.open(true);
                
                newWebcamPanel = new WebcamPanel(newWebcam);
                newWebcamPanel.setPreferredSize(size);
                newWebcamPanel.setMirrored(false);
                initSuccess = true;

            } catch (Exception ex) {
                // Handle errors on the EDT
                final String errorMessage = (ex instanceof WebcamException) ?
                    "Failed to start webcam: " + ex.getMessage() :
                    "Webcam is already in use or locked.";
                SwingUtilities.invokeLater(() -> handleScannerStartError(errorMessage));
                if (newWebcam != null && newWebcam.isOpen()) try { newWebcam.close(); } catch (Exception e) {}

            } finally {
                // Update UI back on the EDT
                 final boolean finalSuccess = initSuccess;
                 final WebcamPanel finalPanel = newWebcamPanel;
                 final Webcam finalWebcam = newWebcam;

                SwingUtilities.invokeLater(() -> {
                    if (finalSuccess) {
                        webcam = finalWebcam;
                        webcamPanel = finalPanel;
                        isScanning.set(true);

                        scannerDisplayPanel.removeAll();
                        scannerDisplayPanel.add(webcamPanel, BorderLayout.CENTER);
                        scannerDisplayPanel.revalidate();
                        scannerDisplayPanel.repaint();

                        // Enable *only* the active mode's button as a "Stop" button
                        if (mode == ScannerMode.ADD_UPDATE_STOCK) {
                            scanAddStockButton.setText("Stop Add Scan");
                            scanAddStockButton.setEnabled(true);
                        } else if (mode == ScannerMode.SELL) {
                            scanSellButton.setText("Stop Sell Scan");
                            scanSellButton.setEnabled(true);
                        }
                        
                        // Start the scan loop
                        executor.execute(this::scanLoop);
                    } else {
                        // Reset UI if initialization failed
                        currentScannerMode = ScannerMode.OFF;
                        scanAddStockButton.setEnabled(true);
                        scanSellButton.setEnabled(true);
                        scanAddStockButton.setText("Scan to Add/Update Stock");
                        scanSellButton.setText("Scan to Sell");
                    }
                });
            }
        });
    }

    /**
     * Stops the webcam and scanning loop.
     */
    private void stopScanner() {
        if (currentScannerMode == ScannerMode.OFF) {
            return; // Already stopped
        }
        
        currentScannerMode = ScannerMode.OFF;
        isScanning.set(false); // Signal scanLoop to stop

        // Schedule resource cleanup on the executor
        executor.execute(() -> {
            WebcamPanel panelToStop = webcamPanel;
            if (panelToStop != null) {
                try { panelToStop.stop(); } catch (Exception e) { e.printStackTrace(); }
            }

            Webcam camToClose = webcam;
            if (camToClose != null && camToClose.isOpen()) {
                try { camToClose.close(); }
                catch (Exception e) { e.printStackTrace(); }
            }

            // Nullify fields
            webcamPanel = null;
            webcam = null;

            // Update UI back on the EDT *after* cleanup is done
            SwingUtilities.invokeLater(() -> {
                scanAddStockButton.setText("Scan to Add/Update Stock");
                scanSellButton.setText("Scan to Sell");
                scanAddStockButton.setEnabled(true);
                scanSellButton.setEnabled(true);

                if (scannerDisplayPanel != null) {
                    scannerDisplayPanel.removeAll();
                    scannerDisplayPanel.add(new JLabel("Scanner Off", SwingConstants.CENTER), BorderLayout.CENTER);
                    scannerDisplayPanel.revalidate();
                    scannerDisplayPanel.repaint();
                }
            });
        });
    }

    /**
     * Helper to reset UI components after a scanner startup error.
     */
    private void handleScannerStartError(String errorMessage) {
        showError(errorMessage);
        currentScannerMode = ScannerMode.OFF;
        isScanning.set(false);
        webcam = null;
        webcamPanel = null;
        scanAddStockButton.setEnabled(true);
        scanSellButton.setEnabled(true);
        scanAddStockButton.setText("Scan to Add/Update Stock");
        scanSellButton.setText("Scan to Sell");
        scannerDisplayPanel.removeAll();
        scannerDisplayPanel.add(new JLabel("Scanner Error", SwingConstants.CENTER), BorderLayout.CENTER);
        scannerDisplayPanel.revalidate();
        scannerDisplayPanel.repaint();
    }

    /**
     * The main loop that captures images and decodes QR codes.
     * Runs on the executor thread.
     */
    private void scanLoop() {
        do {
            try {
                Thread.sleep(100); // Small delay between captures
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            Result result = null;
            BufferedImage image = null;
            Webcam currentWebcam = this.webcam; // Use volatile-like read

            if (currentWebcam == null || !currentWebcam.isOpen()) {
                break; // Webcam was closed
            }

            try {
                image = currentWebcam.getImage();
            } catch (Exception e) {
                 System.err.println("scanLoop: Exception getting image: " + e.getMessage());
                 continue; // Try again
            }

            if (image != null) {
                LuminanceSource source = new BufferedImageLuminanceSource(image);
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                try {
                    result = new MultiFormatReader().decode(bitmap);
                } catch (NotFoundException e) {
                    // No QR code found, this is normal
                } catch (Exception e) {
                    System.err.println("scanLoop: Error during decode: " + e.getMessage());
                }
            }

            if (result != null && isScanning.get()) {
                playSound("beep.wav");
                final String qrText = result.getText();

                // Process the result on the EDT
                SwingUtilities.invokeLater(() -> {
                     if (isScanning.get()) { // Double-check scanner wasn't stopped
                          processQRCodeCommand(qrText);
                     }
                });

                try {
                    // Pause after a successful scan to prevent rapid re-scans
                    Thread.sleep(2000); 
                } catch (InterruptedException e) {
                   Thread.currentThread().interrupt();
                   break;
                }
            }

        } while (isScanning.get());
        
        // Loop finished, ensure scanner is fully stopped (in case of error)
        if (currentScannerMode != ScannerMode.OFF) {
            SwingUtilities.invokeLater(this::stopScanner);
        }
    }
    
    /**
     * Plays a notification sound.
     */
    private void playSound(String soundFileName) {
        try {
            String resourcePath = "/resources/" + soundFileName;
            InputStream audioSrc = getClass().getResourceAsStream(resourcePath);
            
            if (audioSrc == null) {
                System.err.println("Warning: Could not find sound file: " + resourcePath);
                return;
            }
            InputStream bufferedIn = new BufferedInputStream(audioSrc);
            
            javax.sound.sampled.AudioInputStream audioStream = javax.sound.sampled.AudioSystem.getAudioInputStream(bufferedIn);
            javax.sound.sampled.Clip clip = javax.sound.sampled.AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();
            
        } catch (Exception e) {
            System.err.println("Error playing sound: " + e.getMessage());
            e.printStackTrace();
        }
    }
}