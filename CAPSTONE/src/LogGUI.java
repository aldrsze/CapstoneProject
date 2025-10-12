// AuthUI.java
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class LogGUI {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> createAndShowGUI());
    }

    private static void createAndShowGUI() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}

        JFrame frame = new JFrame("Inventory and Sales Management System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);

        // CardLayout for switching between Login and SignUp
        JPanel container = new JPanel(new CardLayout());
        BackgroundPanel loginPanel = new BackgroundPanel("login", container);
        BackgroundPanel signupPanel = new BackgroundPanel("signup", container);

        container.add(loginPanel, "login");
        container.add(signupPanel, "signup");

        frame.setContentPane(container);
        frame.setVisible(true);
    }


    static class BackgroundPanel extends JPanel {
        BackgroundPanel(String type, JPanel container) {
            setLayout(new GridBagLayout());
            RoundedPanel card = new RoundedPanel(type, container);
            add(card);
        }


    }


    static class RoundedPanel extends JPanel {
        RoundedPanel(String type, JPanel container) {
            setOpaque(false);
            setPreferredSize(new Dimension(360, 500));
            setLayout(new GridBagLayout());
            setBorder(new EmptyBorder(18, 18, 18, 18));
            buildContent(type, container);
        }

        private void buildContent(String type, JPanel container) {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.weightx = 1.0;


            JLabel title = new JLabel(type.equals("login") ? "User Login" : "Sign-Up", SwingConstants.CENTER);
            title.setFont(new Font("SansSerif", Font.BOLD, 36));
            gbc.gridy = 0;
            gbc.insets = new Insets(40, 10, 6, 10);
            add(title, gbc);


            JLabel switchLabel;
            if (type.equals("login")) {
                switchLabel = new JLabel("<html><div style='text-align:center'>Don't have an account? <a href=''>Sign Up</a></div></html>", SwingConstants.CENTER);
            } else {
                switchLabel = new JLabel("<html><div style='text-align:center'>Already have an account? <a href=''>Login</a></div></html>", SwingConstants.CENTER);
            }

            switchLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
            switchLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            gbc.gridy = 1;
            gbc.insets = new Insets(0, 10, 18, 10);
            add(switchLabel, gbc);

            switchLabel.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    CardLayout cl = (CardLayout) container.getLayout();
                    cl.show(container, type.equals("login") ? "signup" : "login");
                }
            });


            JPanel form = new JPanel(new GridBagLayout());
            form.setOpaque(false);
            GridBagConstraints f = new GridBagConstraints();
            f.gridx = 0;
            f.anchor = GridBagConstraints.WEST;
            f.insets = new Insets(6, 0, 6, 0);


            JLabel userLabel = new JLabel("Username:");
            userLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
            f.gridy = 0;
            form.add(userLabel, f);

            JTextField userField = new JTextField();
            userField.setPreferredSize(new Dimension(260, 30));
            f.gridy = 1;
            form.add(userField, f);


            JLabel passLabel = new JLabel("Password:");
            passLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
            f.gridy = 2;
            form.add(passLabel, f);

            JPasswordField passField = new JPasswordField();
            passField.setPreferredSize(new Dimension(260, 30));
            f.gridy = 3;
            form.add(passField, f);


            JComboBox<String> roleBox = null;
            if (type.equals("signup")) {
                JLabel roleLabel = new JLabel("User Role:");
                roleLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
                f.gridy = 4;
                form.add(roleLabel, f);

                roleBox = new JComboBox<>(new String[]{"Admin", "Employee"});
                roleBox.setPreferredSize(new Dimension(260, 30));
                f.gridy = 5;
                form.add(roleBox, f);
            }

            gbc.gridy = 2;
            gbc.insets = new Insets(0, 10, 12, 10);
            add(form, gbc);


            JButton actionBtn = new JButton(type.equals("login") ? "Login" : "Sign Up");
            actionBtn.setFont(new Font("SansSerif", Font.PLAIN, 14));
            actionBtn.setPreferredSize(new Dimension(120, 36));
            gbc.gridy = 3;
            gbc.insets = new Insets(8, 10, 28, 10);
            add(actionBtn, gbc);


            JComboBox<String> finalRoleBox = roleBox;
            actionBtn.addActionListener(e -> {
                String username = userField.getText();
                String password = new String(passField.getPassword());

                if (type.equals("login")) {
                    // Example check (replace with real authentication later)
                    if (!username.isEmpty() && !password.isEmpty()) {
                        ((JFrame) SwingUtilities.getWindowAncestor(this)).dispose(); // close login window
                        new mainPage(username); // open dashboard
                    } else {
                        JOptionPane.showMessageDialog(this, "Please enter credentials");
                    }
                } else {
                    String role = (String) finalRoleBox.getSelectedItem();
                    JOptionPane.showMessageDialog(this, "Sign-Up clicked\nUser: " + username + "\nRole: " + role);
                }
            });

        }


    }
}
