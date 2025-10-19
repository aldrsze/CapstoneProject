// DashboardPanel.java
package capstoneJavaGUI;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

public class dashboardPanel extends JPanel {

    public dashboardPanel() {
        setLayout(null); // Use null layout for precise component placement
        setBackground(Color.WHITE);

        // --- PANEL 1: TOTAL PRODUCTS ---
        JPanel totalProductsPanel = new JPanel();
        totalProductsPanel.setLayout(new BorderLayout());
        totalProductsPanel.setBounds(50, 50, 200, 300);
        totalProductsPanel.setBorder(new LineBorder(Color.BLACK));
        totalProductsPanel.setBackground(Color.WHITE);

        JLabel totalProductsLabel = new JLabel("TOTAL PRODUCTS", SwingConstants.CENTER);
        JLabel totalProductsValue = new JLabel("0", SwingConstants.CENTER);
        totalProductsValue.setFont(new Font("Arial", Font.BOLD, 48));

        totalProductsPanel.add(totalProductsLabel, BorderLayout.NORTH);
        totalProductsPanel.add(totalProductsValue, BorderLayout.CENTER);

        // --- PANEL 2: ITEMS OUT OF STOCK ---
        JPanel outOfStockPanel = new JPanel();
        outOfStockPanel.setLayout(new BorderLayout());
        outOfStockPanel.setBounds(280, 50, 200, 300);
        outOfStockPanel.setBorder(new LineBorder(Color.BLACK));
        outOfStockPanel.setBackground(Color.WHITE);

        JLabel outOfStockLabel = new JLabel("ITEMS OUT OF STOCK", SwingConstants.CENTER);
        JLabel outOfStockValue = new JLabel("0", SwingConstants.CENTER);
        outOfStockValue.setFont(new Font("Arial", Font.BOLD, 48));

        outOfStockPanel.add(outOfStockLabel, BorderLayout.NORTH);
        outOfStockPanel.add(outOfStockValue, BorderLayout.CENTER);


        // --- PANEL 3: TOTAL INCOME ---
        JPanel totalIncomePanel = new JPanel();
        totalIncomePanel.setLayout(new BorderLayout());
        totalIncomePanel.setBounds(510, 50, 200, 300);
        totalIncomePanel.setBorder(new LineBorder(Color.BLACK));
        totalIncomePanel.setBackground(Color.WHITE);

        JLabel totalIncomeLabel = new JLabel("TOTAL INCOME", SwingConstants.CENTER);
        JLabel totalIncomeValue = new JLabel("₱0", SwingConstants.CENTER);
        totalIncomeValue.setFont(new Font("Arial", Font.BOLD, 48));

        totalIncomePanel.add(totalIncomeLabel, BorderLayout.NORTH);
        totalIncomePanel.add(totalIncomeValue, BorderLayout.CENTER);

        // --- ADD ALL PANELS TO THE MAIN PANEL ---
        add(totalProductsPanel);
        add(outOfStockPanel);
        add(totalIncomePanel);
    }
}