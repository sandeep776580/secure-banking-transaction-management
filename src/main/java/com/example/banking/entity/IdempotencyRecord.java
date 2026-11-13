package com.example.banking.entity;
import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="idempotency_records",uniqueConstraints=@UniqueConstraint(name="uk_idempotency_owner_key",columnNames={"owner_id","idempotency_key"}))
public class IdempotencyRecord {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="owner_id",nullable=false) private AppUser owner;
 @Column(name="idempotency_key",nullable=false,length=100) private String key;
 @Column(name="reference_id",nullable=false,length=36) private String referenceId;
 @Column(name="request_hash",nullable=false,length=64) private String requestHash;
 @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now();
 protected IdempotencyRecord(){} public IdempotencyRecord(AppUser owner,String key,String referenceId,String requestHash){this.owner=owner;this.key=key;this.referenceId=referenceId;this.requestHash=requestHash;} public String getReferenceId(){return referenceId;} public String getRequestHash(){return requestHash;}
}
