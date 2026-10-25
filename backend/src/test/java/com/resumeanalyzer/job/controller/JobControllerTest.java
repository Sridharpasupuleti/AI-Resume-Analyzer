package com.resumeanalyzer.job.controller;

import java.time.Instant;
import java.util.List;

import com.resumeanalyzer.auth.security.JwtAuthenticationFilter;
import com.resumeanalyzer.auth.security.UserPrincipal;
import com.resumeanalyzer.auth.service.JwtService;
import com.resumeanalyzer.config.SecurityConfig;
import com.resumeanalyzer.job.dto.CreateJobRequest;
import com.resumeanalyzer.job.dto.JobResponse;
import com.resumeanalyzer.job.dto.UpdateJobRequest;
import com.resumeanalyzer.job.service.JobNotFoundException;
import com.resumeanalyzer.job.service.JobService;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@TestPropertySource(properties = "app.jwt.secret=test-signing-key-that-is-at-least-32-bytes-long")
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private JobService jobService;

    @Test
    void jobEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    void createReturnsCreatedJobAndLocation() throws Exception {
        UserPrincipal principal = new UserPrincipal(7L, "casey@example.com");
        String token = jwtService.createToken(principal);
        when(jobService.create(eq(principal), any(CreateJobRequest.class)))
                .thenReturn(job(12L));

        mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyName": "Example Inc.",
                                  "jobTitle": "Java Engineer",
                                  "jobDescription": "Build Spring Boot services.",
                                  "jobUrl": "https://example.com/jobs/1",
                                  "location": "Remote"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/jobs/12"))
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.source").value("MANUAL"));
    }

    @Test
    void createRejectsMissingRequiredFields() throws Exception {
        String token = jwtService.createToken(new UserPrincipal(7L, "casey@example.com"));

        mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Example Inc.\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.jobTitle").exists())
                .andExpect(jsonPath("$.validationErrors.jobDescription").exists());
    }

    @Test
    void listReturnsCurrentUsersJobs() throws Exception {
        UserPrincipal principal = new UserPrincipal(7L, "casey@example.com");
        String token = jwtService.createToken(principal);
        when(jobService.list(principal)).thenReturn(List.of(job(12L)));

        mockMvc.perform(get("/api/v1/jobs").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(12))
                .andExpect(jsonPath("$[0].jobTitle").value("Java Engineer"));
    }

    @Test
    void getMapsUnownedJobsToNotFound() throws Exception {
        UserPrincipal principal = new UserPrincipal(7L, "casey@example.com");
        String token = jwtService.createToken(principal);
        when(jobService.get(principal, 21L)).thenThrow(new JobNotFoundException());

        mockMvc.perform(get("/api/v1/jobs/21").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Job not found"));
    }

    @Test
    void updateReturnsUpdatedJob() throws Exception {
        UserPrincipal principal = new UserPrincipal(7L, "casey@example.com");
        String token = jwtService.createToken(principal);
        when(jobService.update(eq(principal), eq(12L), any(UpdateJobRequest.class)))
                .thenReturn(job(12L));

        mockMvc.perform(put("/api/v1/jobs/12")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "jobTitle": "Java Engineer",
                                  "jobDescription": "Build Spring Boot services."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        UserPrincipal principal = new UserPrincipal(7L, "casey@example.com");
        String token = jwtService.createToken(principal);
        doNothing().when(jobService).delete(principal, 12L);

        mockMvc.perform(delete("/api/v1/jobs/12").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        verify(jobService).delete(principal, 12L);
    }

    private JobResponse job(Long id) {
        return new JobResponse(
                id,
                "Example Inc.",
                "Java Engineer",
                "Build Spring Boot services.",
                "https://example.com/jobs/1",
                "Remote",
                "MANUAL",
                Instant.EPOCH,
                Instant.EPOCH);
    }
}
