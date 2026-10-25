package com.resumeanalyzer.resume.dto;

import java.time.Instant;

public record ResumeDetails(
        Long id,
        String title,
        String originalFileName,
        String fileType,
        String extractedText,
        Instant createdAt) {
}
