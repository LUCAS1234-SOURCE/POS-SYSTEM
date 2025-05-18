import javax.swing.*;
import java.awt.*;

public class POSApplication {
    public static void main(String[] args) {
       
        DatabaseSetup.initializeDatabase();
        
        
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}