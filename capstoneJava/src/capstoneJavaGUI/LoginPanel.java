// LoginPanel.java
package capstoneJavaGUI;

import javax.swing.*;
import java.awt.*;

public class LoginPanel extends JPanel {

    // --- (Constructor and component setup is the same) ---
    public LoginPanel(JPanel container) {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 12, 8, 12);

        JLabel titleLabel = new JLabel("Login", SwingConstants.CENTER);
        titleLabel.setFont(UIConstants.TITLE_FONT);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(25, 10, 25, 10);
        add(titleLabel, gbc);

        gbc.gridwidth = 1;
        gbc.insets = new Insets(8, 12, 8, 12);

        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(UIConstants.LABEL_FONT);
        gbc.gridy = 1;
        add(userLabel, gbc);

        JTextField userField = new JTextField(20);
        userField.setFont(UIConstants.INPUT_FONT);
        gbc.gridy = 2;
        add(userField, gbc);

        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(UIConstants.LABEL_FONT);
        gbc.gridy = 3;
        add(passLabel, gbc);

        JPasswordField passField = new JPasswordField(20);
        passField.setFont(UIConstants.INPUT_FONT);
        gbc.gridy = 4;
        add(passField, gbc);

        JPanel buttonPanel = createButtonPanel(container, userField, passField);
        gbc.gridy = 5;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(25, 10, 20, 10);
        add(buttonPanel, gbc);
    }

    private JPanel createButtonPanel(JPanel container, JTextField userField, JPasswordField passField) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));

        JButton loginButton = new JButton("Login");
        loginButton.setFont(UIConstants.BUTTON_FONT);
        loginButton.setPreferredSize(UIConstants.BUTTON_DIMENSION);
        
        JButton switchToSignupButton = new JButton("Sign Up");
        switchToSignupButton.setFont(UIConstants.BUTTON_FONT);
        switchToSignupButton.setPreferredSize(UIConstants.BUTTON_DIMENSION);

        panel.add(loginButton);
        panel.add(switchToSignupButton);

        // --- MODIFIED ACTION LISTENER ---
        loginButton.addActionListener(e -> {
            String username = userField.getText();
            String password = new String(passField.getPassword());
            
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all fields.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // ADDED: Placeholder logic to determine the user's role.
            // In a real app, this would come from a database.
            String role = "Employee"; // Default role
            if (username.equalsIgnoreCase("Admin")) {
                role = "Admin";
            }

            JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            topFrame.dispose();
            
            // CHANGED: Pass username and role when creating the MainFrame
            new mainFrame(username, role);
        });

        switchToSignupButton.addActionListener(e -> {
            CardLayout cl = (CardLayout) container.getLayout();
            cl.show(container, "signup");
        });

        return panel;
    }
}