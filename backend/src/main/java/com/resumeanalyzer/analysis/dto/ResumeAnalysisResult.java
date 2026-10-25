package com.resumeanalyzer.analysis.dto;

import java.util.List;

public record ResumeAnalysisResult(
        int overallScore,
        String summary,
        List<String> matchingSkills,
        List<String> missingSkills,
        List<String> matchingKeywords,
        List<String> missingKeywords,
        List<String> recommendations) {
}
