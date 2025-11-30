package com.inventorysystem.gui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * About panel displaying team members in cards and system information at the bottom.
 */
public class AboutPanel extends JPanel {

    public AboutPanel() {
        setLayout(new BorderLayout());
        setBackground(UIConstants.BACKGROUND_COLOR);
        setBorder(new EmptyBorder(0, 0, 0, 0)); // Edge-to-edge

        // Main scrollable container
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBackground(UIConstants.BACKGROUND_COLOR);
        mainContent.setBorder(new EmptyBorder(30, 20, 30, 20));

        // --- 1. Header Section ---
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        
        JLabel titleLabel = new JLabel("MEET THE TEAM");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        titleLabel.setForeground(UIConstants.PRIMARY_COLOR);
        headerPanel.add(titleLabel);
        
        mainContent.add(headerPanel);
        mainContent.add(Box.createRigidArea(new Dimension(0, 10)));
        
        JPanel subHeaderPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        subHeaderPanel.setBackground(UIConstants.BACKGROUND_COLOR);
        JLabel subTitleLabel = new JLabel("BSIT-1B | Group #3");
        subTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        subTitleLabel.setForeground(UIConstants.TEXT_SECONDARY);
        subHeaderPanel.add(subTitleLabel);
        
        mainContent.add(subHeaderPanel);
        mainContent.add(Box.createRigidArea(new Dimension(0, 30)));

        // --- 2. Members Grid Section ---
        // Using FlowLayout with gaps to allow wrapping based on screen size
        JPanel membersContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        membersContainer.setBackground(UIConstants.BACKGROUND_COLOR);

        // Member Cards
        membersContainer.add(createMemberCard("George Harold A. Alcantara", "Project Manager / Documentation Writer", "GHA", UIConstants.PRIMARY_DARK));
        membersContainer.add(createMemberCard("Aldrin Miguel A. Jariel", "System Analyst / Dev / QA / Documentation Writer", "AMA", UIConstants.ACCENT_COLOR));
        membersContainer.add(createMemberCard("John Christoper A. Perez", "UI/UX Designer / Documentation Writer", "JCA", UIConstants.SUCCESS_COLOR));
        membersContainer.add(createMemberCard("Ron Paulo G. Angeles", "Documentation Writer", "RPG", UIConstants.WARNING_COLOR));
        membersContainer.add(createMemberCard("Matthew Dane D. Calangian", "Documentation Writer", "MDD", UIConstants.WARNING_COLOR));

        mainContent.add(membersContainer);
        mainContent.add(Box.createRigidArea(new Dimension(0, 40)));

        // --- 3. Divider ---
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setMaximumSize(new Dimension(800, 2));
        separator.setForeground(UIConstants.BORDER_COLOR);
        mainContent.add(separator);
        mainContent.add(Box.createRigidArea(new Dimension(0, 30)));

        // --- 4. System Information Footer ---
        JPanel systemInfoPanel = createSystemInfoPanel();
        mainContent.add(systemInfoPanel);

        // Scroll Pane Configuration
        JScrollPane scrollPane = new JScrollPane(mainContent);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Smooth scrolling
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Creates a card for a team member with avatar image or generated initials.
     */
    private JPanel createMemberCard(String name, String role, String initials, Color avatarColor) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(240, 300)); // Fixed card size
        card.setBackground(Color.WHITE);
        
        // Card Border Styling
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(UIConstants.BORDER_COLOR, 1, true), // Rounded border attempt (Swing limits) or simple line
            new EmptyBorder(25, 15, 25, 15)
        ));

        // 1. Avatar (Try to load image, fallback to circle with initials)
        JLabel avatarLabel = createAvatarLabel(initials, avatarColor);
        
        // Force size for the avatar area (increased for better visibility)
        avatarLabel.setPreferredSize(new Dimension(130, 130));
        avatarLabel.setMaximumSize(new Dimension(130, 130));
        avatarLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // 2. Name Label
        JLabel nameLabel = new JLabel("<html><center>" + name + "</center></html>");
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        nameLabel.setForeground(UIConstants.TEXT_PRIMARY);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // 3. Role Label
        JLabel roleLabel = new JLabel("<html><center>" + role + "</center></html>");
        roleLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        roleLabel.setForeground(UIConstants.TEXT_SECONDARY);
        roleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        roleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Layout Assembly
        card.add(Box.createVerticalGlue());
        card.add(avatarLabel);
        card.add(Box.createRigidArea(new Dimension(0, 20))); // Spacer
        card.add(nameLabel);
        card.add(Box.createRigidArea(new Dimension(0, 8)));  // Spacer
        card.add(roleLabel);
        card.add(Box.createVerticalGlue());
        
        // Hover Effect
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                card.setBackground(new Color(250, 252, 255)); // Very light blue tint
                card.setBorder(new LineBorder(UIConstants.PRIMARY_COLOR, 1));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                card.setBackground(Color.WHITE);
                card.setBorder(new LineBorder(UIConstants.BORDER_COLOR, 1));
            }
        });

        return card;
    }

    /**
     * Creates avatar label - tries to load image from resources/avatars/, falls back to initials
     */
    private JLabel createAvatarLabel(String initials, Color avatarColor) {
        // Try to load avatar image from resources/avatars/{initials}.png or .jpg
        String[] extensions = {".png", ".jpg", ".jpeg"};
        BufferedImage avatarImage = null;
        
        for (String ext : extensions) {
            try {
                File imgFile = new File("resources/avatars/" + initials + ext);
                if (imgFile.exists()) {
                    avatarImage = ImageIO.read(imgFile);
                    break;
                }
            } catch (Exception e) {
                // Continue to next extension or fallback
            }
        }
        
        final BufferedImage finalImage = avatarImage;
        
        JLabel avatarLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                
                int size = Math.min(getWidth(), getHeight());
                
                if (finalImage != null) {
                    // Draw image clipped to circle
                    Ellipse2D.Double circle = new Ellipse2D.Double(0, 0, size, size);
                    g2.setClip(circle);
                    g2.drawImage(finalImage, 0, 0, size, size, null);
                } else {
                    // Draw Circle with Initials (fallback)
                    g2.setColor(avatarColor);
                    g2.fill(new Ellipse2D.Double(0, 0, size, size));
                    // Draw Initials (larger font for bigger avatar)
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, Math.max(38, size / 3)));
                    FontMetrics fm = g2.getFontMetrics();
                    int textX = (size - fm.stringWidth(initials)) / 2;
                    int textY = (size - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(initials, textX, textY);
                }
            }
        };
        
        return avatarLabel;
    }

    /**
     * Creates the bottom section with comprehensive system details.
     */
    private JPanel createSystemInfoPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UIConstants.BACKGROUND_COLOR);
        panel.setBorder(new CompoundBorder(
            new LineBorder(UIConstants.BORDER_COLOR, 1),
            new EmptyBorder(30, 40, 30, 40)
        ));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.insets = new Insets(5, 0, 5, 0);
        gbc.anchor = GridBagConstraints.CENTER;

        // System Title
        JLabel sysTitle = new JLabel("SmartStock Inventory Management System");
        sysTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        sysTitle.setForeground(UIConstants.PRIMARY_COLOR);
        panel.add(sysTitle, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(5, 0, 20, 0);
        JLabel tagline = new JLabel("Organize Your Business, Maximize Your Profits");
        tagline.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        tagline.setForeground(UIConstants.TEXT_SECONDARY);
        panel.add(tagline, gbc);

        // Details Section
        gbc.gridy++;
        gbc.insets = new Insets(8, 0, 8, 0);
        panel.add(createInfoRow("Version:", "1.0.0"), gbc);
        
        gbc.gridy++;
        panel.add(createInfoRow("Release:", "November, 2025"), gbc);
        
        gbc.gridy++;
        panel.add(createInfoRow("Technology Stack:", "Java 21, Swing GUI, MySQL 8.0"), gbc);
        
        gbc.gridy++;
        panel.add(createInfoRow("Libraries:", "ZXing (QR Code), Webcam Capture, JSON"), gbc);

        gbc.gridy++;
        panel.add(createInfoRow("Database:", "XAMPP, MySQL with JDBC Connector"), gbc);
        
        gbc.gridy++;
        gbc.insets = new Insets(20, 0, 5, 0);
        JLabel featuresTitle = new JLabel("Key Features");
        featuresTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        featuresTitle.setForeground(UIConstants.PRIMARY_COLOR);
        panel.add(featuresTitle, gbc);
        
        gbc.gridy++;
        gbc.insets = new Insets(5, 0, 5, 0);
        panel.add(createFeatureLabel("• Product Management with QR Code Generation & Scanning"), gbc);
        
        gbc.gridy++;
        panel.add(createFeatureLabel("• Real-time Inventory Tracking & Stock Alerts"), gbc);
        
        gbc.gridy++;
        panel.add(createFeatureLabel("• Sales & Transaction History with Date Filtering"), gbc);
        
        gbc.gridy++;
        panel.add(createFeatureLabel("• Dashboard Analytics (Best Sellers, Profit Margins, COGS)"), gbc);
        
        gbc.gridy++;
        panel.add(createFeatureLabel("• Multi-User Support (Admin & Employee Roles)"), gbc);
        
        gbc.gridy++;
        panel.add(createFeatureLabel("• Bulk Operations (Markup, Stock Removal, Returns, Delete)"), gbc);
        
        gbc.gridy++;
        panel.add(createFeatureLabel("• Store Settings & Employee Management"), gbc);

        gbc.gridy++;
        gbc.insets = new Insets(20, 0, 5, 0);
        JLabel supportTitle = new JLabel("Support & License");
        supportTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        supportTitle.setForeground(UIConstants.PRIMARY_COLOR);
        panel.add(supportTitle, gbc);
        
        gbc.gridy++;
        gbc.insets = new Insets(5, 0, 5, 0);
        panel.add(createInfoRow("University:", "Quezon City Univerity"), gbc);
        
        gbc.gridy++;
        panel.add(createInfoRow("Course:", "Bachelor of Science in Information Technology"), gbc);
        
        gbc.gridy++;
        panel.add(createInfoRow("Subject:", "Introduction to Programming (1st Semester, AY 2025-2026)"), gbc);

        gbc.gridy++;
        gbc.insets = new Insets(25, 0, 0, 0);
        JLabel copyright = new JLabel("© 2025 SmartStock Development Team. All Rights Reserved.");
        copyright.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        copyright.setForeground(UIConstants.TEXT_LIGHT);
        panel.add(copyright, gbc);

        return panel;
    }
    
    private JLabel createFeatureLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(UIConstants.TEXT_PRIMARY);
        return label;
    }

    private JPanel createInfoRow(String key, String value) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        row.setBackground(UIConstants.BACKGROUND_COLOR);
        
        JLabel k = new JLabel(key);
        k.setFont(new Font("Segoe UI", Font.BOLD, 14));
        k.setForeground(UIConstants.TEXT_SECONDARY);
        
        JLabel v = new JLabel(value);
        v.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        v.setForeground(UIConstants.TEXT_PRIMARY);
        
        row.add(k);
        row.add(v);
        return row;
    }
}