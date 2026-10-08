package com.resumeanalyzer.auth.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import com.resumeanalyzer.auth.security.UserPrincipal;
import com.resumeanalyzer.user.entity.UserAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMillis;

    public JwtService(String secret, long expirationMillis) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must contain at least 32 UTF-8 bytes");
        }
        if (expirationMillis <= 0) {
            throw new IllegalArgumentException("JWT expiration must be positive");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMillis;
    }

    public String createToken(UserAccount user) {
        return createToken(new UserPrincipal(user.getId(), user.getEmail()));
    }

    public String createToken(UserPrincipal principal) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(expirationMillis);
        return Jwts.builder()
                .subject(principal.email())
                .claim("userId", principal.userId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public UserPrincipal parsePrincipal(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Object userIdClaim = claims.get("userId");
        if (!(userIdClaim instanceof Number userId) || claims.getSubject() == null || claims.getSubject().isBlank()) {
            throw new IllegalArgumentException("JWT is missing required user claims");
        }
        return new UserPrincipal(userId.longValue(), claims.getSubject());
    }

    public long getExpirationSeconds() {
        return expirationMillis / 1000;
    }
}
