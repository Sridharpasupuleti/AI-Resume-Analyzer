package com.resumeanalyzer.analysis.dto;

import java.time.Instant;

public record AnalysisResponse(
        Long id,
        Long resumeId,
        String resumeTitle,
        Long jobId,
        String jobTitle,
        ResumeAnalysisResult result,
        Instant createdAt) {
}
