import com.inventorysystem.gui.userFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

// Application entry point
public class App {

    public static void main(String[] args) {
        
        SwingUtilities.invokeLater(() -> {
            try {
                // Native OS appearance
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                
                // Smooth rendering
                System.setProperty("awt.useSystemAAFontSettings", "on");
                System.setProperty("swing.aatext", "true");
                
            } catch (Exception e) { 
                e.printStackTrace();
            }
            
            // Show main window
            new userFrame().setVisible(true);
        });
    }
}