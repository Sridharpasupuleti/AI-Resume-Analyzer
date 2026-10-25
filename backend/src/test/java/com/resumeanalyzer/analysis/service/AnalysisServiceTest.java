package com.resumeanalyzer.analysis.service;

import java.util.List;
import java.util.Optional;

import com.resumeanalyzer.analysis.dto.CreateAnalysisRequest;
import com.resumeanalyzer.analysis.dto.ResumeAnalysisResult;
import com.resumeanalyzer.analysis.entity.ResumeAnalysis;
import com.resumeanalyzer.analysis.repository.ResumeAnalysisRepository;
import com.resumeanalyzer.auth.security.UserPrincipal;
import com.resumeanalyzer.job.entity.Job;
import com.resumeanalyzer.job.repository.JobRepository;
import com.resumeanalyzer.resume.entity.Resume;
import com.resumeanalyzer.resume.repository.ResumeRepository;
import com.resumeanalyzer.user.entity.UserAccount;
import com.resumeanalyzer.user.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock
    private ResumeAnalysisRepository analysisRepository;

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private UserAccountRepository userRepository;

    @Mock
    private LlmAnalysisService llmAnalysisService;

    private AnalysisService analysisService;
    private final AnalysisResultValidator resultValidator = new AnalysisResultValidator();
    private final UserPrincipal principal = new UserPrincipal(7L, "casey@example.com");
    private final ResumeAnalysisResult result = new ResumeAnalysisResult(
            75,
            "The resume includes 3 of 4 identified job skills.",
            List.of("Java", "Spring Boot", "REST"),
            List.of("Docker"),
            List.of("java"),
            List.of("docker"),
            List.of("Highlight related experience."));
    private final Resume resume = new Resume(
            new UserAccount("Casey", "casey@example.com", "hash"),
            "Casey Resume",
            "casey.pdf",
            "PDF",
            "storage-key.pdf",
            "Resume text with Java.");
    private final Job job = new Job(
            new UserAccount("Casey", "casey@example.com", "hash"),
            "Example Inc.",
            "Java Engineer",
            "Job description requires Java.",
            null,
            null);

    @BeforeEach
    void setUp() {
        analysisService = new AnalysisService(
                analysisRepository,
                resumeRepository,
                jobRepository,
                userRepository,
                llmAnalysisService,
                resultValidator);
    }

    @Test
    void createsAndPersistsAnalysisUsingOnlyTheUsersResumeAndJob() {
        when(resumeRepository.findByIdAndUser_Id(10L, 7L)).thenReturn(Optional.of(resume));
        when(jobRepository.findByIdAndUser_Id(25L, 7L)).thenReturn(Optional.of(job));
        when(llmAnalysisService.analyze("Resume text with Java.", "Job description requires Java."))
                .thenReturn(result);
        when(userRepository.getReferenceById(7L))
                .thenReturn(new UserAccount("Casey", "casey@example.com", "hash"));
        when(analysisRepository.save(any(ResumeAnalysis.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = analysisService.create(principal, new CreateAnalysisRequest(10L, 25L));

        var savedAnalysis = org.mockito.ArgumentCaptor.forClass(ResumeAnalysis.class);
        verify(analysisRepository).save(savedAnalysis.capture());
        assertEquals(resume, savedAnalysis.getValue().getResume());
        assertEquals(job, savedAnalysis.getValue().getJob());
        assertEquals(result, savedAnalysis.getValue().getResult());
        assertEquals(75, savedAnalysis.getValue().getOverallScore());
        assertEquals(result, response.result());
        verify(userRepository).getReferenceById(7L);
    }

    @Test
    void doesNotCallAnalyzerOrPersistWhenResumeIsNotOwnedByUser() {
        when(resumeRepository.findByIdAndUser_Id(10L, 7L)).thenReturn(Optional.empty());

        AnalysisException exception = assertThrows(
                AnalysisException.class,
                () -> analysisService.create(principal, new CreateAnalysisRequest(10L, 25L)));

        assertEquals(AnalysisException.Reason.RESOURCE_NOT_FOUND, exception.getReason());
        verify(llmAnalysisService, never()).analyze(any(), any());
        verify(analysisRepository, never()).save(any());
    }

    @Test
    void doesNotCallAnalyzerOrPersistWhenJobIsNotOwnedByUser() {
        when(resumeRepository.findByIdAndUser_Id(10L, 7L)).thenReturn(Optional.of(resume));
        when(jobRepository.findByIdAndUser_Id(25L, 7L)).thenReturn(Optional.empty());

        assertThrows(
                AnalysisException.class,
                () -> analysisService.create(principal, new CreateAnalysisRequest(10L, 25L)));

        verify(llmAnalysisService, never()).analyze(any(), any());
        verify(analysisRepository, never()).save(any());
    }

    @Test
    void rejectsInvalidAnalyzerOutputWithoutPersistingIt() {
        when(resumeRepository.findByIdAndUser_Id(10L, 7L)).thenReturn(Optional.of(resume));
        when(jobRepository.findByIdAndUser_Id(25L, 7L)).thenReturn(Optional.of(job));
        when(llmAnalysisService.analyze("Resume text with Java.", "Job description requires Java."))
                .thenReturn(new ResumeAnalysisResult(
                        120, "Invalid score", List.of(), List.of(), List.of(), List.of(), List.of()));

        assertThrows(
                AnalysisException.class,
                () -> analysisService.create(principal, new CreateAnalysisRequest(10L, 25L)));

        verify(analysisRepository, never()).save(any());
    }
}
