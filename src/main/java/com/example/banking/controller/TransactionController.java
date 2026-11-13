package com.example.banking.controller;

import com.example.banking.dto.ApiDtos.*;
import com.example.banking.entity.TransactionType;
import com.example.banking.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Transactions", description = "Deposits, withdrawals, transfers, and transaction history")
public class TransactionController {
    private final TransactionService service;

    public TransactionController(TransactionService service) { this.service = service; }

    @PostMapping("/deposit")
    @Operation(summary = "Deposit demo funds to an owned account")
    public TransactionView deposit(@Valid @RequestBody MoneyRequest request, Authentication authentication) {
        return service.deposit(request, authentication.getName());
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Withdraw funds from an owned account")
    public TransactionView withdraw(@Valid @RequestBody MoneyRequest request, Authentication authentication) {
        return service.withdraw(request, authentication.getName());
    }

    @PostMapping("/transfer")
    @Operation(summary = "Atomically transfer funds; requires an Idempotency-Key header")
    public TransactionView transfer(@Valid @RequestBody TransferRequest request,
                                    @RequestHeader("Idempotency-Key") String key,
                                    Authentication authentication) {
        return service.transfer(request, authentication.getName(), key);
    }

    @GetMapping("/history")
    public Page<TransactionView> history(@RequestParam Long accountId,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size,
                                         @RequestParam(required = false) Instant from,
                                         @RequestParam(required = false) Instant to,
                                         @RequestParam(required = false) TransactionType type,
                                         Authentication authentication) {
        return service.history(accountId, authentication.getName(), page, size, from, to, type);
    }

    @GetMapping("/{id}")
    public TransactionView get(@PathVariable Long id, Authentication authentication) {
        return service.get(id, authentication.getName());
    }
}
