package com.example.banking.service;

import com.example.banking.repository.RefreshTokenRepository;
import com.example.banking.repository.RevokedTokenRepository;
import java.time.Instant;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableScheduling
public class ExpiredTokenCleanupJob {
    private final RevokedTokenRepository revokedTokens;
    private final RefreshTokenRepository refreshTokens;

    public ExpiredTokenCleanupJob(RevokedTokenRepository revokedTokens, RefreshTokenRepository refreshTokens) {
        this.revokedTokens = revokedTokens;
        this.refreshTokens = refreshTokens;
    }

    @Scheduled(cron = "0 17 * * * *")
    public void removeExpiredTokens() {
        Instant now = Instant.now();
        revokedTokens.deleteByExpiresAtBefore(now);
        refreshTokens.deleteByExpiresAtBefore(now);
    }
}
