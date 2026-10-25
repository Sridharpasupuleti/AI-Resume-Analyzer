package com.resumeanalyzer.job.dto;

import java.time.Instant;

public record JobResponse(
        Long id,
        String companyName,
        String jobTitle,
        String jobDescription,
        String jobUrl,
        String location,
        String source,
        Instant createdAt,
        Instant updatedAt) {
}
