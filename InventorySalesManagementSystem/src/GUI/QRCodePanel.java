package GUI;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.json.JSONObject;
import org.json.JSONException;
import Database.DatabaseConnection; //

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QRCodePanel extends JPanel {

    // --- Form Fields ---
    private JTextField productIdField;
    private JTextField productNameField;
    private JTextField costPriceField;
    private JTextField sellingPriceField;
    private JTextField stockField;
    private JComboBox<String> categoryComboBox;
    private final Map<String, Integer> categoryIdMap = new HashMap<>();

    // --- QR Code Display ---
    private JLabel qrCodeDisplayLabel;
    private JButton saveButton;
    private BufferedImage currentQRCodeImage;

    // --- Mode Selection ---
    private JRadioButton addStockRadio;
    private JRadioButton sellProductRadio;
    private final Color readOnlyColor = new Color(235, 235, 235);

    // --- Data & Frame ---
    private final int storeId;
    private final ProductRepository productRepository; //

    public QRCodePanel(int storeId) {
        this.storeId = storeId;
        this.productRepository = new ProductRepository(storeId); //

        // Main layout centers the content panel
        setLayout(new GridBagLayout());
        setBorder(new EmptyBorder(15, 15, 15, 15));
        setBackground(UIConstants.BACKGROUND_COLOR); //

        // --- Title ---
        JLabel titleLabel = new JLabel("Generate Product QR Code", SwingConstants.CENTER);
        titleLabel.setFont(UIConstants.TITLE_FONT); //
        titleLabel.setBorder(new EmptyBorder(0, 0, 15, 0)); // Bottom margin

        // --- Content Panel (holds form and QR display side-by-side) ---
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBackground(UIConstants.BACKGROUND_COLOR); // Match main background

        GridBagConstraints gbcContent = new GridBagConstraints();
        gbcContent.insets = new Insets(0, 10, 0, 10); // Spacing between form and QR
        gbcContent.fill = GridBagConstraints.VERTICAL; // Allow vertical stretching
        gbcContent.anchor = GridBagConstraints.NORTHWEST; // Anchor components top-left

        // --- Left Side: Form ---
        JPanel formPanel = createFormPanel(); // This panel now takes its natural size
        gbcContent.gridx = 0;
        gbcContent.gridy = 0;
        gbcContent.weightx = 0; // Don't allow horizontal stretching
        contentPanel.add(formPanel, gbcContent);

        // --- Right Side: QR Display ---
        JPanel qrDisplayPanel = createQrDisplayPanel();
        gbcContent.gridx = 1;
        gbcContent.gridy = 0;
        gbcContent.weightx = 1; // Allow QR panel to take extra horizontal space if needed
        gbcContent.fill = GridBagConstraints.BOTH; // Fill available space
        contentPanel.add(qrDisplayPanel, gbcContent);

        // Add title and content panel to the main panel
        GridBagConstraints gbcMain = new GridBagConstraints();
        gbcMain.gridx = 0;
        gbcMain.gridy = 0;
        gbcMain.weightx = 1.0;
        gbcMain.fill = GridBagConstraints.HORIZONTAL;
        gbcMain.anchor = GridBagConstraints.NORTH;
        add(titleLabel, gbcMain);

        gbcMain.gridy = 1;
        gbcMain.weighty = 1.0; // Allow content panel to take vertical space
        gbcMain.fill = GridBagConstraints.BOTH; // Fill horizontally and vertically
        gbcMain.anchor = GridBagConstraints.CENTER; // Center the content panel
        add(contentPanel, gbcMain);


        // Initial setup
        loadCategories();
        refreshForm();
    }

    // --- Panel Creation Methods ---

    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(UIConstants.FORM_COLOR); //
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Input Details"),
                new EmptyBorder(10, 15, 10, 15))
        );

        // REMOVED: Fixed size settings - panel will size naturally now
        // Dimension fixedSize = new Dimension(400, 500);
        // formPanel.setPreferredSize(fixedSize);
        // formPanel.setMinimumSize(fixedSize);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL; // Fields fill horizontally

        // --- Mode Radio Buttons ---
        addStockRadio = new JRadioButton("Add/Update Stock", true); // Shortened text
        sellProductRadio = new JRadioButton("Sell Product", false); // Shortened text
        addStockRadio.setBackground(UIConstants.FORM_COLOR); //
        sellProductRadio.setBackground(UIConstants.FORM_COLOR); //
        addStockRadio.setFont(UIConstants.LABEL_FONT); //
        sellProductRadio.setFont(UIConstants.LABEL_FONT); //

        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(addStockRadio);
        modeGroup.add(sellProductRadio);

        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        radioPanel.setBackground(UIConstants.FORM_COLOR); //
        radioPanel.add(new JLabel("QR Type: "));
        radioPanel.add(addStockRadio);
        radioPanel.add(Box.createHorizontalStrut(15));
        radioPanel.add(sellProductRadio);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        formPanel.add(radioPanel, gbc);

        // --- Input Fields ---
        gbc.gridwidth = 1; gbc.gridy++;
        gbc.gridx = 0; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        JLabel idLabel = new JLabel("Product ID:"); idLabel.setFont(UIConstants.LABEL_FONT); //
        formPanel.add(idLabel, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL;
        productIdField = new JTextField(15); productIdField.setFont(UIConstants.INPUT_FONT); //
        formPanel.add(productIdField, gbc);

        gbc.gridy++;
        gbc.gridx = 0; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        JLabel nameLabel = new JLabel("Name:"); nameLabel.setFont(UIConstants.LABEL_FONT); //
        formPanel.add(nameLabel, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL;
        productNameField = new JTextField(15); productNameField.setFont(UIConstants.INPUT_FONT); //
        formPanel.add(productNameField, gbc);

        // --- Category with Add Button ---
        gbc.gridy++;
        gbc.gridx = 0; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        JLabel categoryLabel = new JLabel("Category:"); categoryLabel.setFont(UIConstants.LABEL_FONT); //
        formPanel.add(categoryLabel, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel categoryPanel = new JPanel(new BorderLayout(5, 0)); categoryPanel.setBackground(UIConstants.FORM_COLOR); //
        categoryComboBox = new JComboBox<>(); categoryComboBox.setFont(UIConstants.INPUT_FONT); //
        categoryPanel.add(categoryComboBox, BorderLayout.CENTER);
        JButton addCategoryButton = new JButton("+"); addCategoryButton.setMargin(new Insets(1, 4, 1, 4)); addCategoryButton.setToolTipText("Add a new category");
        categoryPanel.add(addCategoryButton, BorderLayout.EAST);
        formPanel.add(categoryPanel, gbc);

        // --- Cost Price ---
        gbc.gridy++;
        gbc.gridx = 0; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        JLabel costLabel = new JLabel("Cost Price:"); costLabel.setFont(UIConstants.LABEL_FONT); //
        formPanel.add(costLabel, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL;
        costPriceField = new JTextField(15); costPriceField.setFont(UIConstants.INPUT_FONT); //
        formPanel.add(costPriceField, gbc);

        // --- Selling Price ---
        gbc.gridy++;
        gbc.gridx = 0; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        JLabel sellingLabel = new JLabel("Selling Price:"); sellingLabel.setFont(UIConstants.LABEL_FONT); //
        formPanel.add(sellingLabel, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL;
        sellingPriceField = new JTextField(15); sellingPriceField.setFont(UIConstants.INPUT_FONT); //
        formPanel.add(sellingPriceField, gbc);

        // --- Stock ---
        gbc.gridy++;
        gbc.gridx = 0; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        JLabel stockLabel = new JLabel("Stock Qty:"); stockLabel.setFont(UIConstants.LABEL_FONT); //
        formPanel.add(stockLabel, gbc);
        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL;
        stockField = new JTextField(15); stockField.setFont(UIConstants.INPUT_FONT); //
        formPanel.add(stockField, gbc);

        // --- Buttons ---
        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER; gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(20, 0, 10, 0);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0)); buttonPanel.setBackground(UIConstants.FORM_COLOR); //
        JButton generateButton = new JButton("Generate QR"); generateButton.setFont(UIConstants.BUTTON_FONT); generateButton.setPreferredSize(UIConstants.BUTTON_DIMENSION); //
        JButton refreshButton = new JButton("Refresh Form"); refreshButton.setFont(UIConstants.BUTTON_FONT); refreshButton.setPreferredSize(new Dimension(140, 40));
        buttonPanel.add(refreshButton);
        buttonPanel.add(generateButton);
        formPanel.add(buttonPanel, gbc);

        // --- Action Listeners ---
        addStockRadio.addActionListener(e -> toggleMode());
        sellProductRadio.addActionListener(e -> toggleMode());
        addCategoryButton.addActionListener(e -> handleAddNewCategory());
        generateButton.addActionListener(e -> generateAndDisplayQRCode());
        refreshButton.addActionListener(e -> refreshForm());

        return formPanel;
    }

    private JPanel createQrDisplayPanel() {
        JPanel qrPanel = new JPanel(new BorderLayout(10, 10)); // Gaps
        qrPanel.setBackground(UIConstants.FORM_COLOR); //
        qrPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Generated QR Code"),
                new EmptyBorder(10, 15, 10, 15))
        );

        // Use a wrapper panel with GridBagLayout to center the label
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(UIConstants.FORM_COLOR); // Match background

        qrCodeDisplayLabel = new JLabel("QR Code will appear here", SwingConstants.CENTER);
        qrCodeDisplayLabel.setFont(new Font("Arial", Font.ITALIC, 16));
        qrCodeDisplayLabel.setPreferredSize(new Dimension(300, 300));
        qrCodeDisplayLabel.setMinimumSize(new Dimension(200, 200));
        qrCodeDisplayLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        qrCodeDisplayLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // Center horizontally

        // Add label to wrapper (GridBagLayout defaults center components)
        centerWrapper.add(qrCodeDisplayLabel, new GridBagConstraints());
        qrPanel.add(centerWrapper, BorderLayout.CENTER);


        // Save Button (South)
        saveButton = new JButton("Save QR Code");
        saveButton.setFont(UIConstants.BUTTON_FONT); //
        saveButton.setPreferredSize(UIConstants.BUTTON_DIMENSION); //
        saveButton.setEnabled(false);
        saveButton.addActionListener(e -> saveQRCode());

        JPanel saveButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        saveButtonPanel.setBackground(UIConstants.FORM_COLOR); //
        saveButtonPanel.add(saveButton);
        qrPanel.add(saveButtonPanel, BorderLayout.SOUTH);

        return qrPanel;
    }


    // --- Other methods remain the same ---
    // handleAddNewCategory(), toggleMode(), refreshForm(), loadCategories(),
    // loadNextProductId(), generateAndDisplayQRCode(), generateAddStockJson(),
    // saveQRCode(), addNewCategory(), toBufferedImage(), resetQrDisplay(),
    // showError(), showSuccess()

    private void handleAddNewCategory() {
        String newCategory = JOptionPane.showInputDialog(this, "Enter new category name:", "Add Category", JOptionPane.PLAIN_MESSAGE);
        if (newCategory != null && !newCategory.trim().isEmpty()) {
            addNewCategory(newCategory); // Call the DB method
            loadCategories(); // Reload the combo box
            categoryComboBox.setSelectedItem(newCategory); // Select the newly added item
        }
    }

    private void toggleMode() {
        boolean isAddMode = addStockRadio.isSelected();

        // Enable/disable fields relevant only to "Add/Update" mode
        productNameField.setEnabled(isAddMode);
        categoryComboBox.setEnabled(isAddMode);
        costPriceField.setEnabled(isAddMode);
        sellingPriceField.setEnabled(isAddMode);
        stockField.setEnabled(isAddMode);

        // Product ID is editable only in "Sell" mode
        productIdField.setEditable(!isAddMode);
        productIdField.setBackground(isAddMode ? readOnlyColor : Color.WHITE); // Visual cue

        // Set/Clear values based on mode
        if (isAddMode) {
            stockField.setText("1"); // Default stock for Add QR
            stockField.setBackground(readOnlyColor); // Usually fixed for Add QR
            stockField.setEditable(false);
            loadNextProductId(); // Suggest the next ID
        } else {
            // Clear fields not needed for "Sell" mode
            stockField.setText("");
            stockField.setBackground(Color.WHITE); // Make editable if needed later, but disable for now
            stockField.setEditable(false); // Stock not relevant for Sell ID QR
            productIdField.setText(""); // Clear ID field for user input
            productNameField.setText("");
            costPriceField.setText("");
            sellingPriceField.setText("");
            // Reset category if list is not empty
            categoryComboBox.setSelectedIndex(categoryIdMap.isEmpty() ? -1 : 0);
        }

        // Reset QR code display when mode changes
        resetQrDisplay(); // Use helper method
    }


    private void refreshForm() {
        // Clear common fields first
        productIdField.setText("");
        productNameField.setText("");
        costPriceField.setText("");
        sellingPriceField.setText("");
        stockField.setText("1"); // Reset stock field too

        // Reset category selection
        categoryComboBox.setSelectedIndex(categoryIdMap.isEmpty() ? -1 : 0);

        // Default to "Add/Update Stock" mode
        addStockRadio.setSelected(true);

        // Apply mode settings (this will load next ID, disable fields etc.)
        toggleMode();
    }


    private void loadCategories() {
        String currentSelection = (String) categoryComboBox.getSelectedItem(); // Store current selection
        categoryComboBox.removeAllItems();
        categoryIdMap.clear();

        try {
            List<Category> categories = productRepository.getCategories(); //
            if (categories.isEmpty()) {
                categoryComboBox.addItem("<No categories added>");
                categoryComboBox.setEnabled(false);
            } else {
                categoryComboBox.setEnabled(true);
                for (Category category : categories) { //
                    categoryIdMap.put(category.name(), category.id()); //
                    categoryComboBox.addItem(category.name()); //
                }
                // Try to re-select the previously selected item
                if (currentSelection != null && categoryIdMap.containsKey(currentSelection)) {
                    categoryComboBox.setSelectedItem(currentSelection);
                }
            }
        } catch (SQLException e) {
            showError("Could not load categories: " + e.getMessage());
            categoryComboBox.addItem("<Error loading>");
            categoryComboBox.setEnabled(false);
        }
    }


    private void loadNextProductId() {
        // Only relevant in "Add" mode
        if (!addStockRadio.isSelected()) {
            return;
        }
        // Use repository method if available, otherwise keep direct SQL
        String sql = "SELECT MAX(product_id) FROM products WHERE store_id = " + this.storeId;
        try (Connection conn = DatabaseConnection.getConnection(); //
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                int maxId = rs.getInt(1);
                productIdField.setText(String.valueOf(maxId + 1));
            } else {
                productIdField.setText("1"); // First product
            }
        } catch (SQLException e) {
            showError("Could not fetch the next Product ID: " + e.getMessage());
            productIdField.setText("Error");
        }
    }


    private void generateAndDisplayQRCode() {
        String qrText;

        try {
            if (addStockRadio.isSelected()) {
                qrText = generateAddStockJson();
                if (qrText == null) return;
            } else {
                qrText = productIdField.getText().trim();
                if (qrText.isEmpty()) {
                    showError("Product ID is required for Sell Product mode.");
                    return;
                }
                try { Integer.parseInt(qrText); }
                catch (NumberFormatException ex) {
                    showError("Product ID must be a valid number for Sell Product mode."); return;
                }
            }

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrText, BarcodeFormat.QR_CODE, 280, 280);
            currentQRCodeImage = toBufferedImage(bitMatrix);

            qrCodeDisplayLabel.setIcon(new ImageIcon(currentQRCodeImage));
            qrCodeDisplayLabel.setText(null);
            qrCodeDisplayLabel.setFont(null); // Clear italic font
            saveButton.setEnabled(true);

        } catch (NumberFormatException ex) {
            showError("Product ID, Prices, and Stock must be valid numbers."); resetQrDisplay();
        } catch (WriterException ex) {
            showError("Could not generate QR code image: " + ex.getMessage()); resetQrDisplay();
        } catch (JSONException e) {
            showError("Could not build JSON data for QR code: " + e.getMessage()); resetQrDisplay();
        }
    }


    private String generateAddStockJson() throws NumberFormatException, JSONException {
        // ... (validation code remains the same as previous version) ...
        String productIdStr = productIdField.getText().trim();
        String name = productNameField.getText().trim();
        String selectedCategory = (String) categoryComboBox.getSelectedItem();
        String costPriceStr = costPriceField.getText().trim();
        String sellingPriceStr = sellingPriceField.getText().trim();
        String stockStr = stockField.getText().trim();

        if (productIdStr.isEmpty() || name.isEmpty() || selectedCategory == null || selectedCategory.startsWith("<") ||
            costPriceStr.isEmpty() || sellingPriceStr.isEmpty() || stockStr.isEmpty()) {
            showError("All fields (ID, Name, Category, Prices, Stock) are required for Add/Update mode.");
            return null;
        }

        int productId = Integer.parseInt(productIdStr);
        double costPrice = Double.parseDouble(costPriceStr);
        double sellingPrice = Double.parseDouble(sellingPriceStr);
        int stock = Integer.parseInt(stockStr);
        int categoryId = categoryIdMap.getOrDefault(selectedCategory, -1);

        if (categoryId == -1) { showError("Invalid category selected."); return null; }
        if (costPrice < 0 || sellingPrice < 0 || stock < 0) { showError("Prices and Stock cannot be negative."); return null; }
        if (sellingPrice < costPrice) {
             int choice = JOptionPane.showConfirmDialog(this, "Warning: Selling price is lower than cost price. Continue?", "Price Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
             if (choice == JOptionPane.NO_OPTION) { return null; }
        }

        JSONObject dataObject = new JSONObject();
        dataObject.put("id", productId);
        dataObject.put("name", name);
        dataObject.put("category_id", categoryId);
        dataObject.put("cost_price", costPrice);
        dataObject.put("selling_price", sellingPrice);
        dataObject.put("stock", stock);

        JSONObject mainObject = new JSONObject();
        mainObject.put("action", "create_product"); //
        mainObject.put("data", dataObject);

        return mainObject.toString();
    }


    private void saveQRCode() {
        if (currentQRCodeImage == null) {
            showError("Please generate a QR code first.");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save QR Code As");

        String defaultFileName = "qrcode.png";
        String productName = productNameField.getText().trim().replaceAll("[^a-zA-Z0-9.-]", "_");
        String productId = productIdField.getText().trim();

        if (sellProductRadio.isSelected() && !productId.isEmpty()) {
            defaultFileName = "SELL_ID_" + productId + ".png";
        } else if (!productName.isEmpty()){
            defaultFileName = productName + "_ID_" + productId + ".png";
        } else if (!productId.isEmpty()) {
             defaultFileName = "ADD_ID_" + productId + ".png";
        }

        fileChooser.setSelectedFile(new File(defaultFileName));
        fileChooser.setFileFilter(new FileNameExtensionFilter("PNG Images (*.png)", "png"));

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".png")) {
                fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".png");
            }

            if (fileToSave.exists()) {
                int overwrite = JOptionPane.showConfirmDialog(this, "File already exists. Overwrite?", "Confirm Overwrite", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (overwrite != JOptionPane.YES_OPTION) { return; }
            }

            try {
                if (ImageIO.write(currentQRCodeImage, "PNG", fileToSave)) {
                     showSuccess("QR Code saved successfully to:\n" + fileToSave.getAbsolutePath());
                } else {
                     showError("Could not save QR Code (format writer not found).");
                }
            } catch (IOException ex) {
                showError("Error saving QR Code file: " + ex.getMessage());
            }
        }
    }


    private void addNewCategory(String categoryName) {
        try {
            productRepository.addNewCategory(categoryName); //
            showSuccess("Category '" + categoryName + "' added successfully.");
        } catch (SQLException e) {
            showError("Error adding category: " + e.getMessage());
        }
    }


    private BufferedImage toBufferedImage(BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, matrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
            }
        }
        return image;
    }

    /** Resets the QR display label to its initial state. */
    private void resetQrDisplay() {
        currentQRCodeImage = null;
        qrCodeDisplayLabel.setIcon(null);
        qrCodeDisplayLabel.setText("QR Code will appear here"); // Reset text
        qrCodeDisplayLabel.setFont(new Font("Arial", Font.ITALIC, 16)); // Reset font
        saveButton.setEnabled(false);
        qrCodeDisplayLabel.repaint();
    }


    /** Shows a standardized error message dialog. */
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /** Shows a standardized success message dialog. */
    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }
}