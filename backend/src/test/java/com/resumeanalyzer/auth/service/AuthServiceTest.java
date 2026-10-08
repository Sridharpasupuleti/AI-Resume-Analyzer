package com.resumeanalyzer.auth.service;

import java.util.Optional;

import com.resumeanalyzer.auth.dto.LoginRequest;
import com.resumeanalyzer.auth.dto.RegisterRequest;
import com.resumeanalyzer.user.entity.UserAccount;
import com.resumeanalyzer.user.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserAccountRepository userRepository;

    @Mock
    private JwtService jwtService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void registerNormalizesEmailAndStoresOnlyEncodedPassword() {
        RegisterRequest request = new RegisterRequest("  Casey Example  ", " CASEY@EXAMPLE.COM ", "strong-pass-1");
        when(userRepository.existsByEmail("casey@example.com")).thenReturn(false);
        when(userRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.createToken(any(UserAccount.class))).thenReturn("signed-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        var response = authService.register(request);

        var savedUser = org.mockito.ArgumentCaptor.forClass(UserAccount.class);
        verify(userRepository).save(savedUser.capture());
        assertEquals("Casey Example", savedUser.getValue().getFullName());
        assertEquals("casey@example.com", savedUser.getValue().getEmail());
        assertNotEquals("strong-pass-1", savedUser.getValue().getPasswordHash());
        org.junit.jupiter.api.Assertions.assertTrue(
                passwordEncoder.matches("strong-pass-1", savedUser.getValue().getPasswordHash()));
        assertEquals("signed-token", response.accessToken());
        assertEquals("casey@example.com", response.user().email());
    }

    @Test
    void registerRejectsExistingEmail() {
        when(userRepository.existsByEmail("casey@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyRegisteredException.class, () ->
                authService.register(new RegisterRequest("Casey", "casey@example.com", "strong-pass-1")));
        verify(userRepository, never()).save(any(UserAccount.class));
    }

    @Test
    void loginUsesSameGenericFailureForUnknownEmailAndWrongPassword() {
        when(userRepository.findByEmail("casey@example.com")).thenReturn(Optional.empty());
        assertThrows(InvalidCredentialsException.class, () ->
                authService.login(new LoginRequest("CASEY@example.com", "wrong-password")));

        when(userRepository.findByEmail("casey@example.com")).thenReturn(
                Optional.of(new UserAccount("Casey", "casey@example.com", passwordEncoder.encode("correct-pass"))));
        assertThrows(InvalidCredentialsException.class, () ->
                authService.login(new LoginRequest("casey@example.com", "wrong-password")));
    }

    @Test
    void loginReturnsTokenForMatchingPassword() {
        UserAccount user = new UserAccount("Casey", "casey@example.com", passwordEncoder.encode("correct-pass"));
        when(userRepository.findByEmail("casey@example.com")).thenReturn(Optional.of(user));
        when(jwtService.createToken(user)).thenReturn("signed-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        var response = authService.login(new LoginRequest("CASEY@example.com", "correct-pass"));

        assertEquals("signed-token", response.accessToken());
        assertEquals("casey@example.com", response.user().email());
    }

    @Test
    void registerRejectsPasswordsLongerThanBcryptByteLimit() {
        String password = "\uD83D\uDE00".repeat(19);
        assertThrows(PasswordTooLongException.class, () ->
                authService.register(new RegisterRequest("Casey", "casey@example.com", password)));
        verify(userRepository, never()).existsByEmail(any());
    }
}
