import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class mainPage {
    private JFrame frame;
    private JPanel contentPanel;

    public mainPage(String username) {
        frame = new JFrame("Management System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());


        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(20, 10, 20, 10));
        sidebar.setPreferredSize(new Dimension(200, 600));

        JLabel title = new JLabel("<html><div style='text-align:center;'>INVENTORY MANAGEMENT<br>SYSTEM</div></html>", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 17));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel userLabel = new JLabel("USER: " + username);
        userLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        userLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton homeBtn = new JButton("Home");
        JButton prodBtn = new JButton("Products");
        JButton salesBtn = new JButton("Sales");
        JButton histBtn = new JButton("History");
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setAlignmentX(Component.CENTER_ALIGNMENT);


        for (JButton btn : new JButton[]{homeBtn, prodBtn, salesBtn, histBtn}) {
            btn.setMaximumSize(new Dimension(140, 40));
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        sidebar.add(title);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(userLabel);
        sidebar.add(Box.createVerticalStrut(20));
        sidebar.add(homeBtn);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(prodBtn);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(salesBtn);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(histBtn);
        sidebar.add(Box.createVerticalStrut(250));
        sidebar.add(logoutBtn);


        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        showPage("HOME");


        homeBtn.addActionListener(e -> showPage("HOME"));
        prodBtn.addActionListener(e -> showPage("PRODUCT LIST"));
        salesBtn.addActionListener(e -> showPage("SALES"));
        histBtn.addActionListener(e -> showPage("TRANSACTION HISTORY"));

        logoutBtn.addActionListener(e -> {
            frame.dispose();
            LogGUI.main(null);
        });

        frame.add(sidebar, BorderLayout.WEST);
        frame.add(contentPanel, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private void showPage(String title) {
        contentPanel.removeAll();

        JPanel page = new JPanel();
        page.setBackground(Color.WHITE);
        page.setLayout(new BorderLayout());
        page.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel lbl = new JLabel(title, SwingConstants.LEFT);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 18));
        page.add(lbl, BorderLayout.NORTH);

        contentPanel.add(page, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }
}
