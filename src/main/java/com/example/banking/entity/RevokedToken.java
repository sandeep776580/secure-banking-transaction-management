package com.example.banking.entity;
import jakarta.persistence.*;import java.time.Instant;
@Entity @Table(name="revoked_tokens") public class RevokedToken {@Id @Column(name="token_id",length=36) private String tokenId;@Column(name="expires_at",nullable=false) private Instant expiresAt;protected RevokedToken(){}public RevokedToken(String id,Instant exp){tokenId=id;expiresAt=exp;}}
