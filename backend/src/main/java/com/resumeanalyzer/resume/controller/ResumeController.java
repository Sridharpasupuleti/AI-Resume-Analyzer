package com.resumeanalyzer.resume.controller;

import java.net.URI;
import java.util.List;

import com.resumeanalyzer.auth.security.UserPrincipal;
import com.resumeanalyzer.resume.dto.ResumeDetails;
import com.resumeanalyzer.resume.dto.ResumeFileDownload;
import com.resumeanalyzer.resume.dto.ResumeSummary;
import com.resumeanalyzer.resume.service.ResumeService;
import jakarta.validation.constraints.Positive;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/v1/resumes")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeDetails> upload(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file) {
        ResumeDetails resume = resumeService.upload(principal, file);
        return ResponseEntity.created(URI.create("/api/v1/resumes/" + resume.id())).body(resume);
    }

    @GetMapping
    public List<ResumeSummary> list(@AuthenticationPrincipal UserPrincipal principal) {
        return resumeService.list(principal);
    }

    @GetMapping("/{resumeId}")
    public ResumeDetails get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Positive Long resumeId) {
        return resumeService.get(principal, resumeId);
    }

    @GetMapping("/{resumeId}/file")
    public ResponseEntity<Resource> download(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Positive Long resumeId) {
        ResumeFileDownload file = resumeService.download(principal, resumeId);
        MediaType mediaType = file.fileType().equals("PDF")
                ? MediaType.APPLICATION_PDF
                : MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.originalFileName()).build().toString())
                .body(file.resource());
    }

    @DeleteMapping("/{resumeId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Positive Long resumeId) {
        resumeService.delete(principal, resumeId);
        return ResponseEntity.noContent().build();
    }
}
