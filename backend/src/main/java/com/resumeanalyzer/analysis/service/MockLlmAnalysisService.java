package com.resumeanalyzer.analysis.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import com.resumeanalyzer.analysis.dto.ResumeAnalysisResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.analysis.provider", havingValue = "mock")
public class MockLlmAnalysisService implements LlmAnalysisService {

    private static final List<String> SKILLS = List.of(
            "spring boot", "spring", "java", "python", "javascript", "typescript",
            "node.js", "react", "angular", "vue", "postgresql", "mysql", "mongodb",
            "redis", "microservices", "docker", "kubernetes", "aws", "azure", "gcp",
            "git", "linux", "maven", "gradle", "junit", "hibernate", "jpa", "sql", "rest api", "rest", "kafka",
            "rabbitmq", "jenkins", "graphql", "oauth", "jwt", "machine learning",
            "deep learning", "pytorch", "tensorflow", "pandas", "numpy", "html",
            "css", "c++", "c#", ".net", "go", "rust", "agile", "scrum");

    private static final Set<String> STOP_WORDS = Set.of(
            "about", "above", "after", "again", "against", "and", "are", "around",
            "been", "before", "being", "below", "between", "both", "build", "can",
            "could", "deliver", "description", "develop", "experience", "for", "from",
            "have", "into", "job", "must", "need", "other", "our", "role", "seeking",
            "should", "such",
            "that", "their", "them", "then", "there", "these", "they", "this", "those",
            "through", "under", "using", "very", "want", "will", "with", "work", "years",
            "your");

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{L}\\p{N}+#]+");

    @Override
    public ResumeAnalysisResult analyze(String resumeText, String jobDescription) {
        String resume = resumeText.toLowerCase(Locale.ROOT);
        String job = jobDescription.toLowerCase(Locale.ROOT);

        List<String> requiredSkills = SKILLS.stream()
                .filter(skill -> containsSkill(job, skill))
                .filter(skill -> !skill.equals("spring") || !containsSkill(job, "spring boot"))
                .filter(skill -> !skill.equals("rest") || !containsSkill(job, "rest api"))
                .toList();
        List<String> matchingSkills = requiredSkills.stream()
                .filter(skill -> containsSkill(resume, skill))
                .toList();
        List<String> missingSkills = requiredSkills.stream()
                .filter(skill -> !containsSkill(resume, skill))
                .toList();

        Set<String> jobKeywords = keywords(jobDescription);
        Set<String> resumeKeywords = keywords(resumeText);
        List<String> sampledKeywords = jobKeywords.stream()
                .limit(10)
                .toList();
        List<String> matchingKeywords = sampledKeywords.stream()
                .filter(resumeKeywords::contains)
                .toList();
        List<String> missingKeywords = sampledKeywords.stream()
                .filter(keyword -> !resumeKeywords.contains(keyword))
                .toList();

        int keywordScore = sampledKeywords.isEmpty()
                ? 0
                : (int) Math.round(100.0 * matchingKeywords.size() / sampledKeywords.size());
        int score = requiredSkills.isEmpty()
                ? keywordScore
                : (int) Math.round(100.0 * matchingSkills.size() / requiredSkills.size());
        String summary = requiredSkills.isEmpty()
                ? "The resume shares " + matchingKeywords.size()
                        + " of the job description's " + sampledKeywords.size()
                        + " sampled keywords. This is a basic keyword comparison, not an AI assessment."
                : "The resume includes " + matchingSkills.size() + " of "
                        + requiredSkills.size() + " identified job skills. This is a basic keyword comparison, "
                        + "not an AI assessment.";

        List<String> recommendations = recommendations(missingSkills, matchingKeywords);
        return new ResumeAnalysisResult(
                score,
                summary,
                matchingSkills,
                missingSkills,
                matchingKeywords,
                missingKeywords,
                recommendations);
    }

    private boolean containsSkill(String text, String skill) {
        String regex = "(?<![\\p{L}\\p{N}])"
                + Pattern.quote(skill).replace(" ", "\\E\\s+\\Q")
                + "(?![\\p{L}\\p{N}])";
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                .matcher(text)
                .find();
    }

    private Set<String> keywords(String text) {
        Set<String> result = new LinkedHashSet<>();
        var matcher = TOKEN_PATTERN.matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find() && result.size() < 30) {
            String token = matcher.group();
            if (token.length() >= 3 && !STOP_WORDS.contains(token)) {
                result.add(token);
            }
        }
        return result;
    }

    private List<String> recommendations(List<String> missingSkills, List<String> matchingKeywords) {
        List<String> result = new ArrayList<>();
        if (!missingSkills.isEmpty()) {
            result.add("If accurate, add evidence of these job skills to the resume: "
                    + String.join(", ", missingSkills) + ".");
        }
        if (matchingKeywords.isEmpty()) {
            result.add("Review the job description and tailor the resume to relevant requirements it genuinely meets.");
        } else {
            result.add("Use concrete examples to demonstrate the matching experience: "
                    + String.join(", ", matchingKeywords) + ".");
        }
        return result;
    }
}
