package GUI;

import Database.DatabaseConnection;
import Database.PasswordHasher;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginPanel extends JPanel {

    private userFrame mainFrame; // Field to hold the frame reference

    public LoginPanel(userFrame mainFrame) { // Constructor accepts userFrame
        this.mainFrame = mainFrame;
        
        // Main panel uses GridBagLayout to center the form
        setLayout(new GridBagLayout());
        setBackground(UIConstants.BACKGROUND_COLOR);

        // Panel that holds the form components
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(UIConstants.FORM_COLOR);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(30, 40, 30, 40)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- Title ---
        JLabel titleLabel = new JLabel("Welcome Back!");
        titleLabel.setFont(UIConstants.TITLE_FONT);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, 20, 0); // Bottom margin for title
        formPanel.add(titleLabel, gbc);

        // Reset insets and grid width for form fields
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridwidth = 1;

        // --- Username Field ---
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(UIConstants.LABEL_FONT);
        formPanel.add(userLabel, gbc);
        
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextField userField = new JTextField(15);
        userField.setFont(UIConstants.INPUT_FONT);
        formPanel.add(userField, gbc);

        // --- Password Field ---
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(UIConstants.LABEL_FONT);
        formPanel.add(passLabel, gbc);
        
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JPasswordField passField = new JPasswordField(15);
        passField.setFont(UIConstants.INPUT_FONT);
        formPanel.add(passField, gbc);

        // --- Button Panel ---
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(20, 0, 0, 0); // Top margin for buttons
        JPanel buttonPanel = createButtonPanel(userField, passField);
        buttonPanel.setBackground(UIConstants.FORM_COLOR); // Match form background
        formPanel.add(buttonPanel, gbc);

        // Add the styled form panel to the main panel (which centers it)
        add(formPanel);
    }

    private JPanel createButtonPanel(JTextField userField, JPasswordField passField) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));

        JButton loginButton = new JButton("Login");
        loginButton.setFont(UIConstants.BUTTON_FONT);
        loginButton.setPreferredSize(UIConstants.BUTTON_DIMENSION);
        
        JButton switchToSignupButton = new JButton("Sign Up");
        switchToSignupButton.setFont(UIConstants.BUTTON_FONT);
        switchToSignupButton.setPreferredSize(UIConstants.BUTTON_DIMENSION);

        panel.add(loginButton);
        panel.add(switchToSignupButton);

        loginButton.addActionListener(e -> {
            String username = userField.getText();
            String password = new String(passField.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all fields.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String sql = "SELECT user_id, password_hash, user_role FROM users WHERE username = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setString(1, username);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    String dbPasswordHash = rs.getString("password_hash");
                    if (PasswordHasher.verifyPassword(password, dbPasswordHash)) {
                        int userId = rs.getInt("user_id");
                        String role = rs.getString("user_role");
                        mainFrame.showStorePanel(userId, username, role);
                    } else {
                        JOptionPane.showMessageDialog(this, "Invalid username or password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid username or password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // FIX IS HERE: Call the method on the 'mainFrame' instance
        switchToSignupButton.addActionListener(e -> mainFrame.showSignupPanel()); 

        return panel;
    }
}