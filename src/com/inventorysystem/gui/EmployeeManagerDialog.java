package com.inventorysystem.gui;

import com.inventorysystem.data.UserRepository;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;

// Dialog for managing employees (add/remove)
public class EmployeeManagerDialog extends JDialog {
    
    private final userFrame mainFrame;
    private final DefaultListModel<String> listModel;
    private final JList<String> employeeList;

    public EmployeeManagerDialog(userFrame mainFrame) {
        super(mainFrame, "Manage Employees", true);
        this.mainFrame = mainFrame;
        
        setLayout(new BorderLayout(10, 10));
        setSize(600, 500);
        setLocationRelativeTo(mainFrame);

        // Employee list
        listModel = new DefaultListModel<>();
        employeeList = new JList<>(listModel);
        employeeList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        JPanel listPanel = new JPanel(new BorderLayout(10, 10));
        listPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        listPanel.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel("Employees Under Your Account");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        listPanel.add(titleLabel, BorderLayout.NORTH);
        listPanel.add(new JScrollPane(employeeList), BorderLayout.CENTER);

        add(listPanel, BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        loadEmployees();
    }

    // Load employees from database
    private void loadEmployees() {
        try {
            java.util.List<String> employees = new UserRepository().getEmployeesByAdmin(mainFrame.loggedInUserId);
            for (String emp : employees) {
                listModel.addElement(emp);
            }
        } catch (SQLException e) {
            listModel.addElement("Error loading employees: " + e.getMessage());
        }
    }

    // Buttons at bottom
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        panel.setBackground(Color.WHITE);

        JButton addBtn = createButton("Add Employee", UIConstants.PRIMARY_COLOR, UIConstants.PRIMARY_DARK);
        addBtn.addActionListener(e -> addEmployee());

        JButton removeBtn = createButton("Remove Selected", UIConstants.DANGER_COLOR, UIConstants.DANGER_DARK);
        removeBtn.addActionListener(e -> removeEmployee());

        JButton closeBtn = createButton("Close", UIConstants.TEXT_SECONDARY, new Color(100, 110, 120));
        closeBtn.addActionListener(e -> dispose());

        panel.add(addBtn);
        panel.add(removeBtn);
        panel.add(closeBtn);

        return panel;
    }

    // Create styled button
    private JButton createButton(String text, Color bgColor, Color hoverColor) {
        JButton button = new JButton(text);
        button.setFont(UIConstants.BUTTON_FONT);
        button.setPreferredSize(new Dimension(140, 38));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { button.setBackground(hoverColor); }
            public void mouseExited(MouseEvent e) { button.setBackground(bgColor); }
        });

        return button;
    }

    // Add new employee
    private void addEmployee() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JTextField usernameField = new JTextField(15);
        JPasswordField passwordField = new JPasswordField(15);

        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add New Employee",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            
            if (!username.isEmpty() && !password.isEmpty()) {
                try {
                    boolean success = new UserRepository().addEmployeeUnderAdmin(
                        mainFrame.loggedInUserId, username, password);
                    if (success) {
                        listModel.addElement(username);
                        JOptionPane.showMessageDialog(this, 
                            "Employee '" + username + "' added successfully!",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Username and password cannot be empty.",
                    "Input Error", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    // Remove selected employee
    private void removeEmployee() {
        String selected = employeeList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select an employee to remove.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Remove employee '" + selected + "'?\nThis will delete their account.",
            "Confirm Removal", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean success = new UserRepository().removeEmployee(selected);
                if (success) {
                    listModel.removeElement(selected);
                    JOptionPane.showMessageDialog(this, "Employee removed successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
