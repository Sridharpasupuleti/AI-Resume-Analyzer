package com.resumeanalyzer.analysis.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateAnalysisRequest(
        @NotNull @Positive Long resumeId,
        @NotNull @Positive Long jobId) {
}
