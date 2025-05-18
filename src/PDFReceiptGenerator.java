

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PDFReceiptGenerator {
    
    public static void generateReceipt(String filePath, String[] items, double total) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
                
                
                contentStream.beginText();
                contentStream.newLineAtOffset(100, 700);
                contentStream.showText("YOUR STORE NAME");
                contentStream.endText();
                
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(100, 680);
                contentStream.showText("Receipt #" + System.currentTimeMillis() % 10000);
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Date: " + LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                contentStream.endText();
                
               
                contentStream.beginText();
                contentStream.newLineAtOffset(100, 640);
                contentStream.showText("ITEMS PURCHASED:");
                contentStream.endText();
                
                int yPosition = 620;
                for (String item : items) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(100, yPosition);
                    contentStream.showText(item);
                    contentStream.endText();
                    yPosition -= 20;
                }
                
               
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                contentStream.beginText();
                contentStream.newLineAtOffset(100, yPosition - 40);
                contentStream.showText(String.format("TOTAL: $%.2f", total));
                contentStream.endText();
            }
            
           
            document.save(filePath);
        }
    }
}