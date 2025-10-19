package capstoneJavaGUI;

//SignupPanel.java
import javax.swing.*;
import java.awt.*;

public class SignupPanel extends JPanel {

 public SignupPanel(JPanel container) {
     setLayout(new GridBagLayout());
     GridBagConstraints gbc = new GridBagConstraints();
     gbc.fill = GridBagConstraints.HORIZONTAL;
     gbc.insets = new Insets(8, 12, 8, 12);

     // --- Title ---
     JLabel titleLabel = new JLabel("Sign Up", SwingConstants.CENTER);
     titleLabel.setFont(UIConstants.TITLE_FONT);
     gbc.gridx = 0;
     gbc.gridy = 0;
     gbc.gridwidth = 2;
     gbc.insets = new Insets(25, 10, 25, 10);
     add(titleLabel, gbc);

     // Reset constraints for form fields
     gbc.gridwidth = 1;
     gbc.insets = new Insets(8, 12, 8, 12);

     // --- Form Fields ---
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

     JLabel roleLabel = new JLabel("User Role:");
     roleLabel.setFont(UIConstants.LABEL_FONT);
     gbc.gridy = 5;
     add(roleLabel, gbc);

     JComboBox<String> roleBox = new JComboBox<>(new String[]{"Admin", "Employee"});
     roleBox.setFont(UIConstants.INPUT_FONT);
     gbc.gridy = 6;
     add(roleBox, gbc);

     // --- Button Panel ---
     JPanel buttonPanel = createButtonPanel(container, userField, passField, roleBox);
     gbc.gridy = 7;
     gbc.fill = GridBagConstraints.NONE;
     gbc.insets = new Insets(25, 10, 20, 10);
     add(buttonPanel, gbc);
 }

 private JPanel createButtonPanel(JPanel container, JTextField userField, JPasswordField passField, JComboBox<String> roleBox) {
     JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));

     JButton signupButton = new JButton("Sign Up");
     signupButton.setFont(UIConstants.BUTTON_FONT);
     signupButton.setPreferredSize(UIConstants.BUTTON_DIMENSION);

     JButton switchToLoginButton = new JButton("Login");
     switchToLoginButton.setFont(UIConstants.BUTTON_FONT);
     switchToLoginButton.setPreferredSize(UIConstants.BUTTON_DIMENSION);
     
     panel.add(signupButton);
     panel.add(switchToLoginButton);

     // Action for the Sign-Up button
     signupButton.addActionListener(e -> {
         String username = userField.getText();
         String password = new String(passField.getPassword());
         if (username.isEmpty() || password.isEmpty()) {
             JOptionPane.showMessageDialog(this, "Please fill in all fields.", "Input Error", JOptionPane.WARNING_MESSAGE);
             return;
         }
         String role = (String) roleBox.getSelectedItem();
         System.out.println("Sign-up attempt: " + username + ", Role: " + role);
         JOptionPane.showMessageDialog(this, "Account created for " + username + " as " + role);
     });

     // Action to switch back to the Login panel
     switchToLoginButton.addActionListener(e -> {
         CardLayout cl = (CardLayout) container.getLayout();
         cl.show(container, "login");
     });
     
     return panel;
 }
}