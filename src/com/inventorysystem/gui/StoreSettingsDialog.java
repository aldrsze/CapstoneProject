package com.inventorysystem.gui;

import com.inventorysystem.data.StoreRepository;
import com.inventorysystem.data.UserRepository;
import com.inventorysystem.model.Store;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;

// Dialog window for editing store details and default markup
public class StoreSettingsDialog extends JDialog {
    
    private final userFrame mainFrame;
    private final StoreRepository storeRepo;
    private final JLabel storeNameLabel;
    private final JLabel storeLocationLabel;
    private final JLabel storeContactLabel;
    
    // Input fields
    private JTextField nameField;
    private JTextField locationField;
    private JTextField contactField;
    private JTextField markupField;

    public StoreSettingsDialog(userFrame mainFrame, StoreRepository storeRepo,
                              JLabel nameLabel, JLabel locationLabel, JLabel contactLabel) {
        super(mainFrame, "Store Settings", true);
        this.mainFrame = mainFrame;
        this.storeRepo = storeRepo;
        this.storeNameLabel = nameLabel;
        this.storeLocationLabel = locationLabel;
        this.storeContactLabel = contactLabel;

        setupDialog();
        loadStoreData();
    }

    // Setup the dialog layout with tabs
    private void setupDialog() {
        setLayout(new BorderLayout());
        setSize(550, 450);
        setLocationRelativeTo(mainFrame);

        // Two tabs: Store Profile and Default Markup
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UIConstants.LABEL_BOLD_FONT);
        tabbedPane.setBorder(new EmptyBorder(10, 10, 10, 10));

        tabbedPane.addTab("Store Profile", createStoreProfilePanel());
        tabbedPane.addTab("Default Markup", createMarkupPanel());

        add(tabbedPane, BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    // Get store data from database and fill the fields
    private void loadStoreData() {
        try {
            Store currentStore = storeRepo.getStoreByUserId(mainFrame.loggedInUserId);
            if (currentStore == null) {
                JOptionPane.showMessageDialog(this, "Could not find store profile.", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                dispose();
                return;
            }

            // Fill in store info
            nameField.setText(currentStore.name());
            locationField.setText(currentStore.location());
            contactField.setText(currentStore.contact());

            // Fill in markup
            UserRepository userRepo = new UserRepository();
            double markup = userRepo.getDefaultMarkup(mainFrame.loggedInUserId);
            markupField.setText(String.valueOf(markup));

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading data: " + e.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }

    // Tab 1: Store profile fields (name, location, contact)
    private JPanel createStoreProfilePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UIConstants.FORM_COLOR);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;

        // Store name
        addLabel(panel, gbc, "Store Name:");
        nameField = addTextField(panel, gbc);

        // Location
        addLabel(panel, gbc, "Location:");
        locationField = addTextField(panel, gbc);

        // Contact number
        addLabel(panel, gbc, "Contact Number:");
        contactField = addTextField(panel, gbc);

        return panel;
    }

    // Tab 2: Default markup percentage field
    private JPanel createMarkupPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UIConstants.FORM_COLOR);
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;

        JLabel label = new JLabel("Default Markup Percentage (%):");
        label.setFont(UIConstants.LABEL_BOLD_FONT);
        label.setForeground(UIConstants.TEXT_PRIMARY);
        panel.add(label, gbc);

        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        markupField = new JTextField();
        markupField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        markupField.setBorder(new CompoundBorder(
            new LineBorder(Color.BLACK, 1, false),
            new EmptyBorder(10, 15, 10, 15)));
        markupField.setPreferredSize(new Dimension(200, 45));
        panel.add(markupField, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(5, 5, 8, 5);
        JLabel helpText = new JLabel("<html><i>Used when product has no specific markup</i></html>");
        helpText.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        helpText.setForeground(UIConstants.TEXT_SECONDARY);
        panel.add(helpText, gbc);

        return panel;
    }

    // Helper: Add a label to the panel
    private void addLabel(JPanel panel, GridBagConstraints gbc, String text) {
        gbc.gridy++;
        JLabel label = new JLabel(text);
        label.setFont(UIConstants.LABEL_BOLD_FONT);
        label.setForeground(UIConstants.TEXT_PRIMARY);
        panel.add(label, gbc);
    }

    // Helper: Add a text input field to the panel
    private JTextField addTextField(JPanel panel, GridBagConstraints gbc) {
        gbc.gridy++;
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        field.setPreferredSize(new Dimension(400, 50));
        field.setMinimumSize(new Dimension(350, 45));
        field.setBorder(new CompoundBorder(
            new LineBorder(Color.BLACK, 1, false),
            new EmptyBorder(12, 18, 12, 18)));
        panel.add(field, gbc);
        return field;
    }

    // Bottom buttons: Save and Cancel
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        panel.setBackground(UIConstants.FORM_COLOR);
        panel.setBorder(new CompoundBorder(
            new LineBorder(Color.BLACK, 1, false),
            new EmptyBorder(5, 10, 5, 10)));

        JButton saveBtn = createButton("Save Changes", UIConstants.SUCCESS_COLOR, UIConstants.SUCCESS_DARK);
        saveBtn.addActionListener(e -> saveChanges());

        JButton cancelBtn = createButton("Cancel", UIConstants.TEXT_SECONDARY, new Color(100, 110, 120));
        cancelBtn.addActionListener(e -> dispose());

        panel.add(cancelBtn);
        panel.add(saveBtn);

        return panel;
    }

    // Create a styled button with hover effect
    private JButton createButton(String text, Color bgColor, Color hoverColor) {
        JButton button = new JButton(text);
        button.setFont(UIConstants.BUTTON_FONT);
        button.setPreferredSize(new Dimension(140, 38));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { button.setBackground(hoverColor); }
            public void mouseExited(MouseEvent e) { button.setBackground(bgColor); }
        });

        return button;
    }

    // Save all changes to database
    private void saveChanges() {
        String newName = nameField.getText().trim();
        String newLocation = locationField.getText().trim();
        String newContact = contactField.getText().trim();

        // Name is required
        if (newName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Store Name cannot be empty.",
                "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // Save store details
            storeRepo.updateStore(mainFrame.loggedInUserId, newName, newLocation, newContact);
            
            // Update the labels in navigation sidebar
            storeNameLabel.setText(newName);
            storeLocationLabel.setText(newLocation.isEmpty() ? "No location" : newLocation);
            storeContactLabel.setText(newContact.isEmpty() ? "No contact" : newContact);

            // Save markup percentage
            double newMarkup = Double.parseDouble(markupField.getText().trim());
            if (newMarkup < 0) {
                JOptionPane.showMessageDialog(this, "Markup cannot be negative.",
                    "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            UserRepository userRepo = new UserRepository();
            userRepo.updateDefaultMarkup(mainFrame.loggedInUserId, newMarkup);

            JOptionPane.showMessageDialog(this, "Settings saved successfully!",
                "Success", JOptionPane.INFORMATION_MESSAGE);
            dispose();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid markup percentage.",
                "Input Error", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error saving: " + ex.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
