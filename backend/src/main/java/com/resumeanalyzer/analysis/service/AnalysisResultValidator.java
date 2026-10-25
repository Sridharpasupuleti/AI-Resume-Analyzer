package com.resumeanalyzer.analysis.service;

import java.util.List;

import com.resumeanalyzer.analysis.dto.ResumeAnalysisResult;
import org.springframework.stereotype.Component;

@Component
public class AnalysisResultValidator {

    public ResumeAnalysisResult validate(ResumeAnalysisResult result) {
        if (result == null
                || result.overallScore() < 0
                || result.overallScore() > 100
                || result.summary() == null
                || result.summary().isBlank()
                || result.summary().length() > 1000
                || !validItems(result.matchingSkills(), 60)
                || !validItems(result.missingSkills(), 60)
                || !validItems(result.matchingKeywords(), 10)
                || !validItems(result.missingKeywords(), 10)
                || !validItems(result.recommendations(), 10)
                || !disjoint(result.matchingSkills(), result.missingSkills())
                || !disjoint(result.matchingKeywords(), result.missingKeywords())) {
            throw new AnalysisException(
                    AnalysisException.Reason.INVALID_RESULT,
                    "The analysis service returned an invalid result");
        }
        return result;
    }

    private boolean disjoint(List<String> first, List<String> second) {
        return first.stream().noneMatch(second::contains);
    }

    private boolean validItems(List<String> items, int maxSize) {
        return items != null
                && items.size() <= maxSize
                && items.stream().allMatch(item -> item != null && !item.isBlank() && item.length() <= 500)
                && items.stream().distinct().count() == items.size();
    }
}
