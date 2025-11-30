package com.inventorysystem.gui;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.json.JSONObject;
import org.json.JSONException;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.font.TextAttribute;
import java.awt.geom.Rectangle2D;
import java.text.AttributedString;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.inventorysystem.data.ProductRepository;
import com.inventorysystem.model.Category;

// Panel for creating QR codes with product info (Admin only)
public class QRCodePanel extends JPanel {

    // Input fields for product details
    private JTextField productNameField;
    private JTextField costPriceField;
    private JTextField stockField;
    private JComboBox<String> categoryComboBox;
    
    // QR code display
    private JLabel qrCodeDisplayLabel;
    private JButton saveButton;
    private BufferedImage currentQRCodeImage;

    // Database and user info
    private final Map<String, Integer> categoryIdMap = new HashMap<>();
    private final int userId;
    private final ProductRepository productRepository;
    private final String userRole;

    public QRCodePanel(userFrame mainFrame) {
        this.userId = mainFrame.loggedInUserId;
        this.userRole = mainFrame.loggedInUserRole;
        this.productRepository = new ProductRepository(this.userId);

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(15, 15, 15, 15));
        setBackground(UIConstants.BACKGROUND_COLOR);

        // Employees can't access this - show error message
        if (userRole.equalsIgnoreCase("Employee")) {
            displayAccessDenied();
            return;
        }

        // Admins get full QR code functionality
        initializeQRCodePanel();
    }

    // Show "Access Denied" message for employees
    private void displayAccessDenied() {
        removeAll();
        
        JPanel restrictedPanel = new JPanel(new GridBagLayout());
        restrictedPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        
        JPanel messageBox = new JPanel();
        messageBox.setLayout(new BoxLayout(messageBox, BoxLayout.Y_AXIS));
        messageBox.setBackground(Color.WHITE);
        messageBox.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(Color.BLACK, 3, false),
            new EmptyBorder(50, 70, 50, 70)
        ));
        
        // Access denied icon
        JLabel iconLabel = new JLabel("ACCESS DENIED");
        iconLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        iconLabel.setForeground(UIConstants.DANGER_COLOR);
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        messageBox.add(iconLabel);
        
        messageBox.add(Box.createRigidArea(new Dimension(0, 25)));
        
        // Red title
        JLabel titleLabel = new JLabel("Access Restricted");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        titleLabel.setForeground(UIConstants.DANGER_COLOR);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        messageBox.add(titleLabel);
        
        messageBox.add(Box.createRigidArea(new Dimension(0, 20)));
        
        // Explanation message
        JLabel messageLabel = new JLabel("<html><center>" +
            "QR Code generation is available to Administrators only.<br><br>" +
            "Please contact your system administrator<br>to generate QR codes for products." +
            "</center></html>");
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        messageLabel.setForeground(UIConstants.TEXT_SECONDARY);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        messageBox.add(messageLabel);
        
        restrictedPanel.add(messageBox);
        add(restrictedPanel, BorderLayout.CENTER);
    }

    // Setup the full QR code panel for admins
    private void initializeQRCodePanel() {
        removeAll();
        
        JLabel titleLabel = new JLabel("Generate Product QR Code", SwingConstants.CENTER);
        titleLabel.setFont(UIConstants.TITLE_FONT);
        titleLabel.setBorder(new EmptyBorder(0, 0, 15, 0));
        add(titleLabel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBackground(UIConstants.BACKGROUND_COLOR);

        GridBagConstraints gbcContent = new GridBagConstraints();
        gbcContent.insets = new Insets(0, 10, 0, 10);
        gbcContent.fill = GridBagConstraints.NONE;
        gbcContent.anchor = GridBagConstraints.CENTER;

        // Left side: input form
        JPanel formPanel = createFormPanel();
        gbcContent.gridx = 0;
        gbcContent.gridy = 0;
        contentPanel.add(formPanel, gbcContent);

        // Right side: QR code display
        JPanel qrDisplayPanel = createQrDisplayPanel();
        gbcContent.gridx = 1;
        gbcContent.gridy = 0;
        contentPanel.add(qrDisplayPanel, gbcContent);

        add(contentPanel, BorderLayout.CENTER);

        // Load data when panel is shown
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                loadCategories();
                setUIForAddUpdateMode();
            }
        });

        loadCategories();
        setUIForAddUpdateMode();
    }

    // Left panel: product info form
    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(UIConstants.FORM_COLOR);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(Color.BLACK, 2, false),
            new EmptyBorder(25, 25, 25, 25)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // "Product Information" title
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel formTitle = new JLabel("Product Information");
        formTitle.setFont(UIConstants.SUBTITLE_FONT);
        formTitle.setForeground(UIConstants.PRIMARY_COLOR);
        formTitle.setBorder(new EmptyBorder(0, 0, 15, 0));
        formPanel.add(formTitle, gbc);
        
        gbc.gridwidth = 1;

        // Product Name
        gbc.gridy = 1;
        gbc.gridx = 0;
        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setFont(UIConstants.LABEL_BOLD_FONT);
        nameLabel.setForeground(UIConstants.TEXT_PRIMARY);
        formPanel.add(nameLabel, gbc);
        
        gbc.gridx = 1;
        productNameField = createStyledTextField();
        formPanel.add(productNameField, gbc);

        // Category dropdown with + button
        gbc.gridy = 2;
        gbc.gridx = 0;
        JLabel categoryLabel = new JLabel("Category:");
        categoryLabel.setFont(UIConstants.LABEL_BOLD_FONT);
        categoryLabel.setForeground(UIConstants.TEXT_PRIMARY);
        formPanel.add(categoryLabel, gbc);
        
        gbc.gridx = 1;
        JPanel categoryPanel = new JPanel(new BorderLayout(5, 0));
        categoryPanel.setBackground(UIConstants.FORM_COLOR);
        categoryComboBox = new JComboBox<>();
        categoryComboBox.setFont(UIConstants.INPUT_FONT);
        categoryComboBox.setPreferredSize(new Dimension(180, UIConstants.INPUT_HEIGHT));
        categoryPanel.add(categoryComboBox, BorderLayout.CENTER);
        
        JButton addCategoryButton = new JButton("+");
        addCategoryButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        addCategoryButton.setPreferredSize(new Dimension(40, UIConstants.INPUT_HEIGHT));
        addCategoryButton.setBackground(UIConstants.PRIMARY_COLOR);
        addCategoryButton.setForeground(Color.WHITE);
        addCategoryButton.setFocusPainted(false);
        addCategoryButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addCategoryButton.setToolTipText("Add a new category");
        categoryPanel.add(addCategoryButton, BorderLayout.EAST);
        formPanel.add(categoryPanel, gbc);

        // Cost Price
        gbc.gridy = 3;
        gbc.gridx = 0;
        JLabel costLabel = new JLabel("Cost Price:");
        costLabel.setFont(UIConstants.LABEL_BOLD_FONT);
        costLabel.setForeground(UIConstants.TEXT_PRIMARY);
        formPanel.add(costLabel, gbc);
        
        gbc.gridx = 1;
        costPriceField = createStyledTextField();
        formPanel.add(costPriceField, gbc);

        // Stock quantity
        gbc.gridy = 4;
        gbc.gridx = 0;
        JLabel stockLabel = new JLabel("Stock:");
        stockLabel.setFont(UIConstants.LABEL_BOLD_FONT);
        stockLabel.setForeground(UIConstants.TEXT_PRIMARY);
        formPanel.add(stockLabel, gbc);
        
        gbc.gridx = 1;
        stockField = createStyledTextField();
        stockField.setText("1");
        formPanel.add(stockField, gbc);

        // Bottom buttons
        gbc.gridy = 5;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(25, 10, 10, 10);
        gbc.anchor = GridBagConstraints.CENTER;
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setBackground(UIConstants.FORM_COLOR);
        
        JButton refreshButton = createEmphasizedButton("Clear Form", UIConstants.TEXT_SECONDARY, UIConstants.TEXT_PRIMARY, 140, 45);
        JButton generateButton = createEmphasizedButton("Generate QR Code", UIConstants.PRIMARY_COLOR, UIConstants.PRIMARY_DARK, 180, 45);
        
        buttonPanel.add(refreshButton);
        buttonPanel.add(generateButton);
        formPanel.add(buttonPanel, gbc);

        // Button actions
        addCategoryButton.addActionListener(e -> handleAddNewCategory());
        generateButton.addActionListener(e -> generateAndDisplayQRCode());
        refreshButton.addActionListener(e -> refreshForm());

        return formPanel;
    }

    // Create a text input field with styling
    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setFont(UIConstants.INPUT_FONT);
        field.setPreferredSize(new Dimension(200, UIConstants.INPUT_HEIGHT));
        field.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(Color.BLACK, 1, false),
            new EmptyBorder(8, 12, 8, 12)
        ));
        return field;
    }

    // Right panel: QR code display and save button
    private JPanel createQrDisplayPanel() {
        JPanel qrPanel = new JPanel(new BorderLayout(15, 15));
        qrPanel.setBackground(UIConstants.FORM_COLOR);
        qrPanel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(Color.BLACK, 2, false),
            new EmptyBorder(20, 20, 20, 20)
        ));

        // Title
        JLabel titleLabel = new JLabel("QR Code Output", SwingConstants.CENTER);
        titleLabel.setFont(UIConstants.SUBTITLE_FONT);
        titleLabel.setForeground(UIConstants.PRIMARY_COLOR);
        titleLabel.setBorder(new EmptyBorder(0, 0, 15, 0));
        qrPanel.add(titleLabel, BorderLayout.NORTH);

        // QR display area
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(UIConstants.FORM_COLOR);
        centerWrapper.setBorder(new LineBorder(Color.BLACK, 2, false));

        qrCodeDisplayLabel = new JLabel("QR Code will appear here", SwingConstants.CENTER);
        qrCodeDisplayLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        qrCodeDisplayLabel.setForeground(UIConstants.TEXT_SECONDARY);
        qrCodeDisplayLabel.setPreferredSize(new Dimension(350, 350));
        qrCodeDisplayLabel.setMinimumSize(new Dimension(350, 350));
        qrCodeDisplayLabel.setMaximumSize(new Dimension(350, 350));
        qrCodeDisplayLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        centerWrapper.add(qrCodeDisplayLabel, new GridBagConstraints());
        qrPanel.add(centerWrapper, BorderLayout.CENTER);

        // Save button at bottom
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(UIConstants.FORM_COLOR);
        buttonPanel.setBorder(new EmptyBorder(15, 0, 0, 0));

        saveButton = createEmphasizedButton("Save QR Code", UIConstants.SUCCESS_COLOR, UIConstants.SUCCESS_DARK, 200, 42);
        saveButton.setEnabled(false);
        saveButton.addActionListener(e -> handleSaveQRCode());

        buttonPanel.add(saveButton);
        qrPanel.add(buttonPanel, BorderLayout.SOUTH);

        return qrPanel;
    }

    // Create styled button with hover effect
    private JButton createEmphasizedButton(String text, Color bgColor, Color hoverColor, int width, int height) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setPreferredSize(new Dimension(width, height));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(bgColor.darker(), 2, false),
            new EmptyBorder(8, 16, 8, 16)
        ));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(hoverColor);
                    button.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(hoverColor.darker(), 2, false),
                        new EmptyBorder(8, 16, 8, 16)
                    ));
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
                button.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(bgColor.darker(), 2, false),
                    new EmptyBorder(8, 16, 8, 16)
                ));
            }
        });

        return button;
    }

    // Add new category to database
    private void handleAddNewCategory() {
        String newCategory = JOptionPane.showInputDialog(this, "Enter new category name:", "Add Category", JOptionPane.PLAIN_MESSAGE);
        if (newCategory != null && !newCategory.trim().isEmpty()) {
            addNewCategoryToDB(newCategory);
            loadCategories();
            categoryComboBox.setSelectedItem(newCategory);
        }
    }

    // Setup form for adding products
    private void setUIForAddUpdateMode() {
        productNameField.setEnabled(true);
        categoryComboBox.setEnabled(!categoryIdMap.isEmpty());
        costPriceField.setEnabled(true);
        stockField.setEnabled(true);
        stockField.setBackground(Color.WHITE);
        stockField.setEditable(true);

        stockField.setText("1");

        resetQrDisplay();
    }

    // Clear all input fields
    private void refreshForm() {
        productNameField.setText("");
        costPriceField.setText("");
        stockField.setText("1");
        categoryComboBox.setSelectedIndex(categoryIdMap.isEmpty() ? -1 : 0);

        setUIForAddUpdateMode();
    }

    // Get categories from database and fill dropdown
    private void loadCategories() {
        String currentSelection = (String) categoryComboBox.getSelectedItem();
        categoryComboBox.removeAllItems();
        categoryIdMap.clear();

        try {
            List<Category> categories = productRepository.getCategories();
            if (categories.isEmpty()) {
                categoryComboBox.addItem("<No categories added>");
                categoryComboBox.setEnabled(false);
            } else {
                categoryComboBox.setEnabled(true);
                for (Category category : categories) {
                    categoryIdMap.put(category.name(), category.id());
                    categoryComboBox.addItem(category.name());
                }
                if (currentSelection != null && categoryIdMap.containsKey(currentSelection)) {
                    categoryComboBox.setSelectedItem(currentSelection);
                } else {
                     categoryComboBox.setSelectedIndex(0);
                }
            }
        } catch (SQLException e) {
            showError("Could not load categories: " + e.getMessage());
            categoryComboBox.addItem("<Error loading>");
            categoryComboBox.setEnabled(false);
        } finally {
             categoryComboBox.setEnabled(!categoryIdMap.isEmpty());
        }
    }

    // Create QR code image from form data
    private void generateAndDisplayQRCode() {
        String qrText;
        Map<String, String> detailsMap = new HashMap<>();

        try {
            qrText = generateProductJsonForQR(detailsMap);
            if (qrText == null) {
                resetQrDisplay();
                return;
            }

            // Generate QR code image (280x280 pixels)
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            final int qrCodeSize = 280;
            BitMatrix bitMatrix = qrCodeWriter.encode(qrText, BarcodeFormat.QR_CODE, qrCodeSize, qrCodeSize);
            BufferedImage qrImage = toBufferedImage(bitMatrix);

            // Add product details below QR code
            int textHeight = 65;
            int combinedHeight = qrCodeSize + textHeight;
            BufferedImage combinedImage = new BufferedImage(qrCodeSize, combinedHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = combinedImage.createGraphics();

            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, qrCodeSize, combinedHeight);

            g2d.drawImage(qrImage, 0, 0, null);

            // Draw product info text
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Monospaced", Font.PLAIN, 12));
            FontMetrics fm = g2d.getFontMetrics();
            int lineHeight = fm.getHeight();
            int startY = qrCodeSize + 20;
            int currentY = startY;
            int paddingX = 10;

            drawDetailLine(g2d, "Name:", detailsMap.getOrDefault("Name", "N/A"), paddingX, currentY);
            currentY += lineHeight;
            drawDetailLine(g2d, "Category:", detailsMap.getOrDefault("Category", "N/A"), paddingX, currentY);
            currentY += lineHeight;
            drawDetailLine(g2d, "Cost:", detailsMap.getOrDefault("Cost Price", "N/A"), paddingX, currentY);
            currentY += lineHeight;

            g2d.dispose();

            currentQRCodeImage = combinedImage;

            qrCodeDisplayLabel.setIcon(new ImageIcon(currentQRCodeImage));
            qrCodeDisplayLabel.setText(null);
            qrCodeDisplayLabel.setFont(null);
            saveButton.setEnabled(true);

        } catch (NumberFormatException ex) {
            showError("Cost Price and Stock must be valid numbers."); 
            resetQrDisplay();
        } catch (WriterException ex) {
            showError("Could not generate QR code image: " + ex.getMessage()); 
            resetQrDisplay();
        } catch (JSONException e) {
            showError("Could not build JSON data for QR code: " + e.getMessage()); 
            resetQrDisplay();
        } catch (Exception ex) {
            showError("An unexpected error occurred during QR generation: " + ex.getMessage()); 
            resetQrDisplay();
            ex.printStackTrace();
        }
    }

    // Draw one line of product info (label + value)
    private void drawDetailLine(Graphics2D g2d, String label, String value, int x, int y) {
        AttributedString asLabel = new AttributedString(String.format("%-10s", label));
        asLabel.addAttribute(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD, 0, label.length());
        g2d.drawString(asLabel.getIterator(), x, y);

        FontMetrics fm = g2d.getFontMetrics();
        Rectangle2D labelBounds = fm.getStringBounds(String.format("%-10s", label), g2d);
        g2d.drawString(value, x + (int)labelBounds.getWidth() + 5, y);
    }

    // Convert form fields to JSON string for QR code
    private String generateProductJsonForQR(Map<String, String> detailsMap) throws NumberFormatException, JSONException {
        detailsMap.clear();

        String name = productNameField.getText().trim();
        String selectedCategory = (String) categoryComboBox.getSelectedItem();
        String costPriceStr = costPriceField.getText().trim();
        String stockStr = stockField.getText().trim();

        // Check all fields are filled (ID is no longer required - auto-assigned on scan)
        if (name.isEmpty() || selectedCategory == null || selectedCategory.startsWith("<") ||
            costPriceStr.isEmpty() || stockStr.isEmpty()) {
            showError("Required fields are: Name, Category, Cost Price, and Stock.");
            return null;
        }

        double costPrice = Double.parseDouble(costPriceStr);
        int stock = Integer.parseInt(stockStr);
        int categoryId = categoryIdMap.getOrDefault(selectedCategory, -1);

        if (categoryId == -1) { 
            showError("Invalid category selected."); 
            return null; 
        }
        if (costPrice < 0 || stock < 0) { 
            showError("Cost Price and Stock cannot be negative."); 
            return null; 
        }
        
        // Save for display below QR code (ID will be auto-assigned when scanned)
        detailsMap.put("Name", name);
        detailsMap.put("Category", selectedCategory);
        detailsMap.put("Cost Price", String.format("₱%,.2f", costPrice));

        // Build JSON for QR code (without product ID - will be auto-assigned on scan)
        JSONObject dataObject = new JSONObject();
        dataObject.put("name", name);
        dataObject.put("category_id", categoryId);
        dataObject.put("cost_price", costPrice);
        dataObject.put("stock", stock);

        JSONObject mainObject = new JSONObject();
        mainObject.put("action", "create_product");
        mainObject.put("data", dataObject);

        return mainObject.toString();
    }

    // Save QR code image to file
    private void handleSaveQRCode() {
        if (currentQRCodeImage == null) {
            showError("Please generate a QR code first.");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save QR Code As");

        // Suggest filename based on product name
        String productName = productNameField.getText().trim().replaceAll("[^a-zA-Z0-9.-]", "_");
        String defaultFileName = "product_qrcode.png";

        if (!productName.isEmpty()){
            defaultFileName = productName + "_QR.png";
        }

        fileChooser.setSelectedFile(new File(defaultFileName));
        fileChooser.setFileFilter(new FileNameExtensionFilter("PNG Images (*.png)", "png"));

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();

            // Add .png if missing
            if (!fileToSave.getName().toLowerCase().endsWith(".png")) {
                fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".png");
            }

            // Ask before overwriting
            if (fileToSave.exists()) {
                int overwrite = JOptionPane.showConfirmDialog(this,
                    "File '" + fileToSave.getName() + "' already exists. Overwrite?",
                    "Confirm Overwrite", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (overwrite != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            try {
                if (ImageIO.write(currentQRCodeImage, "PNG", fileToSave)) {
                     showSuccess("QR Code saved successfully to:\n" + fileToSave.getAbsolutePath());
                } else {
                     showError("Could not save QR Code (ImageIO writer for PNG not found).");
                }
            } catch (IOException ex) {
                showError("Error saving QR Code file: " + ex.getMessage());
            }
        }
    }

    // Save category to database
    private void addNewCategoryToDB(String categoryName) {
        try {
            productRepository.addNewCategory(categoryName);
            showSuccess("Category '" + categoryName + "' added successfully.");
        } catch (SQLException e) {
            showError("Error adding category: " + e.getMessage());
        }
    }

    // Convert QR matrix to image
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

    // Clear QR display area
    private void resetQrDisplay() {
        currentQRCodeImage = null;
        qrCodeDisplayLabel.setIcon(null);
        qrCodeDisplayLabel.setText("QR Code will appear here");
        qrCodeDisplayLabel.setFont(new Font("Arial", Font.ITALIC, 16));
        saveButton.setEnabled(false);
        qrCodeDisplayLabel.repaint();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }
}