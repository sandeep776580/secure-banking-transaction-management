package com.example.banking.repository;
import com.example.banking.entity.AuditLog; import org.springframework.data.jpa.repository.JpaRepository;
public interface AuditRepository extends JpaRepository<AuditLog,Long>{}
