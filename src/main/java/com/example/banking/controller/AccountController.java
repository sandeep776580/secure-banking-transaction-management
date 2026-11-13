
package com.example.banking.controller;

import com.example.banking.dto.ApiDtos.AccountView;
import com.example.banking.dto.ApiDtos.AccountStatement;
import com.example.banking.dto.ApiDtos.BalanceView;
import com.example.banking.service.TransactionService;
import com.example.banking.service.AccountService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Accounts", description = "Customer-owned bank accounts and balances")
public class AccountController {

    private final AccountService service;
    private final TransactionService transactions;

    public AccountController(AccountService service, TransactionService transactions) {
        this.service = service;
        this.transactions = transactions;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountView create(Authentication authentication) {
        return service.create(authentication.getName());
    }

    @GetMapping
    public List<AccountView> list(Authentication authentication) {
        return service.list(authentication.getName());
    }

    @GetMapping("/{id}")
    public AccountView get(@PathVariable Long id, Authentication authentication) {
        return service.get(id, authentication.getName(), false);
    }

    @GetMapping("/{id}/balance")
    public BalanceView balance(@PathVariable Long id, Authentication authentication) {
        AccountView account = service.get(id, authentication.getName(), false);
        return new BalanceView(
                account.id(),
                account.accountNumber(),
                account.balance(),
                "USD"
        );
    }

    @GetMapping("/{id}/statement")
    public AccountStatement statement(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            Authentication authentication) {

        return transactions.statement(
                id,
                authentication.getName(),
                page,
                size,
                from,
                to
        );
    }
}
