// MainFrame.java
package capstoneJavaGUI;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

// CONVENTION: Class names in Java should use PascalCase (e.g., MainFrame)
public class mainFrame extends JFrame {

    private CardLayout cardLayout = new CardLayout();
    private JPanel mainContentPanel;
    private dashboardPanel dashboardPanel;
    private productsPanel productsPanel;
    private recordsPanel recordsPanel;

    // CHANGED: The constructor now accepts the username and role
    public mainFrame(String username, String role) {
        setTitle("Inventory and Sales Management System");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel navPanel = new JPanel();
        navPanel.setLayout(null);
        navPanel.setPreferredSize(new Dimension(180, 0));
        navPanel.setBackground(new Color(240, 240, 240));
        navPanel.setBorder(new LineBorder(Color.GRAY));

        // Navigation Buttons (no changes here)
        JButton dashboardButton = new JButton("DASHBOARD");
        dashboardButton.setBounds(20, 30, 140, 80);
        dashboardButton.setFocusPainted(false);

        JButton productsButton = new JButton("PRODUCTS");
        productsButton.setBounds(20, 130, 140, 80);
        productsButton.setFocusPainted(false);

        JButton recordsButton = new JButton("RECORDS");
        recordsButton.setBounds(20, 230, 140, 80);
        recordsButton.setFocusPainted(false);
        
        JButton logoutButton = new JButton("LOGOUT");
        logoutButton.setBounds(20, 330, 140, 80);
        logoutButton.setFocusPainted(false);
        logoutButton.setBackground(new Color(255, 100, 100));
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setContentAreaFilled(false);
        logoutButton.setOpaque(true);
       
        navPanel.add(dashboardButton);
        navPanel.add(productsButton);
        navPanel.add(recordsButton);
        navPanel.add(logoutButton);

        // ADDED: Labels to display the current user's information
        JLabel userLabel = new JLabel("User: " + username);
        userLabel.setBounds(15, 500, 150, 20);
        userLabel.setFont(new Font("Arial", Font.BOLD, 12));
        navPanel.add(userLabel);

        JLabel roleLabel = new JLabel("Role: " + role);
        roleLabel.setBounds(15, 520, 150, 20);
        roleLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        navPanel.add(roleLabel);

        // --- Main Content Panel Setup (no changes here) ---
        mainContentPanel = new JPanel(cardLayout);
        dashboardPanel = new dashboardPanel();
        productsPanel = new productsPanel();
        recordsPanel = new recordsPanel();

        mainContentPanel.add(dashboardPanel, "Dashboard");
        mainContentPanel.add(productsPanel, "Products");
        mainContentPanel.add(recordsPanel, "Records");

        add(navPanel, BorderLayout.WEST);
        add(mainContentPanel, BorderLayout.CENTER);

        // --- Button Actions (no changes here) ---
        dashboardButton.addActionListener(e -> cardLayout.show(mainContentPanel, "Dashboard"));
        productsButton.addActionListener(e -> cardLayout.show(mainContentPanel, "Products"));
        recordsButton.addActionListener(e -> cardLayout.show(mainContentPanel, "Records"));
        logoutButton.addActionListener(e -> {
            dispose();
            userFrame.main(null); // Assuming UserFrame is your login screen's entry point
        });

        cardLayout.show(mainContentPanel, "Dashboard");
        setVisible(true);
    }
    
    // REMOVED: The main method. This frame should only be opened from the
    // login screen, not run directly. Your app's entry point is UserFrame.
}