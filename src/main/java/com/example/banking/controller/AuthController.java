package com.example.banking.controller;

import com.example.banking.dto.ApiDtos.*;
import com.example.banking.entity.RevokedToken;
import com.example.banking.repository.RevokedTokenRepository;
import com.example.banking.security.JwtService;
import com.example.banking.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Registration, login, and opaque refresh-token rotation")
public class AuthController {
    private final AuthService service;
    private final JwtService jwt;
    private final RevokedTokenRepository revoked;

    public AuthController(AuthService service, JwtService jwt, RevokedTokenRepository revoked) {
        this.service = service;
        this.jwt = jwt;
        this.revoked = revoked;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) { return service.register(request); }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) { return service.login(request); }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate a single-use refresh token and issue a new token pair")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) { return service.refresh(request); }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Revoke the current access token and refresh token")
    @Transactional
    public void logout(@RequestHeader("Authorization") String authorization, @Valid @RequestBody LogoutRequest request,
                       Authentication authentication) {
        String token = authorization.substring("Bearer ".length());
        if (!jwt.subject(token).equals(authentication.getName())) throw new org.springframework.security.access.AccessDeniedException("Token subject mismatch");
        String tokenId = jwt.tokenId(token);
        if (!revoked.existsById(tokenId)) revoked.save(new RevokedToken(tokenId, jwt.expiresAt(token)));
        service.revokeRefresh(request.refreshToken(), authentication.getName());
    }
}
