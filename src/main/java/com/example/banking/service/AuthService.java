package com.example.banking.service;

import com.example.banking.dto.ApiDtos.*;
import com.example.banking.entity.*;
import com.example.banking.exception.ApiException;
import com.example.banking.repository.RefreshTokenRepository;
import com.example.banking.repository.UserRepository;
import com.example.banking.security.JwtService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final long refreshTtl;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens, PasswordEncoder encoder,
                       JwtService jwt, @Value("${app.jwt.refresh-ttl-seconds:2592000}") long refreshTtl) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
        this.jwt = jwt;
        if (refreshTtl < 60) throw new IllegalArgumentException("Refresh-token lifetime must be at least 60 seconds");
        this.refreshTtl = refreshTtl;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        validateBcryptPasswordLength(request.password());
        String email = normalize(request.email());
        if (users.existsByEmail(email)) throw new ApiException(HttpStatus.CONFLICT, "Email is already registered");
        AppUser user = users.save(new AppUser(email, encoder.encode(request.password()), request.fullName().trim(), Role.CUSTOMER));
        return issue(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        validateBcryptPasswordLength(request.password());
        AppUser user = users.findByEmail(normalize(request.email()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!encoder.matches(request.password(), user.getPasswordHash()))
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        return issue(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String hash = hash(request.refreshToken());
        RefreshToken token = refreshTokens.lockByTokenHash(hash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        Instant now = Instant.now();
        if (!token.isUsableAt(now)) throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token is expired or revoked");
        token.revoke(now);
        return issue(token.getUser());
    }

    @Transactional
    public void revokeRefresh(String rawToken, String email) {
        refreshTokens.findByTokenHash(hash(rawToken)).ifPresent(token -> {
            if (!token.getUser().getEmail().equals(email)) throw ApiException.forbidden("Refresh token does not belong to this user");
            token.revoke(Instant.now());
        });
    }

    private AuthResponse issue(AppUser user) {
        byte[] random = new byte[32];
        secureRandom.nextBytes(random);
        String rawRefreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        Instant expiresAt = Instant.now().plusSeconds(refreshTtl);
        refreshTokens.save(new RefreshToken(user, hash(rawRefreshToken), expiresAt));
        return new AuthResponse(jwt.create(user), rawRefreshToken, "Bearer", jwt.ttl(), refreshTtl,
                new UserView(user.getId(), user.getEmail(), user.getFullName(), user.getRoles()));
    }

    private static String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }

    private static void validateBcryptPasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw ApiException.badRequest("Password must not exceed 72 UTF-8 bytes");
        }
    }

    private static String hash(String token) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
