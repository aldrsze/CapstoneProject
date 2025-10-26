package GUI;

import Database.DatabaseConnection;
import Database.PasswordHasher;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SignupPanel extends JPanel {

    private userFrame mainFrame;

    public SignupPanel(userFrame mainFrame) {
        this.mainFrame = mainFrame;
        
        setLayout(new GridBagLayout());
        setBackground(UIConstants.BACKGROUND_COLOR);

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
        JLabel titleLabel = new JLabel("Create Account");
        titleLabel.setFont(UIConstants.TITLE_FONT);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, 20, 0);
        formPanel.add(titleLabel, gbc);
        
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
        
        // --- Role Field ---
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        JLabel roleLabel = new JLabel("User Role:");
        roleLabel.setFont(UIConstants.LABEL_FONT);
        formPanel.add(roleLabel, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JComboBox<String> roleBox = new JComboBox<>(new String[]{"Admin", "Employee"});
        roleBox.setFont(UIConstants.INPUT_FONT);
        formPanel.add(roleBox, gbc);

        // --- Button Panel ---
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(20, 0, 0, 0);
        JPanel buttonPanel = createButtonPanel(userField, passField, roleBox);
        buttonPanel.setBackground(UIConstants.FORM_COLOR);
        formPanel.add(buttonPanel, gbc);

        add(formPanel);
    }

    private JPanel createButtonPanel(JTextField userField, JPasswordField passField, JComboBox<String> roleBox) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));

        JButton signupButton = new JButton("Sign Up");
        signupButton.setFont(UIConstants.BUTTON_FONT);
        signupButton.setPreferredSize(UIConstants.BUTTON_DIMENSION);

        JButton switchToLoginButton = new JButton("Login");
        switchToLoginButton.setFont(UIConstants.BUTTON_FONT);
        switchToLoginButton.setPreferredSize(UIConstants.BUTTON_DIMENSION);
        
        panel.add(signupButton);
        panel.add(switchToLoginButton);

        signupButton.addActionListener(e -> {
            String username = userField.getText();
            String password = new String(passField.getPassword());
            String role = (String) roleBox.getSelectedItem();

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all fields.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String hashedPassword = PasswordHasher.hashPassword(password);
            String sql = "INSERT INTO users (username, password_hash, user_role) VALUES (?, ?, ?)";

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, username);
                pstmt.setString(2, hashedPassword);
                pstmt.setString(3, role);
                
                if (pstmt.executeUpdate() > 0) {
                    JOptionPane.showMessageDialog(this, "Account created successfully for " + username + "!\nYou can now log in.");
                    userField.setText("");
                    passField.setText("");
                }
            } catch (SQLException ex) {
                if (ex.getErrorCode() == 1062) { // Handles duplicate username
                    JOptionPane.showMessageDialog(this, "Username '" + username + "' already exists.", "Registration Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        switchToLoginButton.addActionListener(e -> mainFrame.showLoginPanel());
        
        return panel;
    }
}