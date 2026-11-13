package com.example.banking.service;

import com.example.banking.dto.ApiDtos.MoneyRequest;
import com.example.banking.dto.ApiDtos.AccountStatement;
import com.example.banking.dto.ApiDtos.TransactionView;
import com.example.banking.dto.ApiDtos.TransferRequest;
import com.example.banking.entity.AccountStatus;
import com.example.banking.entity.AppUser;
import com.example.banking.entity.BankAccount;
import com.example.banking.entity.BankTransaction;
import com.example.banking.entity.IdempotencyRecord;
import com.example.banking.entity.TransactionType;
import com.example.banking.entity.AuditLog;
import com.example.banking.exception.ApiException;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.AuditRepository;
import com.example.banking.repository.IdempotencyRepository;
import com.example.banking.repository.TransactionRepository;
import com.example.banking.repository.UserRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {
    private final AccountRepository accounts;
    private final UserRepository users;
    private final TransactionRepository transactions;
    private final IdempotencyRepository idempotencyRecords;
    private final AuditRepository audits;
    private final FailedTransactionAuditService failureAudits;

    public TransactionService(AccountRepository accounts, UserRepository users, TransactionRepository transactions,
                              IdempotencyRepository idempotencyRecords, AuditRepository audits,
                              FailedTransactionAuditService failureAudits) {
        this.accounts = accounts;
        this.users = users;
        this.transactions = transactions;
        this.idempotencyRecords = idempotencyRecords;
        this.audits = audits;
        this.failureAudits = failureAudits;
    }

    @Transactional
    public TransactionView deposit(MoneyRequest request, String email) {
        try {
            BankAccount account = lockOwned(request.accountId(), email);
            requireActive(account);
            BigDecimal amount = amount(request.amount());
            account.credit(amount);
            TransactionView result = save(account, TransactionType.DEPOSIT, amount, request.description(), newReference());
            transactions.flush();
            return result;
        } catch (RuntimeException failure) {
            recordFailure(email, "DEPOSIT", failure, moneyAttemptDetails(request));
            throw failure;
        }
    }

    @Transactional
    public TransactionView withdraw(MoneyRequest request, String email) {
        try {
            BankAccount account = lockOwned(request.accountId(), email);
            requireActive(account);
            BigDecimal amount = amount(request.amount());
            if (account.getBalance().compareTo(amount) < 0) throw ApiException.badRequest("Insufficient funds");
            account.debit(amount);
            TransactionView result = save(account, TransactionType.WITHDRAWAL, amount, request.description(), newReference());
            transactions.flush();
            return result;
        } catch (RuntimeException failure) {
            recordFailure(email, "WITHDRAWAL", failure, moneyAttemptDetails(request));
            throw failure;
        }
    }

    @Transactional
    public TransactionView transfer(TransferRequest request, String email, String idempotencyKey) {
        try {
            return executeTransfer(request, email, idempotencyKey);
        } catch (RuntimeException failure) {
            recordFailure(email, "TRANSFER", failure, transferAttemptDetails(request));
            throw failure;
        }
    }

    private TransactionView executeTransfer(TransferRequest request, String email, String key) {
        validateIdempotencyKey(key);
        String requestHash = fingerprint(request);
        AppUser owner = users.findByEmail(email).orElseThrow(() -> ApiException.notFound("User not found"));

        var existing = idempotencyRecords.findByOwnerIdAndKey(owner.getId(), key);
        if (existing.isPresent()) {
            if (!existing.get().getRequestHash().equals(requestHash)) {
                throw new ApiException(HttpStatus.CONFLICT, "Idempotency key was already used with a different request");
            }
            return transactions.findFirstByReferenceIdAndType(existing.get().getReferenceId(), TransactionType.TRANSFER_OUT)
                    .map(TransactionService::view)
                    .orElseThrow(() -> ApiException.badRequest("Idempotency record has no transaction"));
        }

        if (request.fromAccountId().equals(request.toAccountId())) {
            throw ApiException.badRequest("Sender and receiver must differ");
        }

        // Every transfer locks both rows in the same order to avoid opposite-direction deadlocks.
        long lowerAccountId = Math.min(request.fromAccountId(), request.toAccountId());
        long higherAccountId = Math.max(request.fromAccountId(), request.toAccountId());
        BankAccount first = lockAccount(lowerAccountId);
        BankAccount second = lockAccount(higherAccountId);
        BankAccount sender = first.getId().equals(request.fromAccountId()) ? first : second;
        BankAccount receiver = first.getId().equals(request.toAccountId()) ? first : second;

        if (!sender.getOwner().getEmail().equals(email)) throw ApiException.forbidden("Sender account access denied");
        requireActive(sender);
        requireActive(receiver);

        BigDecimal amount = amount(request.amount());
        if (sender.getBalance().compareTo(amount) < 0) throw ApiException.badRequest("Insufficient funds");

        String reference = newReference();
        reserveIdempotencyKey(owner, key, reference, requestHash);
        sender.debit(amount);
        receiver.credit(amount);

        BankTransaction debit = transactions.save(new BankTransaction(reference, sender,
                TransactionType.TRANSFER_OUT, amount, request.description()));
        transactions.save(new BankTransaction(reference, receiver,
                TransactionType.TRANSFER_IN, amount, request.description()));
        audits.save(new AuditLog(owner.getId(), "TRANSFER_COMPLETED", reference));

        // Force SQL writes here so database constraint failures enter the audit/rollback path below.
        transactions.flush();
        return view(debit);
    }

    @Transactional(readOnly = true)
    public Page<TransactionView> history(Long accountId, String email, int page, int size,
                                         Instant from, Instant to, TransactionType type) {
        BankAccount account = owned(accountId, email);
        Instant start = from == null ? Instant.EPOCH : from;
        Instant end = to == null ? Instant.now() : to;
        if (start.isAfter(end)) throw ApiException.badRequest("from must be before to");

        Pageable pageable = transactionPage(page, size);
        return transactions.searchOwnerHistory(account.getId(), start, end, type, pageable).map(TransactionService::view);
    }

    @Transactional(readOnly = true)
    public AccountStatement statement(Long accountId, String email, int page, int size, Instant from, Instant to) {
        BankAccount account = owned(accountId, email);
        Instant start = from == null ? Instant.EPOCH : from;
        Instant end = to == null ? Instant.now() : to;
        if (start.isAfter(end)) throw ApiException.badRequest("from must be before to");

        BigDecimal opening = transactions.findFirstByAccountIdAndCreatedAtBeforeOrderByCreatedAtDescIdDesc(accountId, start)
                .map(BankTransaction::getBalanceAfter)
                .orElse(BigDecimal.ZERO.setScale(2));
        Page<TransactionView> entries = transactions.searchOwnerHistory(accountId, start, end, null,
                transactionPage(page, size)).map(TransactionService::view);
        BigDecimal closing = transactions.findFirstByAccountIdAndCreatedAtBetweenOrderByCreatedAtDescIdDesc(accountId, start, end)
                .map(BankTransaction::getBalanceAfter)
                .orElse(opening);
        return new AccountStatement(accountId, account.getAccountNumber(), start, end, opening, closing, entries);
    }

    @Transactional(readOnly = true)
    public TransactionView get(Long id, String email) {
        BankTransaction transaction = transactions.findById(id)
                .orElseThrow(() -> ApiException.notFound("Transaction not found"));
        if (!transaction.getAccount().getOwner().getEmail().equals(email)) {
            throw ApiException.forbidden("Transaction access denied");
        }
        return view(transaction);
    }

    private void reserveIdempotencyKey(AppUser owner, String key, String reference, String requestHash) {
        try {
            idempotencyRecords.saveAndFlush(new IdempotencyRecord(owner, key, reference, requestHash));
        } catch (DataIntegrityViolationException duplicate) {
            throw new ApiException(HttpStatus.CONFLICT, "Idempotency key already used");
        }
    }

    private BankAccount owned(Long id, String email) {
        BankAccount account = accounts.findById(id).orElseThrow(() -> ApiException.notFound("Account not found"));
        if (!account.getOwner().getEmail().equals(email)) throw ApiException.forbidden("Account access denied");
        return account;
    }

    private BankAccount lockOwned(Long id, String email) {
        BankAccount account = lockAccount(id);
        if (!account.getOwner().getEmail().equals(email)) throw ApiException.forbidden("Account access denied");
        return account;
    }

    private BankAccount lockAccount(Long id) {
        return accounts.lockById(id).orElseThrow(() -> ApiException.notFound("Account not found"));
    }

    private static void requireActive(BankAccount account) {
        if (account.getStatus() != AccountStatus.ACTIVE) throw ApiException.badRequest("Account is not active");
    }

    private static BigDecimal amount(BigDecimal value) {
        if (value == null || value.scale() > 2 || value.signum() <= 0) {
            throw ApiException.badRequest("Amount must be positive with at most two decimal places");
        }
        return value.setScale(2);
    }

    private TransactionView save(BankAccount account, TransactionType type, BigDecimal amount,
                                 String description, String reference) {
        BankTransaction transaction = transactions.save(new BankTransaction(reference, account, type, amount, description));
        audits.save(new AuditLog(account.getOwner().getId(), type.name(), reference));
        return view(transaction);
    }

    private void recordFailure(String email, String operation, RuntimeException failure, String details) {
        try {
            failureAudits.record(email, operation, failure, details);
        } catch (RuntimeException auditFailure) {
            // Keep the primary business failure and rollback behavior even if audit storage is unavailable.
            failure.addSuppressed(auditFailure);
        }
    }

    private static void validateIdempotencyKey(String key) {
        if (key == null || key.isBlank() || key.length() > 100) {
            throw ApiException.badRequest("A valid Idempotency-Key header is required");
        }
    }

    private static String fingerprint(TransferRequest request) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String canonical = request.fromAccountId() + "|" + request.toAccountId() + "|"
                    + request.amount().stripTrailingZeros().toPlainString() + "|"
                    + Objects.toString(request.description(), "");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private static String newReference() { return UUID.randomUUID().toString(); }

    private static Pageable transactionPage(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }

    private static String moneyAttemptDetails(MoneyRequest request) {
        return request == null ? "Request body was null"
                : "accountId=" + request.accountId() + ", amount=" + request.amount();
    }

    private static String transferAttemptDetails(TransferRequest request) {
        return request == null ? "Request body was null"
                : "fromAccountId=" + request.fromAccountId() + ", toAccountId=" + request.toAccountId()
                + ", amount=" + request.amount();
    }

    private static TransactionView view(BankTransaction transaction) {
        return new TransactionView(transaction.getId(), transaction.getReferenceId(), transaction.getAccount().getId(),
                transaction.getType(), transaction.getAmount(), transaction.getBalanceAfter(),
                transaction.getDescription(), transaction.getCreatedAt());
    }
}
