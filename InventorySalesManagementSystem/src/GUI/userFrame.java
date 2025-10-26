package GUI;

import Database.DatabaseConnection; // Import Database connection
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class userFrame extends JFrame {

    private CardLayout cardLayout = new CardLayout();
    private JPanel mainContainer;

    
    public int loggedInUserId;
    public String loggedInUsername;
    public String loggedInUserRole;
    private int selectedStoreId;

    public userFrame() {
        setTitle("Inventory and Sales Management System");
        setSize(500, 550); // Original login/signup size
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        mainContainer = new JPanel(cardLayout);

        // Initialize panels
        LoginPanel loginPanel = new LoginPanel(this);
        SignupPanel signupPanel = new SignupPanel(this);

        mainContainer.add(loginPanel, "login");
        mainContainer.add(signupPanel, "signup");
        
        add(mainContainer);
    }
    
    // Updated to match your LoginPanel logic:
    public void showStorePanel(int userId, String username, String role) {
        this.loggedInUserId = userId;
        this.loggedInUsername = username;
        this.loggedInUserRole = role;
        
        StorePanel storePanel = new StorePanel(this, userId);
        mainContainer.add(storePanel, "stores");
        cardLayout.show(mainContainer, "stores");
        
        // Adjust size for the StorePanel
        setSize(800, 600); 
        setLocationRelativeTo(null);
    }
    
    // CRITICAL UPDATE: Fetch store name and pass it to MainApplicationPanel
    public void showMainApplication(int storeId) {
        this.selectedStoreId = storeId; 
        String storeName = "Unknown Store"; 

        String sql = "SELECT store_name FROM stores WHERE store_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, storeId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                storeName = rs.getString("store_name");
            }

        } catch (SQLException e) {
            System.err.println("Error fetching store name: " + e.getMessage());
        }
        
        // Pass the fetched storeName along with other info
        MainApplicationPanel mainAppPanel = new MainApplicationPanel(this, loggedInUsername, loggedInUserRole, this.selectedStoreId, storeName);
        mainContainer.add(mainAppPanel, "mainApp");
        cardLayout.show(mainContainer, "mainApp");

        // Adjust size for the main application
        setSize(1280, 720); 
        setLocationRelativeTo(null);
    }

    public void showLoginPanel() {
        // Clear the main application panel when logging out to free resources
        if (mainContainer.getComponentCount() > 2) {
             // Remove the MainApplicationPanel if it exists
            mainContainer.remove(mainContainer.getComponent(2)); 
        }
        
        cardLayout.show(mainContainer, "login");
        setSize(500, 550); // Resize back for Login
        setLocationRelativeTo(null);
    }

    // Add this method inside your existing userFrame class:

    public void showSignupPanel() {
    // We assume the SignupPanel is already added as "signup"
    cardLayout.show(mainContainer, "signup");
    
    // Resize for the SignupPanel, which may be different from the LoginPanel
    setSize(500, 650); // Slightly larger for the role selection
    setLocationRelativeTo(null);
}
    
    // Keep the main method for running the app
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new userFrame().setVisible(true);
        });
    }
}