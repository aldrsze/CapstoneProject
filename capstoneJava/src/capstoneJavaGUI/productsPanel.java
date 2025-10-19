// ProductsPanel.java
package capstoneJavaGUI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class productsPanel extends JPanel {

    public productsPanel() {
        setLayout(null); // Use null layout for precise component placement
        setBackground(Color.WHITE);

        // --- TITLE ---
        JLabel titleLabel = new JLabel("MANAGE PRODUCTS AND STOCKS");
        titleLabel.setBounds(30, 20, 400, 30);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        add(titleLabel);

        // --- PRODUCTS TABLE ---
        String[] columnNames = {"Product id", "Product name", "Category", "Price", "Stock"};
        Object[][] data = {}; // Empty data
        DefaultTableModel model = new DefaultTableModel(data, columnNames);
        JTable productsTable = new JTable(model);
        JScrollPane tableScrollPane = new JScrollPane(productsTable);
        tableScrollPane.setBounds(30, 60, 450, 450);
        add(tableScrollPane);

        // --- ADD/EDIT PRODUCTS FORM ---
        JLabel addEditLabel = new JLabel("ADD/EDIT PRODUCTS");
        addEditLabel.setBounds(520, 60, 200, 25);
        addEditLabel.setFont(new Font("Arial", Font.BOLD, 14));
        add(addEditLabel);

        // Labels
        String[] formLabels = {"PRODUCT ID:", "PRODUCT NAME:", "CATEGORY:", "PRICE:", "STOCK QUANTITY:"};
        for (int i = 0; i < formLabels.length; i++) {
            JLabel label = new JLabel(formLabels[i]);
            label.setBounds(520, 100 + (i * 30), 120, 25);
            add(label);
        }

        // Text Fields
        for (int i = 0; i < 5; i++) {
            JTextField textField = new JTextField();
            textField.setBounds(650, 100 + (i * 30), 120, 25);
            add(textField);
        }

        // Buttons
        JButton addButton = new JButton("ADD NEW");
        addButton.setBounds(520, 260, 150, 25);
        add(addButton);

        JButton saveButton = new JButton("SAVE CHANGES");
        saveButton.setBounds(520, 295, 150, 25);
        add(saveButton);

        JButton deleteButton = new JButton("DELETE SELECTED");
        deleteButton.setBounds(520, 330, 150, 25);
        add(deleteButton);


        // --- SELL PRODUCTS FORM ---
        JLabel sellLabel = new JLabel("SELL PRODUCTS");
        sellLabel.setBounds(520, 380, 200, 25);
        sellLabel.setFont(new Font("Arial", Font.BOLD, 14));
        add(sellLabel);

        // Labels and Text Fields for selling
        JLabel priceLabel = new JLabel("SELLING PRICE:");
        priceLabel.setBounds(520, 415, 100, 25);
        add(priceLabel);
        JTextField priceField = new JTextField();
        priceField.setBounds(650, 415, 120, 25);
        add(priceField);

        JLabel quantityLabel = new JLabel("QUANTITY:");
        quantityLabel.setBounds(520, 450, 100, 25);
        add(quantityLabel);
        JTextField quantityField = new JTextField();
        quantityField.setBounds(650, 450, 120, 25);
        add(quantityField);

        // Sell Button
        JButton sellButton = new JButton("SELL SELECTED");
        sellButton.setBounds(520, 490, 150, 25);
        add(sellButton);
    }
}