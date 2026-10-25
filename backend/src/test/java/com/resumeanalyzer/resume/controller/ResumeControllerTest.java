package com.resumeanalyzer.resume.controller;

import java.time.Instant;

import com.resumeanalyzer.auth.security.JwtAuthenticationFilter;
import com.resumeanalyzer.auth.security.UserPrincipal;
import com.resumeanalyzer.auth.service.JwtService;
import com.resumeanalyzer.config.SecurityConfig;
import com.resumeanalyzer.resume.dto.ResumeDetails;
import com.resumeanalyzer.resume.service.ResumeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResumeController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@TestPropertySource(properties = "app.jwt.secret=test-signing-key-that-is-at-least-32-bytes-long")
class ResumeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private ResumeService resumeService;

    @Test
    void resumeEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/resumes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    void uploadReturnsCreatedResumeDetails() throws Exception {
        UserPrincipal principal = new UserPrincipal(7L, "casey@example.com");
        String token = jwtService.createToken(principal);
        when(resumeService.upload(eq(principal), any())).thenReturn(
                new ResumeDetails(12L, "Casey Resume", "Casey Resume.pdf", "PDF", "Casey Example", Instant.EPOCH));
        MockMultipartFile file = new MockMultipartFile(
                "file", "Casey Resume.pdf", MediaType.APPLICATION_PDF_VALUE, "%PDF-1.7".getBytes());

        mockMvc.perform(multipart("/api/v1/resumes")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/resumes/12"))
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.extractedText").value("Casey Example"));
    }
}
