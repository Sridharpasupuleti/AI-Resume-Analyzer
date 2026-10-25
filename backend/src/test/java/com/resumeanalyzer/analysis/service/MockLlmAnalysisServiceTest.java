package com.resumeanalyzer.analysis.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MockLlmAnalysisServiceTest {

    private final MockLlmAnalysisService analysisService = new MockLlmAnalysisService();

    @Test
    void returnsDeterministicSkillsKeywordsAndScore() {
        var result = analysisService.analyze(
                "Experienced Java engineer with Spring Boot and Docker experience.",
                "We need a Java developer with Spring Boot, Docker, and AWS experience.");

        assertEquals(75, result.overallScore());
        assertEquals("The resume includes 3 of 4 identified job skills. This is a basic keyword comparison, "
                + "not an AI assessment.", result.summary());
        assertEquals(java.util.List.of("spring boot", "java", "docker"), result.matchingSkills());
        assertEquals(java.util.List.of("aws"), result.missingSkills());
        assertEquals(java.util.List.of("java", "spring", "boot", "docker"), result.matchingKeywords());
        assertEquals(java.util.List.of("developer", "aws"), result.missingKeywords());
    }

    @Test
    void doesNotMatchSkillNamesInsideLongerWords() {
        var result = analysisService.analyze(
                "Google engineer using JavaScript.",
                "Must know Java and Go.");

        assertEquals(java.util.List.of(), result.matchingSkills());
        assertEquals(java.util.List.of("java", "go"), result.missingSkills());
        assertEquals(0, result.overallScore());
    }

    @Test
    void fallsBackToKeywordComparisonWhenNoKnownSkillsArePresent() {
        var result = analysisService.analyze(
                "Led customer onboarding and support.",
                "Seeking customer support and onboarding experience.");

        assertEquals(100, result.overallScore());
        assertEquals(java.util.List.of(), result.matchingSkills());
        assertEquals(java.util.List.of(), result.missingSkills());
        assertEquals(java.util.List.of("customer", "support", "onboarding"), result.matchingKeywords());
    }
}
