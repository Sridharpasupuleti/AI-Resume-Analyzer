package com.resumeanalyzer.analysis.controller;

import java.net.URI;
import java.util.List;

import com.resumeanalyzer.analysis.dto.AnalysisResponse;
import com.resumeanalyzer.analysis.dto.CreateAnalysisRequest;
import com.resumeanalyzer.analysis.service.AnalysisService;
import com.resumeanalyzer.auth.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/analyses")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping
    public ResponseEntity<AnalysisResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateAnalysisRequest request) {
        AnalysisResponse analysis = analysisService.create(principal, request);
        return ResponseEntity.created(URI.create("/api/v1/analyses/" + analysis.id())).body(analysis);
    }

    @GetMapping
    public List<AnalysisResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
        return analysisService.list(principal);
    }

    @GetMapping("/{analysisId}")
    public AnalysisResponse get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @Positive Long analysisId) {
        return analysisService.get(principal, analysisId);
    }
}
