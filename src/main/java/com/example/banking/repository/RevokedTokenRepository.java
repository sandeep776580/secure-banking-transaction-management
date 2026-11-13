package com.example.banking.repository;
import com.example.banking.entity.RevokedToken;import org.springframework.data.jpa.repository.JpaRepository;
public interface RevokedTokenRepository extends JpaRepository<RevokedToken,String>{long deleteByExpiresAtBefore(java.time.Instant now);}
