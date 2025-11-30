package com.inventorysystem.gui;

import com.inventorysystem.data.UserRepository;
import com.inventorysystem.model.User;
import com.inventorysystem.util.SoundUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

// Login screen - username/password authentication, Enter key support, hover effects
public class LoginPanel extends JPanel {

    private userFrame mainFrame;
    private UserRepository userRepository;
    private BufferedImage backgroundImage;

    public LoginPanel(userFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.userRepository = new UserRepository();
        
        // Load background image
        try {
            backgroundImage = ImageIO.read(new File("src/resources/login_bg.png"));
        } catch (IOException e) {
            System.err.println("Background image not found, using default background");
        }

        setLayout(new GridBagLayout());

        // Login form
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

        // App name
        JLabel brandLabel = new JLabel("SmartStock", SwingConstants.CENTER);
        brandLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
        brandLabel.setForeground(new Color(41, 128, 185));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 5, 0);
        formPanel.add(brandLabel, gbc);

        // Subtitle
        JLabel subtitleLabel = new JLabel("Inventory Management System", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(127, 140, 141));
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 40, 0);
        formPanel.add(subtitleLabel, gbc);

        // Welcome
        JLabel welcomeLabel = new JLabel("Welcome Back!", SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        welcomeLabel.setForeground(new Color(44, 62, 80));
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 30, 0);
        formPanel.add(welcomeLabel, gbc);

        // Username
        gbc.gridy = 3;
        gbc.insets = new Insets(12, 0, 5, 0);
        JLabel userLabel = new JLabel("Username");
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        userLabel.setForeground(new Color(44, 62, 80));
        formPanel.add(userLabel, gbc);

        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 18, 0);
        JTextField userField = new JTextField(20);
        userField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        userField.setBorder(new CompoundBorder(
            new LineBorder(new Color(189, 195, 199), 1, true),
            new EmptyBorder(12, 15, 12, 15)
        ));
        formPanel.add(userField, gbc);

        // Password
        gbc.gridy = 5;
        gbc.insets = new Insets(12, 0, 5, 0);
        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        passLabel.setForeground(new Color(44, 62, 80));
        formPanel.add(passLabel, gbc);

        gbc.gridy = 6;
        gbc.insets = new Insets(0, 0, 25, 0);
        JPasswordField passField = new JPasswordField(20);
        passField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        passField.setBorder(new CompoundBorder(
            new LineBorder(new Color(189, 195, 199), 1, true),
            new EmptyBorder(12, 15, 12, 15)
        ));
        formPanel.add(passField, gbc);

        // Buttons
        gbc.gridy = 7;
        gbc.insets = new Insets(15, 0, 0, 0);
        JPanel buttonPanel = createButtonPanel(userField, passField);
        buttonPanel.setBackground(Color.WHITE);
        formPanel.add(buttonPanel, gbc);
        
        // Enter key to login
        java.awt.event.KeyAdapter enterKeyListener = new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    handleLogin(userField, passField);
                }
            }
        };
        userField.addKeyListener(enterKeyListener);
        passField.addKeyListener(enterKeyListener);

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

    // Login and Sign Up buttons
    private JPanel createButtonPanel(JTextField userField, JPasswordField passField) {
        JPanel panel = new JPanel(new GridLayout(1, 2, 15, 0));
        panel.setBackground(Color.WHITE);

        JButton loginButton = createStyledButton("Login", new Color(41, 128, 185), new Color(31, 97, 141));
        JButton signupButton = createStyledButton("Sign Up", new Color(46, 204, 113), new Color(39, 174, 96));

        panel.add(loginButton);
        panel.add(signupButton);

        loginButton.addActionListener(e -> handleLogin(userField, passField));
        signupButton.addActionListener(e -> mainFrame.showSignupPanel());

        return panel;
    }

    // Styled button with hover effect
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

    // Authenticate user
    private void handleLogin(JTextField userField, JPasswordField passField) {
        String username = userField.getText().trim();
        String password = new String(passField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showStyledMessage("Please fill in all fields.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // Check credentials
            User user = userRepository.login(username, password);
            if (user != null) {
                // Success
                mainFrame.handleLoginSuccess(user.id(), user.username(), user.role());
                SwingUtilities.invokeLater(() -> showWelcomeDialog(user));
            } else {
                // Failed
                showStyledMessage("Invalid username or password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            showStyledMessage("Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showStyledMessage(String message, String title, int messageType) {
        UIManager.put("OptionPane.messageFont", new Font("Segoe UI", Font.PLAIN, 14));
        JOptionPane.showMessageDialog(this, message, title, messageType);
    }

    // Modern, clean, and borderless welcome dialog
    private void showWelcomeDialog(User user) {
        JDialog welcomeDialog = new JDialog(mainFrame, true);
        welcomeDialog.setUndecorated(true);
        welcomeDialog.setLayout(new BorderLayout());
        welcomeDialog.setSize(380, 220); // Slightly larger for better spacing
        welcomeDialog.setLocationRelativeTo(mainFrame);
        
        // Main Container with a subtle border
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(Color.WHITE);
        // Border matching the subtle background theme
        mainPanel.setBorder(new LineBorder(new Color(220, 220, 220), 3));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; 
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 0, 5, 0);
        gbc.anchor = GridBagConstraints.CENTER;

        // 1. Large Success Icon
        JLabel iconLabel = new JLabel();
        iconLabel.setFont(new Font("Segoe UI", Font.BOLD, 55));
        iconLabel.setForeground(new Color(46, 204, 113)); // Success Green
        mainPanel.add(iconLabel, gbc);

        // 2. "Success" Title
        gbc.gridy++;
        JLabel titleLabel = new JLabel("Login Successful");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(new Color(50, 50, 50)); // Dark Gray
        mainPanel.add(titleLabel, gbc);
        
        // 3. User Welcome Message
        gbc.gridy++;
        gbc.insets = new Insets(5, 20, 20, 20); // Side padding
        JLabel userLabel = new JLabel("Welcome, " + user.username());
        userLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        userLabel.setForeground(new Color(100, 100, 100)); // Lighter Gray
        mainPanel.add(userLabel, gbc);
        
        welcomeDialog.add(mainPanel, BorderLayout.CENTER);

        // Play success sound
        SoundUtil.play("success.wav");

        // Auto-close timer (3 seconds)
        Timer timer = new Timer(1500, e -> welcomeDialog.dispose());
        timer.setRepeats(false);
        timer.start();



        welcomeDialog.setVisible(true);
    }

    // Test method
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Login Test");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 700);
            frame.add(new LoginPanel(new userFrame()));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}