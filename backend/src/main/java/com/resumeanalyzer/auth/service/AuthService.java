package com.resumeanalyzer.auth.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.resumeanalyzer.auth.dto.AuthResponse;
import com.resumeanalyzer.auth.dto.LoginRequest;
import com.resumeanalyzer.auth.dto.RegisterRequest;
import com.resumeanalyzer.auth.security.UserPrincipal;
import com.resumeanalyzer.user.dto.UserResponse;
import com.resumeanalyzer.user.entity.UserAccount;
import com.resumeanalyzer.user.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserAccountRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new PasswordTooLongException();
        }
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        UserAccount user = userRepository.save(new UserAccount(
                request.fullName().trim(),
                email,
                passwordEncoder.encode(request.password())));
        return authResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidCredentialsException();
        }
        String email = normalizeEmail(request.email());
        UserAccount user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return authResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UserPrincipal principal) {
        UserAccount user = userRepository.findById(principal.userId())
                .orElseThrow(InvalidCredentialsException::new);
        return toUserResponse(user);
    }

    private AuthResponse authResponse(UserAccount user) {
        return new AuthResponse(
                jwtService.createToken(user),
                "Bearer",
                jwtService.getExpirationSeconds(),
                toUserResponse(user));
    }

    private UserResponse toUserResponse(UserAccount user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
