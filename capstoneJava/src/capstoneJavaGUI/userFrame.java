package capstoneJavaGUI;
//UserFrame.java
import java.awt.*;
import javax.swing.*;

public class userFrame {

 public static void main(String[] args) {
     // Run the GUI creation on the Event Dispatch Thread for thread safety
     SwingUtilities.invokeLater(() -> createAndShowGUI());
 }

 private static void createAndShowGUI() {
     try {
         // Set the look and feel to the system's default for a native appearance
         UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
     } catch (Exception e) {
         e.printStackTrace();
     }

     JFrame frame = new JFrame("Inventory and Sales Management");
     frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
     frame.setSize(500, 550);
     frame.setLocationRelativeTo(null); // Center the frame on the screen

     // This panel uses CardLayout to switch between the login and sign-up views
     JPanel mainContainer = new JPanel(new CardLayout());

     // Create instances of our new, separate panels
     LoginPanel loginPanel = new LoginPanel(mainContainer);
     SignupPanel signupPanel = new SignupPanel(mainContainer);

     // Add the panels to the container, giving each a unique name to switch to
     mainContainer.add(loginPanel, "login");
     mainContainer.add(signupPanel, "signup");

     frame.add(mainContainer);
     frame.setVisible(true);
 }
}