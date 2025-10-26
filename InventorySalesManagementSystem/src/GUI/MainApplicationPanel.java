package GUI;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MainApplicationPanel extends JPanel {

    private CardLayout cardLayout = new CardLayout();
    private JPanel mainContentPanel;
    private userFrame mainFrame;

    // CONSTRUCTOR UPDATED: Now accepts 'storeName'
    // CONSTRUCTOR UPDATED: Now accepts 'storeName'
    public MainApplicationPanel(userFrame mainFrame, String username, String role, int storeId, String storeName) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());

        JPanel navPanel = new JPanel();
        navPanel.setLayout(null); // Using absolute positioning
        navPanel.setPreferredSize(new Dimension(180, 0));
        navPanel.setBackground(new Color(240, 240, 240));
        navPanel.setBorder(new LineBorder(new Color(220, 220, 220)));

        // --- Store/User Info Display (MOVED TO TOP) ---
        int currentY = 20; // Starting Y coordinate

        JLabel storeTitleLabel = new JLabel("CURRENT STORE", SwingConstants.CENTER);
        storeTitleLabel.setBounds(10, currentY, 160, 20); // Position near top
        storeTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        storeTitleLabel.setForeground(new Color(50, 50, 50));
        navPanel.add(storeTitleLabel);
        currentY += 20; // Move Y down for next label

        JLabel storeNameLabel = new JLabel(storeName, SwingConstants.CENTER);
        storeNameLabel.setBounds(10, currentY, 160, 25); // Position below title
        storeNameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        storeNameLabel.setForeground(new Color(0, 100, 0)); // Highlight store name
        navPanel.add(storeNameLabel);
        currentY += 25; // Move Y down

        JLabel storeIdLabel = new JLabel("ID: " + storeId + " | Role: " + role, SwingConstants.CENTER); // Combine secondary info
        storeIdLabel.setBounds(10, currentY, 160, 20); // Position below name
        storeIdLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        storeIdLabel.setHorizontalAlignment(SwingConstants.CENTER);
        navPanel.add(storeIdLabel);
        currentY += 30; // Add some space before buttons

        // --- Navigation Buttons (Shifted Down) ---
        int buttonHeight = 70; // Slightly smaller height
        int buttonSpacing = 15; // Space between buttons

        JButton dashboardButton = new JButton("DASHBOARD");
        dashboardButton.setBounds(20, currentY, 140, buttonHeight);
        dashboardButton.setFocusPainted(false);
        navPanel.add(dashboardButton);
        currentY += buttonHeight + buttonSpacing;

        JButton productsButton = new JButton("PRODUCTS");
        productsButton.setBounds(20, currentY, 140, buttonHeight);
        productsButton.setFocusPainted(false);
        navPanel.add(productsButton);
        currentY += buttonHeight + buttonSpacing;

        JButton recordsButton = new JButton("RECORDS");
        recordsButton.setBounds(20, currentY, 140, buttonHeight);
        recordsButton.setFocusPainted(false);
        navPanel.add(recordsButton);
        currentY += buttonHeight + buttonSpacing;

        JButton qrCodeButton = new JButton("QR CODE");
        qrCodeButton.setBounds(20, currentY, 140, buttonHeight);
        qrCodeButton.setFocusPainted(false);
        navPanel.add(qrCodeButton);
        currentY += buttonHeight + buttonSpacing + 30; // Extra space before action buttons


        // --- Action Buttons (Positioned after nav buttons) ---
        int actionButtonHeight = 45;

        JButton changeStoreButton = new JButton("CHANGE STORE ");
        changeStoreButton.setBounds(20, currentY, 140, actionButtonHeight);
        navPanel.add(changeStoreButton);
        currentY += actionButtonHeight + 15; // Space before logout

        JButton logoutButton = new JButton("LOGOUT ");
        logoutButton.setBounds(20, currentY, 140, actionButtonHeight);
        navPanel.add(logoutButton);

        // Style action buttons
        styleActionButton(changeStoreButton, new Color(100, 150, 255), new Color(80, 120, 220));
        styleActionButton(logoutButton, new Color(255, 100, 100), new Color(220, 80, 80));


        // --- Main Content Panel Setup (Pass storeId to each panel) ---
        mainContentPanel = new JPanel(cardLayout);
        mainContentPanel.add(new GUI.dashboardPanel(storeId), "Dashboard"); //
        mainContentPanel.add(new GUI.productsPanel(storeId), "Products"); //
        mainContentPanel.add(new GUI.recordsPanel(storeId), "Records"); //
        mainContentPanel.add(new GUI.QRCodePanel(storeId), "QRCode"); //

        add(navPanel, BorderLayout.WEST);
        add(mainContentPanel, BorderLayout.CENTER);

        // --- Button Actions (Listeners remain the same) ---
        dashboardButton.addActionListener(e -> cardLayout.show(mainContentPanel, "Dashboard"));
        productsButton.addActionListener(e -> cardLayout.show(mainContentPanel, "Products"));
        recordsButton.addActionListener(e -> cardLayout.show(mainContentPanel, "Records"));
        qrCodeButton.addActionListener(e -> cardLayout.show(mainContentPanel, "QRCode"));

        changeStoreButton.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(mainFrame,
                "Are you sure you want to change stores?", "Confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                // Ensure loggedInUserId, etc. are accessible or passed correctly
                mainFrame.showStorePanel(mainFrame.loggedInUserId, mainFrame.loggedInUsername, mainFrame.loggedInUserRole); //
            }
        });

        logoutButton.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(mainFrame,
                "Are you sure you want to logout?", "Confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                mainFrame.showLoginPanel(); //
            }
        });

        cardLayout.show(mainContentPanel, "Dashboard"); // Start on Dashboard
    }

    // styleActionButton method remains unchanged...
    private void styleActionButton(JButton button, Color normalColor, Color hoverColor) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(normalColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(normalColor);
            }
        });
    }

    // Rest of the class...
}