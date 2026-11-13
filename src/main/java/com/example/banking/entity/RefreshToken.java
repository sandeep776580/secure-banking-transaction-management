package com.example.banking.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "refresh_tokens", uniqueConstraints = @UniqueConstraint(name = "uk_refresh_token_hash", columnNames = "token_hash"), indexes = @Index(name = "idx_refresh_expires", columnList = "expires_at"))
public class RefreshToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private AppUser user;
    @Column(name = "token_hash", nullable = false, length = 64) private String tokenHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "revoked_at") private Instant revokedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    protected RefreshToken() {}
    public RefreshToken(AppUser user, String tokenHash, Instant expiresAt) { this.user = user; this.tokenHash = tokenHash; this.expiresAt = expiresAt; }
    public AppUser getUser() { return user; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isUsableAt(Instant now) { return revokedAt == null && expiresAt.isAfter(now); }
    public void revoke(Instant now) { revokedAt = now; }
}
