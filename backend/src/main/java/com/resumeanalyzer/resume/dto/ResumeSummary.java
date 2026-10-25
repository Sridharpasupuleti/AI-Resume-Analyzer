package com.resumeanalyzer.resume.dto;

import java.time.Instant;

public record ResumeSummary(
        Long id,
        String title,
        String originalFileName,
        String fileType,
        Instant createdAt) {
}
