package com.inventorysystem.gui;

import com.inventorysystem.data.StoreRepository;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// Main panel - navigation sidebar and content area
public class MainApplicationPanel extends JPanel {
    
    // State
    private final userFrame mainFrame;
    private final String userRole;
    private final StoreRepository storeRepository;
    
    // UI components
    private final CardLayout cardLayout;
    private final JPanel mainContentPanel;
    private JLabel storeNameLabel;
    private JLabel storeLocationLabel;
    private JLabel storeContactLabel;
    private JButton currentSelectedButton;

    public MainApplicationPanel(userFrame mainFrame, String username, String role, 
                                String storeName, String storeLocation, String storeContact) {
        this.mainFrame = mainFrame;
        this.userRole = role;
        this.storeRepository = new StoreRepository();
        this.cardLayout = new CardLayout();
        
        setLayout(new BorderLayout());

        // Left side - navigation menu
        JPanel navPanel = createNavigationPanel(username, role, storeName, storeLocation, storeContact);
        add(navPanel, BorderLayout.WEST);

        // Right side - main content area (changes based on what button you click)
        mainContentPanel = new JPanel(cardLayout);
        mainContentPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        
        mainContentPanel.add(new dashboardPanel(mainFrame), "Dashboard");
        mainContentPanel.add(new productsPanel(mainFrame), "Products");
        mainContentPanel.add(new stockPanel(mainFrame), "Stocks");
        mainContentPanel.add(new recordsPanel(mainFrame), "Records");
        mainContentPanel.add(new AboutPanel(), "About");
        
        // Wrap the main content panel in a JScrollPane
        JScrollPane contentScrollPane = new JScrollPane(mainContentPanel);
        contentScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        contentScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        // Make the scroll pane blend in by removing its border
        contentScrollPane.setBorder(BorderFactory.createEmptyBorder()); 
        
        // Add the scroll pane to the center instead of the panel directly
        add(contentScrollPane, BorderLayout.CENTER);
        
        // Show dashboard first
        cardLayout.show(mainContentPanel, "Dashboard");
    }

    // ========== NAVIGATION PANEL SETUP ==========
    
    private JPanel createNavigationPanel(String username, String role, String storeName, 
                                        String storeLocation, String storeContact) {
        JPanel navPanel = new JPanel(new BorderLayout());
        navPanel.setPreferredSize(new Dimension(260, 0));
        navPanel.setBackground(UIConstants.NAV_BACKGROUND);
        navPanel.setBorder(new LineBorder(Color.BLACK, 1, false));

        // Top: store and user info
        navPanel.add(createStoreInfoSection(storeName, storeLocation, storeContact, username, role), 
                    BorderLayout.NORTH);
        
        // Middle: navigation buttons
        navPanel.add(createNavigationButtons(), BorderLayout.CENTER);
        
        // Bottom: action buttons (settings, logout)
        navPanel.add(createActionButtons(), BorderLayout.SOUTH);

        return navPanel;
    }

    // Store info at the top of navigation
    private JPanel createStoreInfoSection(String storeName, String storeLocation, 
                                         String storeContact, String username, String role) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UIConstants.NAV_BACKGROUND);
        panel.setBorder(new EmptyBorder(25, 20, 25, 20));

        // "CURRENT STORE" label
        JLabel titleLabel = new JLabel("CURRENT STORE");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLabel.setForeground(new Color(189, 195, 199));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(titleLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Store name (big and bold)
        storeNameLabel = new JLabel(storeName);
        storeNameLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        storeNameLabel.setForeground(Color.WHITE);
        storeNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(storeNameLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 6)));

        // Location
        storeLocationLabel = new JLabel(storeLocation.isEmpty() ? "No location" : storeLocation);
        storeLocationLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        storeLocationLabel.setForeground(new Color(206, 214, 224));
        storeLocationLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(storeLocationLabel);

        // Contact number
        storeContactLabel = new JLabel(storeContact.isEmpty() ? "No contact" : storeContact);
        storeContactLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        storeContactLabel.setForeground(new Color(206, 214, 224));
        storeContactLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(storeContactLabel);

        panel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Line separator
        JSeparator separator = new JSeparator();
        separator.setForeground(new Color(127, 140, 141));
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        panel.add(separator);

        panel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Username
        JLabel userLabel = new JLabel(username);
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        userLabel.setForeground(Color.WHITE);
        userLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(userLabel);

        // Role badge (Admin/Employee)
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(createRoleBadge(role));

        return panel;
    }

    // Small colored box showing role
    private JPanel createRoleBadge(String role) {
        JPanel badge = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        badge.setMaximumSize(new Dimension(140, 32));
        badge.setPreferredSize(new Dimension(140, 32));
        badge.setBackground(UIConstants.PRIMARY_COLOR);
        badge.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.BLACK, 1, false),
            new EmptyBorder(6, 15, 6, 15)
        ));
        badge.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel roleLabel = new JLabel(role.toUpperCase());
        roleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        roleLabel.setForeground(Color.WHITE);
        badge.add(roleLabel);

        return badge;
    }

    // Main navigation buttons (Dashboard, Products, etc.)
    private JPanel createNavigationButtons() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UIConstants.NAV_BACKGROUND);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Create all buttons
        JButton dashboardBtn = createNavButton("Dashboard", "Dashboard");
        JButton productsBtn = createNavButton("Products", "Products");
        JButton stocksBtn = createNavButton("Stocks", "Stocks");
        JButton recordsBtn = createNavButton("Records", "Records");
        JButton aboutBtn = createNavButton("About", "About");

        // Add buttons with spacing
        panel.add(dashboardBtn);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(productsBtn);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(stocksBtn);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(recordsBtn);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(aboutBtn);
        panel.add(Box.createVerticalGlue());

        // Dashboard is selected by default
        currentSelectedButton = dashboardBtn;
        updateButtonSelection(dashboardBtn);

        return panel;
    }

    // Create a single navigation button
    private JButton createNavButton(String text, String panelName) {
        // Nav buttons start transparent (opaque=false) and use NAV_HOVER color
        JButton button = createBaseButton(text, UIConstants.NAV_BACKGROUND, new Color(206, 214, 224), UIConstants.NAV_HOVER, false);

        // Specific action for Nav buttons
        button.addActionListener(e -> {
            cardLayout.show(mainContentPanel, panelName);
            updateButtonSelection(button);
        });

        return button;
    }

    // Highlight the selected button
    private void updateButtonSelection(JButton selectedButton) {
        // Reset old button
        if (currentSelectedButton != null) {
            currentSelectedButton.setForeground(new Color(206, 214, 224));
            currentSelectedButton.setBackground(UIConstants.NAV_BACKGROUND);
            currentSelectedButton.setOpaque(false);
        }
        
        // Highlight new button
        currentSelectedButton = selectedButton;
        selectedButton.setForeground(Color.WHITE);
        selectedButton.setBackground(UIConstants.ACCENT_COLOR);
        selectedButton.setOpaque(true);
    }

    // Bottom action buttons (Store Settings, Logout, etc.)
    private JPanel createActionButtons() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UIConstants.NAV_BACKGROUND);
        panel.setBorder(new EmptyBorder(10, 10, 15, 10));

        // Only admins can see these buttons
        if (userRole.equalsIgnoreCase("Admin")) {
            JButton storeDetailsBtn = createActionButton("Store Details", 
                UIConstants.PRIMARY_COLOR, UIConstants.PRIMARY_DARK);
            storeDetailsBtn.addActionListener(e -> openStoreSettings());
            panel.add(storeDetailsBtn);
            panel.add(Box.createRigidArea(new Dimension(0, 8)));
            
            JButton manageEmployeesBtn = createActionButton("Manage Employees", 
                UIConstants.PRIMARY_COLOR, UIConstants.PRIMARY_DARK);
            manageEmployeesBtn.addActionListener(e -> openEmployeeManager());
            panel.add(manageEmployeesBtn);
            panel.add(Box.createRigidArea(new Dimension(0, 8)));
        }

        // Everyone can logout
        JButton logoutBtn = createActionButton("Logout", 
            UIConstants.DANGER_COLOR, UIConstants.DANGER_DARK);
        logoutBtn.addActionListener(e -> logout());
        panel.add(logoutBtn);

        return panel;
    }

    // Create action button with colors
    private JButton createActionButton(String text, Color bgColor, Color hoverColor) {
        return createBaseButton(text, bgColor, Color.WHITE, hoverColor, true);
    }

    // Helper method to consolidate button creation logic
    private JButton createBaseButton(String text, Color bgColor, Color fgColor, Color hoverColor, boolean isOpaque) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, (bgColor == UIConstants.NAV_BACKGROUND) ? 14 : 12)); // Slight font difference handled
        button.setForeground(fgColor);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false); // Key for hover effects
        button.setOpaque(isOpaque);
        button.setHorizontalAlignment(bgColor == UIConstants.NAV_BACKGROUND ? SwingConstants.LEFT : SwingConstants.CENTER); // Alignment differs
        
        // Size handling
        if (bgColor == UIConstants.NAV_BACKGROUND) {
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
            button.setPreferredSize(new Dimension(220, 50));
            button.setBorder(new EmptyBorder(12, 20, 12, 20));
        } else {
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            button.setPreferredSize(new Dimension(200, 40));
            button.setBorder(new EmptyBorder(8, 15, 8, 15));
        }
        
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Common MouseListener for Hover Effects
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                // Logic for Nav Buttons (Selection state check)
                if (bgColor == UIConstants.NAV_BACKGROUND) {
                    if (button != currentSelectedButton) {
                        button.setBackground(hoverColor);
                        button.setForeground(Color.WHITE);
                        button.setOpaque(true);
                    }
                } 
                // Logic for Action Buttons (Simple hover)
                else {
                    button.setBackground(hoverColor);
                    button.setOpaque(true);
                }
            }

            public void mouseExited(MouseEvent e) {
                // Logic for Nav Buttons
                if (bgColor == UIConstants.NAV_BACKGROUND) {
                    if (button != currentSelectedButton) {
                        button.setForeground(fgColor);
                        button.setOpaque(false);
                        button.setBackground(bgColor);
                    }
                } 
                // Logic for Action Buttons
                else {
                    button.setBackground(bgColor);                    
                }
            }
        });

        return button;
    }
    // ========== BUTTON ACTIONS ==========

    // Open store settings dialog
    private void openStoreSettings() {
        StoreSettingsDialog dialog = new StoreSettingsDialog(mainFrame, storeRepository, 
            storeNameLabel, storeLocationLabel, storeContactLabel);
        dialog.setVisible(true);
    }

    // Open employee management dialog
    private void openEmployeeManager() {
        EmployeeManagerDialog dialog = new EmployeeManagerDialog(mainFrame);
        dialog.setVisible(true);
    }

    // Logout confirmation
    private void logout() {
        int choice = JOptionPane.showConfirmDialog(mainFrame,
            "Are you sure you want to logout?", "Confirm Logout",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            mainFrame.showLoginPanel();
        }
    }
}