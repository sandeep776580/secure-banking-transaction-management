package com.example.banking.controller;

import com.example.banking.dto.ApiDtos.*;
import com.example.banking.entity.TransactionType;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.TransactionRepository;
import com.example.banking.repository.UserRepository;
import com.example.banking.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin", description = "ADMIN-only user, account, transaction, and statistics APIs")
public class AdminController {
    private final UserRepository users;
    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final AccountService accountService;

    public AdminController(UserRepository users, AccountRepository accounts, TransactionRepository transactions,
                           AccountService accountService) {
        this.users = users;
        this.accounts = accounts;
        this.transactions = transactions;
        this.accountService = accountService;
    }

    @GetMapping("/users")
    @Operation(summary = "Search and page customers by email or name")
    public Page<UserView> users(@RequestParam(required = false) String q,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "50") int size) {
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100),
                Sort.by(Sort.Direction.ASC, "email"));
        return users.searchCustomers(q == null || q.isBlank() ? null : q.trim(), pageable)
                .map(user -> new UserView(user.getId(), user.getEmail(), user.getFullName(), user.getRoles()));
    }

    @GetMapping("/accounts")
    public List<AccountView> accounts() { return accounts.findAll().stream().map(AccountService::view).toList(); }

    @GetMapping("/transactions")
    public Page<TransactionView> transactions(@RequestParam(required = false) Long accountId,
                                               @RequestParam(required = false) TransactionType type,
                                               @RequestParam(required = false) Instant from,
                                               @RequestParam(required = false) Instant to,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "50") int size) {
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return transactions.adminSearch(accountId, type, from, to, pageable).map(transaction ->
                new TransactionView(transaction.getId(), transaction.getReferenceId(), transaction.getAccount().getId(),
                        transaction.getType(), transaction.getAmount(), transaction.getBalanceAfter(),
                        transaction.getDescription(), transaction.getCreatedAt()));
    }

    @GetMapping("/transactions/suspicious")
    @Operation(summary = "Find high-value transfers for manual review; this is a threshold heuristic, not fraud detection")
    public Page<TransactionView> suspiciousTransfers(@RequestParam(defaultValue = "10000.00") java.math.BigDecimal minAmount,
                                                       @RequestParam(required = false) Instant since,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "50") int size) {
        if (minAmount == null || minAmount.signum() <= 0) {
            throw com.example.banking.exception.ApiException.badRequest("minAmount must be positive");
        }
        Instant start = since == null ? Instant.now().minus(java.time.Duration.ofHours(24)) : since;
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return transactions.findByTypeAndAmountGreaterThanEqualAndCreatedAtAfter(
                        TransactionType.TRANSFER_OUT, minAmount, start, pageable)
                .map(transaction -> new TransactionView(transaction.getId(), transaction.getReferenceId(),
                        transaction.getAccount().getId(), transaction.getType(), transaction.getAmount(),
                        transaction.getBalanceAfter(), transaction.getDescription(), transaction.getCreatedAt()));
    }

    @GetMapping("/statistics")
    public Map<String, Long> statistics() {
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("users", users.count());
        result.put("accounts", accounts.count());
        for (TransactionType type : TransactionType.values())
            result.put(type.name().toLowerCase(Locale.ROOT), transactions.countByType(type));
        return result;
    }

    @PatchMapping("/accounts/{id}/status")
    public AccountView status(@PathVariable Long id, @Valid @RequestBody AccountStatusRequest request,
                              Authentication authentication) {
        Long actorId = users.findByEmail(authentication.getName()).orElseThrow().getId();
        return accountService.status(id, request.status(), actorId);
    }
}
