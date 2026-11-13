package com.example.banking.service;

import com.example.banking.entity.AuditLog;
import com.example.banking.exception.ApiException;
import com.example.banking.repository.AuditRepository;
import com.example.banking.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FailedTransactionAuditService {
    private final UserRepository users;
    private final AuditRepository audits;

    public FailedTransactionAuditService(UserRepository users, AuditRepository audits) {
        this.users = users;
        this.audits = audits;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String email, String operation, RuntimeException failure, String attemptDetails) {
        Long actorId = users.findByEmail(email).map(user -> user.getId()).orElse(null);
        String action = failure instanceof ApiException api
                ? operation + "_REJECTED_" + api.getStatus().name()
                : operation + "_FAILED_INTERNAL";
        audits.save(new AuditLog(actorId, action, attemptDetails));
    }
}
