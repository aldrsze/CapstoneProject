package com.inventorysystem.gui;

import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class CustomTableRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        // Alignment
        setHorizontalAlignment(JLabel.CENTER);
        
        // Fonts
        if (column == 0) { // Row numbers
            setFont(new Font("Segoe UI", Font.PLAIN, 10));
            if (!isSelected) setForeground(Color.GRAY);
        } else {
            setFont(UIConstants.TABLE_FONT);
            setForeground(isSelected ? Color.WHITE : UIConstants.TEXT_PRIMARY);
        }

        // Colors
        if (isSelected) {
            c.setBackground(UIConstants.PRIMARY_LIGHT);
        } else {
            c.setBackground(row % 2 == 0 ? Color.WHITE : UIConstants.BACKGROUND_COLOR);
        }
        
        // Padding
        ((JLabel) c).setBorder(new EmptyBorder(0, 10, 0, 10));
        
        // Special logic for Stock Status (specific to Stock Panel, safe to keep here if generic)
        if (value != null) {
            String s = value.toString();
            if (s.equals("Out of Stock")) c.setForeground(new Color(220, 53, 69));
            else if (s.equals("Low Stock")) c.setForeground(new Color(255, 140, 0));
            else if (s.equals("Normal")) c.setForeground(new Color(40, 167, 69));
        }

        return c;
    }
}