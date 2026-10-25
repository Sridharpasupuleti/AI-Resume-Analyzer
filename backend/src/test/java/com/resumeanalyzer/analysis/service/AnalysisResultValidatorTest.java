package com.resumeanalyzer.analysis.service;

import java.util.List;

import com.resumeanalyzer.analysis.dto.ResumeAnalysisResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnalysisResultValidatorTest {

    private final AnalysisResultValidator validator = new AnalysisResultValidator();

    @Test
    void acceptsWellFormedResult() {
        ResumeAnalysisResult result = new ResumeAnalysisResult(
                80,
                "Good skill coverage.",
                List.of("Java", "Spring", "SQL", "REST"),
                List.of("Docker"),
                List.of("backend"),
                List.of("leadership"),
                List.of("Highlight relevant projects."));

        assertEquals(result, validator.validate(result));
    }

    @Test
    void rejectsOutOfRangeScore() {
        ResumeAnalysisResult result = new ResumeAnalysisResult(
                101, "Summary", List.of(), List.of(), List.of(), List.of(), List.of());

        AnalysisException exception = assertThrows(
                AnalysisException.class,
                () -> validator.validate(result));

        assertEquals(AnalysisException.Reason.INVALID_RESULT, exception.getReason());
    }

    @Test
    void rejectsMissingOrBlankResultFields() {
        assertThrows(AnalysisException.class, () -> validator.validate(null));
        assertThrows(AnalysisException.class, () -> validator.validate(
                new ResumeAnalysisResult(50, " ", List.of(), List.of(), List.of(), List.of(), List.of())));
        assertThrows(AnalysisException.class, () -> validator.validate(
                new ResumeAnalysisResult(50, "Summary", null, List.of(), List.of(), List.of(), List.of())));
    }

    @Test
    void acceptsModelScoreThatReflectsTheOverallAssessment() {
        ResumeAnalysisResult result = new ResumeAnalysisResult(
                75,
                "The overall fit is good, with an important missing skill.",
                List.of("Java"),
                List.of("Docker"),
                List.of(),
                List.of(),
                List.of());

        assertEquals(result, validator.validate(result));
    }

    @Test
    void rejectsDuplicateOrConflictingEvidence() {
        ResumeAnalysisResult result = new ResumeAnalysisResult(
                100,
                "Summary",
                List.of("Java", "Java"),
                List.of(),
                List.of(),
                List.of(),
                List.of());

        assertThrows(AnalysisException.class, () -> validator.validate(result));
    }
}
