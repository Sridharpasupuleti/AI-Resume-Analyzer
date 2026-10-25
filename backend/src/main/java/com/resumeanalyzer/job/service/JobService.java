package com.resumeanalyzer.job.service;

import java.util.List;

import com.resumeanalyzer.auth.security.UserPrincipal;
import com.resumeanalyzer.job.dto.CreateJobRequest;
import com.resumeanalyzer.job.dto.JobResponse;
import com.resumeanalyzer.job.dto.UpdateJobRequest;
import com.resumeanalyzer.job.entity.Job;
import com.resumeanalyzer.job.repository.JobRepository;
import com.resumeanalyzer.user.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final UserAccountRepository userRepository;

    public JobService(JobRepository jobRepository, UserAccountRepository userRepository) {
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public JobResponse create(UserPrincipal principal, CreateJobRequest request) {
        Job job = new Job(
                userRepository.getReferenceById(principal.userId()),
                request.companyName(),
                request.jobTitle(),
                request.jobDescription(),
                request.jobUrl(),
                request.location());
        return toResponse(jobRepository.save(job));
    }

    @Transactional(readOnly = true)
    public List<JobResponse> list(UserPrincipal principal) {
        return jobRepository.findAllByUser_IdOrderByCreatedAtDesc(principal.userId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public JobResponse get(UserPrincipal principal, Long jobId) {
        return toResponse(findOwnedJob(principal, jobId));
    }

    @Transactional
    public JobResponse update(UserPrincipal principal, Long jobId, UpdateJobRequest request) {
        Job job = findOwnedJob(principal, jobId);
        job.updateDetails(
                request.companyName(),
                request.jobTitle(),
                request.jobDescription(),
                request.jobUrl(),
                request.location());
        return toResponse(jobRepository.save(job));
    }

    @Transactional
    public void delete(UserPrincipal principal, Long jobId) {
        Job job = findOwnedJob(principal, jobId);
        jobRepository.delete(job);
    }

    private Job findOwnedJob(UserPrincipal principal, Long jobId) {
        return jobRepository.findByIdAndUser_Id(jobId, principal.userId())
                .orElseThrow(JobNotFoundException::new);
    }

    private JobResponse toResponse(Job job) {
        return new JobResponse(
                job.getId(),
                job.getCompanyName(),
                job.getJobTitle(),
                job.getJobDescription(),
                job.getJobUrl(),
                job.getLocation(),
                job.getSource(),
                job.getCreatedAt(),
                job.getUpdatedAt());
    }
}
