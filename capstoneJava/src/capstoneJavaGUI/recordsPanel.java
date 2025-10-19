// RecordsPanel.java
package capstoneJavaGUI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class recordsPanel extends JPanel {

    public recordsPanel() {
        setLayout(null); // Use null layout for precise component placement
        setBackground(Color.WHITE);

        // --- TITLE ---
        JLabel titleLabel = new JLabel("TRANSACTION HISTORY");
        titleLabel.setBounds(30, 20, 300, 30);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        add(titleLabel);
        
        JLabel searchLabel = new JLabel("Search: ");
        searchLabel.setBounds(520, 20, 60, 30);
        searchLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        JTextField searchField = new JTextField();
        searchField.setBounds(580, 25, 190, 20);
        searchField.setFont(new Font("Arial", Font.PLAIN, 14));
        
        // --- TRANSACTION TABLE ---
        String[] columnNames = {"Product id", "Product name", "Category", "Price", "Stock", "DATE / TIME"};
        Object[][] data = {}; // Empty data for the table
        DefaultTableModel model = new DefaultTableModel(data, columnNames);
        JTable recordsTable = new JTable(model);

        // Make columns wider
        recordsTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        recordsTable.getColumnModel().getColumn(5).setPreferredWidth(120);
        
        add(searchLabel);
        add(searchField);

        JScrollPane tableScrollPane = new JScrollPane(recordsTable);
        tableScrollPane.setBounds(30, 60, 740, 450);
        add(tableScrollPane);
    }
}