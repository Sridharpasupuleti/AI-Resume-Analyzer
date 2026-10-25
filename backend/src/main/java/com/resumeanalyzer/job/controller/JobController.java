package com.resumeanalyzer.job.controller;

import java.net.URI;
import java.util.List;

import com.resumeanalyzer.auth.security.UserPrincipal;
import com.resumeanalyzer.job.dto.CreateJobRequest;
import com.resumeanalyzer.job.dto.JobResponse;
import com.resumeanalyzer.job.dto.UpdateJobRequest;
import com.resumeanalyzer.job.service.JobService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<JobResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateJobRequest request) {
        JobResponse job = jobService.create(principal, request);
        return ResponseEntity.created(URI.create("/api/v1/jobs/" + job.id())).body(job);
    }

    @GetMapping
    public List<JobResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return jobService.list(principal);
    }

    @GetMapping("/{jobId}")
    public JobResponse get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Positive Long jobId) {
        return jobService.get(principal, jobId);
    }

    @PutMapping("/{jobId}")
    public JobResponse update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Positive Long jobId,
            @Valid @RequestBody UpdateJobRequest request) {
        return jobService.update(principal, jobId, request);
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Positive Long jobId) {
        jobService.delete(principal, jobId);
        return ResponseEntity.noContent().build();
    }
}
