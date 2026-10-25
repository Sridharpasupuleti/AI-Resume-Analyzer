package com.resumeanalyzer.analysis.service;

import java.util.List;

import com.resumeanalyzer.analysis.dto.AnalysisResponse;
import com.resumeanalyzer.analysis.dto.CreateAnalysisRequest;
import com.resumeanalyzer.analysis.dto.ResumeAnalysisResult;
import com.resumeanalyzer.analysis.entity.ResumeAnalysis;
import com.resumeanalyzer.analysis.repository.ResumeAnalysisRepository;
import com.resumeanalyzer.auth.security.UserPrincipal;
import com.resumeanalyzer.job.entity.Job;
import com.resumeanalyzer.job.repository.JobRepository;
import com.resumeanalyzer.resume.entity.Resume;
import com.resumeanalyzer.resume.repository.ResumeRepository;
import com.resumeanalyzer.user.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisService {

    private final ResumeAnalysisRepository analysisRepository;
    private final ResumeRepository resumeRepository;
    private final JobRepository jobRepository;
    private final UserAccountRepository userRepository;
    private final LlmAnalysisService llmAnalysisService;
    private final AnalysisResultValidator resultValidator;

    public AnalysisService(
            ResumeAnalysisRepository analysisRepository,
            ResumeRepository resumeRepository,
            JobRepository jobRepository,
            UserAccountRepository userRepository,
            LlmAnalysisService llmAnalysisService,
            AnalysisResultValidator resultValidator) {
        this.analysisRepository = analysisRepository;
        this.resumeRepository = resumeRepository;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.llmAnalysisService = llmAnalysisService;
        this.resultValidator = resultValidator;
    }

    public AnalysisResponse create(UserPrincipal principal, CreateAnalysisRequest request) {
        Resume resume = resumeRepository.findByIdAndUser_Id(request.resumeId(), principal.userId())
                .orElseThrow(AnalysisService::resourceNotFound);
        Job job = jobRepository.findByIdAndUser_Id(request.jobId(), principal.userId())
                .orElseThrow(AnalysisService::resourceNotFound);
        ResumeAnalysisResult result = resultValidator.validate(
                llmAnalysisService.analyze(resume.getExtractedText(), job.getJobDescription()));
        ResumeAnalysis analysis = analysisRepository.save(new ResumeAnalysis(
                userRepository.getReferenceById(principal.userId()),
                resume,
                job,
                result));
        return toResponse(analysis);
    }

    @Transactional(readOnly = true)
    public List<AnalysisResponse> list(UserPrincipal principal) {
        return analysisRepository.findAllByUser_IdOrderByCreatedAtDesc(principal.userId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AnalysisResponse get(UserPrincipal principal, Long analysisId) {
        ResumeAnalysis analysis = analysisRepository.findByIdAndUser_Id(analysisId, principal.userId())
                .orElseThrow(AnalysisService::resourceNotFound);
        return toResponse(analysis);
    }

    private AnalysisResponse toResponse(ResumeAnalysis analysis) {
        return new AnalysisResponse(
                analysis.getId(),
                analysis.getResume().getId(),
                analysis.getResume().getTitle(),
                analysis.getJob().getId(),
                analysis.getJob().getJobTitle(),
                analysis.getResult(),
                analysis.getCreatedAt());
    }

    private static AnalysisException resourceNotFound() {
        return new AnalysisException(
                AnalysisException.Reason.RESOURCE_NOT_FOUND,
                "Resume, job, or analysis not found");
    }
}
