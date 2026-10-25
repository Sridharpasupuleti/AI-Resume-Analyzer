package com.resumeanalyzer.resume.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResumeTextExtractorTest {

    private final ResumeTextExtractor extractor = new ResumeTextExtractor();

    @Test
    void extractsAndNormalizesDocxText() throws IOException {
        byte[] content;
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("  Casey Example  ");
            document.createParagraph().createRun().setText("Java Engineer");
            document.write(output);
            content = output.toByteArray();
        }

        assertEquals("Casey Example\nJava Engineer", extractor.extract(content, "docx"));
    }

    @Test
    void extractsPdfText() throws IOException {
        byte[] content;
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 700);
                stream.showText("Casey Example");
                stream.endText();
            }
            document.save(output);
            content = output.toByteArray();
        }

        assertEquals("Casey Example", extractor.extract(content, "pdf"));
    }

    @Test
    void rejectsFilesThatCannotBeParsed() {
        ResumeException exception = assertThrows(
                ResumeException.class,
                () -> extractor.extract(new byte[] {1, 2, 3, 4}, "pdf"));

        assertEquals(ResumeException.Reason.EXTRACTION_FAILED, exception.getReason());
    }

    @Test
    void rejectsFilesWithNoExtractableText() throws IOException {
        byte[] content;
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.write(output);
            content = output.toByteArray();
        }

        ResumeException exception = assertThrows(
                ResumeException.class,
                () -> extractor.extract(content, "docx"));

        assertEquals(ResumeException.Reason.EXTRACTION_FAILED, exception.getReason());
    }
}
