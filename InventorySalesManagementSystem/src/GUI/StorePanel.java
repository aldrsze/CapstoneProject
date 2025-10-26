package GUI;

import Database.DatabaseConnection;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StorePanel extends JPanel {

    private int userId;
    private JPanel storeListPanel;
    private JTextField storeNameField;
    private JTextField locationField;
    private JTextField contactField;
    private userFrame mainFrame; // Changed to userFrame

    public StorePanel(userFrame mainFrame, int userId) { // Changed constructor
        this.userId = userId;
        this.mainFrame = mainFrame;
        
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setBackground(Color.WHITE);

        // Title
        JLabel titleLabel = new JLabel("Manage Your Stores", SwingConstants.CENTER);
        titleLabel.setFont(UIConstants.TITLE_FONT);
        add(titleLabel, BorderLayout.NORTH);

        // Store List Panel
        storeListPanel = new JPanel();
        storeListPanel.setLayout(new BoxLayout(storeListPanel, BoxLayout.Y_AXIS));
        storeListPanel.setBackground(Color.WHITE);
        JScrollPane scrollPane = new JScrollPane(storeListPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Your Stores"));

        // Add Store Panel
        JPanel addStorePanel = createAddStorePanel();

        // Main Layout
        add(scrollPane, BorderLayout.CENTER);
        add(addStorePanel, BorderLayout.SOUTH);

        loadStores();
    }
    
    private JPanel createAddStorePanel() {
        JPanel addStorePanel = new JPanel(new GridBagLayout());
        addStorePanel.setBorder(BorderFactory.createTitledBorder("Add a New Store"));
        addStorePanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        addStorePanel.add(new JLabel("Store Name:"), gbc);
        gbc.gridx = 1; storeNameField = new JTextField(20);
        addStorePanel.add(storeNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        addStorePanel.add(new JLabel("Location/Address:"), gbc);
        gbc.gridx = 1; locationField = new JTextField(20);
        addStorePanel.add(locationField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        addStorePanel.add(new JLabel("Contact Number:"), gbc);
        gbc.gridx = 1; contactField = new JTextField(20);
        addStorePanel.add(contactField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton addStoreButton = new JButton("Add Store");
        addStoreButton.addActionListener(e -> addStore());
        addStorePanel.add(addStoreButton, gbc);

        return addStorePanel;
    }

    private void loadStores() {
        storeListPanel.removeAll();
        String sql = "SELECT store_id, store_name, location, contact_number FROM stores WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (!rs.isBeforeFirst()) {
                JLabel noStoresLabel = new JLabel("You haven't added any stores yet.", SwingConstants.CENTER);
                noStoresLabel.setFont(new Font("SansSerif", Font.ITALIC, 16));
                noStoresLabel.setForeground(Color.GRAY);
                storeListPanel.add(noStoresLabel);
            } else {
                while (rs.next()) {
                    int storeId = rs.getInt("store_id");
                    String storeName = rs.getString("store_name");
                    String location = rs.getString("location");
                    String contact = rs.getString("contact_number");

                    storeListPanel.add(createStoreEntryPanel(storeId, storeName, location, contact));
                    storeListPanel.add(Box.createRigidArea(new Dimension(0, 5)));
                }
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading stores: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }

        storeListPanel.revalidate();
        storeListPanel.repaint();
    }
    
    private JPanel createStoreEntryPanel(int storeId, String name, String location, String contact) {
        JPanel entryPanel = new JPanel(new BorderLayout(15, 0));
        entryPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
            new EmptyBorder(10, 10, 10, 10)
        ));
        entryPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JLabel infoLabel = new JLabel("<html><b>" + name + "</b><br><small><i>" + (location != null ? location : "") + " | " + (contact != null ? contact : "") + "</i></small></html>");
        infoLabel.setVerticalAlignment(SwingConstants.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton selectButton = new JButton("Select");
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");

        selectButton.addActionListener(e -> mainFrame.showMainApplication(storeId));
        editButton.addActionListener(e -> editStore(storeId, name, location, contact));
        deleteButton.addActionListener(e -> deleteStore(storeId, name));

        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(selectButton);

        entryPanel.add(infoLabel, BorderLayout.CENTER);
        entryPanel.add(buttonPanel, BorderLayout.EAST);

        return entryPanel;
    }

    private void addStore() {
        String storeName = storeNameField.getText();
        String location = locationField.getText();
        String contact = contactField.getText();

        if (storeName.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Store Name is required.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = "INSERT INTO stores (user_id, store_name, location, contact_number) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setString(2, storeName);
            pstmt.setString(3, location);
            pstmt.setString(4, contact);

            if (pstmt.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(this, "Store added successfully!");
                storeNameField.setText("");
                locationField.setText("");
                contactField.setText("");
                loadStores();
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error adding store: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editStore(int storeId, String currentName, String currentLocation, String currentContact) {
        JTextField nameField = new JTextField(currentName);
        JTextField locField = new JTextField(currentLocation);
        JTextField contactField = new JTextField(currentContact);

        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        panel.add(new JLabel("Store Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Location:"));
        panel.add(locField);
        panel.add(new JLabel("Contact:"));
        panel.add(contactField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Edit Store", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String newName = nameField.getText();
            if (newName.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Store Name cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String sql = "UPDATE stores SET store_name = ?, location = ?, contact_number = ? WHERE store_id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, newName);
                pstmt.setString(2, locField.getText());
                pstmt.setString(3, contactField.getText());
                pstmt.setInt(4, storeId);
                pstmt.executeUpdate();
                loadStores();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error updating store: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteStore(int storeId, String storeName) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete the store '" + storeName + "'?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            String sql = "DELETE FROM stores WHERE store_id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, storeId);
                pstmt.executeUpdate();
                loadStores();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error deleting store: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}