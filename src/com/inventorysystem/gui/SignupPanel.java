package com.inventorysystem.gui;

import com.inventorysystem.data.UserRepository;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

// Admin account registration
public class SignupPanel extends JPanel {

    private userFrame mainFrame;
    private UserRepository userRepository;
    private BufferedImage backgroundImage;

    public SignupPanel(userFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.userRepository = new UserRepository();
        
        // Load background image
        try {
            backgroundImage = ImageIO.read(new File("src/resources/login_bg.png"));
        } catch (IOException e) {
            System.err.println("Background image not found, using default background");
        }

        setLayout(new GridBagLayout());

        // Signup form
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(new Color(255, 255, 255, 240)); // Semi-transparent white
        formPanel.setBorder(new CompoundBorder(
            new LineBorder(new Color(189, 195, 199), 1, true),
            new EmptyBorder(50, 60, 50, 60)
        ));
        formPanel.setPreferredSize(new Dimension(500, 600));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);

        // Big title at top
        JLabel titleLabel = new JLabel("Create Admin Account", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(new Color(41, 128, 185));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 30, 0);
        formPanel.add(titleLabel, gbc);

        // Info message
        JLabel infoLabel = new JLabel("<html><center>Only Admin accounts can be created here.<br>" +
            "Admins can add Employee accounts later.</center></html>", SwingConstants.CENTER);
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        infoLabel.setForeground(new Color(127, 140, 141));
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 25, 0);
        formPanel.add(infoLabel, gbc);

        // Username label
        gbc.gridy = 2;
        gbc.insets = new Insets(12, 0, 5, 0);
        JLabel userLabel = new JLabel("Username");
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(userLabel, gbc);

        // Username input box
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 18, 0);
        JTextField userField = new JTextField(20);
        userField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        userField.setBorder(new CompoundBorder(
            new LineBorder(new Color(189, 195, 199), 1, true),
            new EmptyBorder(12, 15, 12, 15)
        ));
        formPanel.add(userField, gbc);

        // Password label
        gbc.gridy = 4;
        gbc.insets = new Insets(12, 0, 5, 0);
        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        formPanel.add(passLabel, gbc);

        // Password input box (hidden characters)
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 25, 0);
        JPasswordField passField = new JPasswordField(20);
        passField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        passField.setBorder(new CompoundBorder(
            new LineBorder(new Color(189, 195, 199), 1, true),
            new EmptyBorder(12, 15, 12, 15)
        ));
        formPanel.add(passField, gbc);

        // Back and Create Admin buttons
        gbc.gridy = 6;
        gbc.insets = new Insets(15, 0, 0, 0);
        JPanel buttonPanel = createButtonPanel(userField, passField);
        buttonPanel.setBackground(Color.WHITE);
        formPanel.add(buttonPanel, gbc);

        add(formPanel);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            // Draw background image scaled to fit panel
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            // Fallback to solid color
            g.setColor(new Color(236, 240, 241));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    // Panel with Back and Create Admin buttons
    private JPanel createButtonPanel(JTextField userField, JPasswordField passField) {
        JPanel panel = new JPanel(new GridLayout(1, 2, 15, 0));
        panel.setBackground(Color.WHITE);

        JButton backButton = createStyledButton("Back", new Color(127, 140, 141), new Color(100, 110, 120));
        JButton signupButton = createStyledButton("Create Admin", new Color(231, 76, 60), new Color(192, 57, 43));

        panel.add(backButton);
        panel.add(signupButton);

        backButton.addActionListener(e -> mainFrame.showLoginPanel());
        signupButton.addActionListener(e -> handleSignup(userField, passField));

        return panel;
    }

    // Make button with color and hover effect
    private JButton createStyledButton(String text, Color bgColor, Color hoverColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 15));
        button.setPreferredSize(new Dimension(150, 45));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(hoverColor);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });

        return button;
    }

    // Try to create the admin account
    private void handleSignup(JTextField userField, JPasswordField passField) {
        String username = userField.getText().trim();
        String password = new String(passField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showStyledMessage("Please fill in all fields.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // Create as Admin (not Employee)
            boolean success = userRepository.signup(username, password, "Admin");
            if (success) {
                showStyledMessage("Admin account created successfully!\nYou can now log in and add employees.", 
                    "Success", JOptionPane.INFORMATION_MESSAGE);
                userField.setText("");
                passField.setText("");
                mainFrame.showLoginPanel();
            }
        } catch (SQLException ex) {
            if (ex.getErrorCode() == 1062) {
                showStyledMessage("Username '" + username + "' already exists.", 
                    "Registration Error", JOptionPane.ERROR_MESSAGE);
            } else {
                showStyledMessage("Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showStyledMessage(String message, String title, int messageType) {
        UIManager.put("OptionPane.messageFont", new Font("Segoe UI", Font.PLAIN, 14));
        JOptionPane.showMessageDialog(this, message, title, messageType);
    }

    // Test method
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Signup Test");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 750);
            frame.add(new SignupPanel(new userFrame()));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}