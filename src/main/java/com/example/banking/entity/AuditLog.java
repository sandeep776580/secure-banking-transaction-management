package com.example.banking.entity;
import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="audit_logs",indexes=@Index(name="idx_audit_created",columnList="created_at"))
public class AuditLog {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="actor_id") private Long actorId; @Column(nullable=false,length=80) private String action; @Column(length=500) private String details; @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now();
 protected AuditLog(){} public AuditLog(Long actorId,String action,String details){this.actorId=actorId;this.action=action;this.details=details;}
}
