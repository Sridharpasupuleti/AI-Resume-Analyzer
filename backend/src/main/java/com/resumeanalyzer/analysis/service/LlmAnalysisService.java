package com.resumeanalyzer.analysis.service;

import com.resumeanalyzer.analysis.dto.ResumeAnalysisResult;

public interface LlmAnalysisService {

    ResumeAnalysisResult analyze(String resumeText, String jobDescription);
}
