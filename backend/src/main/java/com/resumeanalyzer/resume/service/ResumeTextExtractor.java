package com.resumeanalyzer.resume.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

@Component
public class ResumeTextExtractor {

    public String extract(byte[] content, String extension) {
        try {
            String text = switch (extension.toLowerCase(Locale.ROOT)) {
                case "pdf" -> extractPdf(content);
                case "docx" -> extractDocx(content);
                default -> throw new ResumeException(
                        ResumeException.Reason.INVALID_FILE,
                        "Only PDF and DOCX resumes are supported");
            };
            String normalized = normalize(text);
            if (normalized.isBlank()) {
                throw new ResumeException(
                        ResumeException.Reason.EXTRACTION_FAILED,
                        "No text could be extracted; scanned or empty resumes are not supported");
            }
            return normalized;
        } catch (ResumeException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new ResumeException(
                    ResumeException.Reason.EXTRACTION_FAILED,
                    "The file could not be read as a valid, unprotected PDF or DOCX resume",
                    exception);
        }
    }

    private String extractPdf(byte[] content) throws IOException {
        try (PDDocument document = Loader.loadPDF(content)) {
            if (document.isEncrypted()) {
                throw new IOException("Encrypted PDF");
            }
            return new PDFTextStripper().getText(document);
        }
    }

    private String extractDocx(byte[] content) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(content));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String normalize(String text) {
        return text.replace("\r\n", "\n")
                .replace('\r', '\n')
                .lines()
                .map(String::stripTrailing)
                .collect(Collectors.joining("\n"))
                .strip();
    }
}
