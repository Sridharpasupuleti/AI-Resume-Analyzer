package com.resumeanalyzer.auth.controller;

import com.resumeanalyzer.auth.dto.AuthResponse;
import com.resumeanalyzer.auth.security.JwtAuthenticationFilter;
import com.resumeanalyzer.auth.security.UserPrincipal;
import com.resumeanalyzer.auth.service.AuthService;
import com.resumeanalyzer.auth.service.JwtService;
import com.resumeanalyzer.config.SecurityConfig;
import com.resumeanalyzer.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@TestPropertySource(properties = "app.jwt.secret=test-signing-key-that-is-at-least-32-bytes-long")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AuthService authService;

    @Test
    void registrationReturnsCreatedAndTokenResponse() throws Exception {
        when(authService.register(any())).thenReturn(
                new AuthResponse("test-token", "Bearer", 3600, new UserResponse(1L, "Casey", "casey@example.com")));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Casey","email":"casey@example.com","password":"strong-pass-1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("test-token"))
                .andExpect(jsonPath("$.user.email").value("casey@example.com"))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    void registrationRejectsInvalidInput() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.fullName").exists())
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    @Test
    void currentUserRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    void currentUserResolvesTheAuthenticatedPrincipalFromJwt() throws Exception {
        UserPrincipal principal = new UserPrincipal(42L, "casey@example.com");
        String token = jwtService.createToken(principal);
        when(authService.getCurrentUser(principal))
                .thenReturn(new UserResponse(42L, "Casey", "casey@example.com"));

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.email").value("casey@example.com"));
    }

    @Test
    void invalidBearerTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired access token"));
    }
}
