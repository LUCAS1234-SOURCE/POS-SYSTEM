import net.sourceforge.barbecue.Barcode;
import net.sourceforge.barbecue.BarcodeFactory;
import net.sourceforge.barbecue.BarcodeImageHandler;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class BarcodeUtil {
    public static Image generateBarcodeImage(String barcodeText) {
        try {
            Barcode barcode = BarcodeFactory.createCode128(barcodeText);
            barcode.setBarWidth(2);
            barcode.setBarHeight(50);
            barcode.setDrawingText(true);
            return BarcodeImageHandler.getImage(barcode);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static void showBarcodeDialog(Component parent, String barcodeText) {
        Image barcodeImage = generateBarcodeImage(barcodeText);
        if (barcodeImage != null) {
            JDialog dialog = new JDialog();
            dialog.setTitle("Barcode: " + barcodeText);
            dialog.setModal(true);
            
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(new JLabel(new ImageIcon(barcodeImage)), BorderLayout.CENTER);
            panel.add(new JLabel(barcodeText, JLabel.CENTER), BorderLayout.SOUTH);
            
            dialog.add(panel);
            dialog.pack();
            dialog.setLocationRelativeTo(parent);
            dialog.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(parent, 
                    "Failed to generate barcode", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}