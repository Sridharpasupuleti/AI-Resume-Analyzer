package com.resumeanalyzer.analysis.controller;

import java.time.Instant;
import java.util.List;

import com.resumeanalyzer.analysis.dto.AnalysisResponse;
import com.resumeanalyzer.analysis.dto.CreateAnalysisRequest;
import com.resumeanalyzer.analysis.dto.ResumeAnalysisResult;
import com.resumeanalyzer.analysis.service.AnalysisException;
import com.resumeanalyzer.analysis.service.AnalysisService;
import com.resumeanalyzer.auth.security.JwtAuthenticationFilter;
import com.resumeanalyzer.auth.security.UserPrincipal;
import com.resumeanalyzer.auth.service.JwtService;
import com.resumeanalyzer.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalysisController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@TestPropertySource(properties = "app.jwt.secret=test-signing-key-that-is-at-least-32-bytes-long")
class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AnalysisService analysisService;

    @Test
    void analysisEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/analyses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    void createReturnsPersistedStructuredResult() throws Exception {
        UserPrincipal principal = new UserPrincipal(7L, "casey@example.com");
        String token = jwtService.createToken(principal);
        when(analysisService.create(eq(principal), any(CreateAnalysisRequest.class)))
                .thenReturn(analysis());

        mockMvc.perform(post("/api/v1/analyses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resumeId\":10,\"jobId\":25}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/analyses/31"))
                .andExpect(jsonPath("$.result.overallScore").value(75))
                .andExpect(jsonPath("$.result.missingSkills[0]").value("Docker"));
    }

    @Test
    void createValidatesRequiredPositiveSourceIds() throws Exception {
        String token = jwtService.createToken(new UserPrincipal(7L, "casey@example.com"));

        mockMvc.perform(post("/api/v1/analyses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resumeId\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.resumeId").exists())
                .andExpect(jsonPath("$.validationErrors.jobId").exists());
    }

    @Test
    void getHidesAnalysesOwnedByAnotherUser() throws Exception {
        UserPrincipal principal = new UserPrincipal(7L, "casey@example.com");
        String token = jwtService.createToken(principal);
        when(analysisService.get(principal, 31L)).thenThrow(new AnalysisException(
                AnalysisException.Reason.RESOURCE_NOT_FOUND,
                "Resume, job, or analysis not found"));

        mockMvc.perform(get("/api/v1/analyses/31").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Resume, job, or analysis not found"));
    }

    @Test
    void returnsServiceUnavailableWhenOpenAiIsNotConfigured() throws Exception {
        UserPrincipal principal = new UserPrincipal(7L, "casey@example.com");
        String token = jwtService.createToken(principal);
        when(analysisService.create(eq(principal), any(CreateAnalysisRequest.class)))
                .thenThrow(new AnalysisException(
                        AnalysisException.Reason.PROVIDER_NOT_CONFIGURED,
                        "OpenAI is selected but LLM_API_KEY is not configured"));

        mockMvc.perform(post("/api/v1/analyses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resumeId\":10,\"jobId\":25}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(
                        "OpenAI is selected but LLM_API_KEY is not configured"));
    }

    @Test
    void listReturnsAnalysisHistory() throws Exception {
        UserPrincipal principal = new UserPrincipal(7L, "casey@example.com");
        String token = jwtService.createToken(principal);
        when(analysisService.list(principal)).thenReturn(List.of(analysis()));

        mockMvc.perform(get("/api/v1/analyses").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(31))
                .andExpect(jsonPath("$[0].result.overallScore").value(75));
    }

    private AnalysisResponse analysis() {
        return new AnalysisResponse(
                31L,
                10L,
                "Casey Resume",
                25L,
                "Java Engineer",
                new ResumeAnalysisResult(
                        75,
                        "The resume includes 3 of 4 identified job skills.",
                        List.of("Java", "Spring Boot", "REST"),
                        List.of("Docker"),
                        List.of("java"),
                        List.of("docker"),
                        List.of("Highlight relevant experience.")),
                Instant.EPOCH);
    }
}
