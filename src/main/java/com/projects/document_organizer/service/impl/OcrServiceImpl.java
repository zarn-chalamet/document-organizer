package com.projects.document_organizer.service.impl;

import com.projects.document_organizer.service.OcrService;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.apache.pdfbox.Loader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class OcrServiceImpl implements OcrService {

    private static final String TESSERACT_DATA_PATH =
            "C:\\Program Files\\Tesseract-OCR\\tessdata";

    private Tesseract buildTesseract() {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(TESSERACT_DATA_PATH);
        tesseract.setLanguage("eng");
        tesseract.setPageSegMode(1);
        tesseract.setOcrEngineMode(1);
        return tesseract;
    }

    @Override
    public String extractTextFromImage(byte[] imageBytes) {
        try {
            Tesseract tesseract = buildTesseract();
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                log.warn("Could not read image bytes");
                return "";
            }
            return tesseract.doOCR(image);
        } catch (TesseractException | IOException e) {
            log.error("OCR failed for image: {}", e.getMessage());
            return "";
        }
    }

    @Override
    public List<String> extractTextFromPdf(byte[] pdfBytes) {
        List<String> pageTexts = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            Tesseract tesseract = buildTesseract();
            int pageCount = document.getNumberOfPages();

            log.info("Processing PDF with {} pages", pageCount);

            for (int i = 0; i < pageCount; i++) {
                try {
                    // Render page to image at 300 DPI (good quality for OCR)
                    BufferedImage pageImage = renderer.renderImageWithDPI(i, 300);

                    // Convert BufferedImage to bytes for Tesseract
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(pageImage, "PNG", baos);

                    String pageText = tesseract.doOCR(pageImage);
                    pageTexts.add(pageText != null ? pageText : "");
                    log.info("Page {} OCR complete ({} chars)", i + 1, pageText != null ? pageText.length() : 0);

                } catch (TesseractException e) {
                    log.error("OCR failed on page {}: {}", i + 1, e.getMessage());
                    pageTexts.add("");
                }
            }

        } catch (IOException e) {
            log.error("Failed to load PDF: {}", e.getMessage());
        }

        return pageTexts;
    }
}