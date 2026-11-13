package com.example.banking.dto;

import com.example.banking.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import org.springframework.data.domain.Page;

public final class ApiDtos {
 private ApiDtos() {}
 public record RegisterRequest(@NotBlank @Email String email,@NotBlank @Size(min=12,max=72) String password,@NotBlank @Size(max=120) String fullName){}
 public record LoginRequest(@NotBlank @Email String email,@NotBlank String password){}
 public record AuthResponse(String accessToken,String refreshToken,String tokenType,long expiresIn,long refreshExpiresIn,UserView user){}
 public record RefreshRequest(@NotBlank String refreshToken){}
 public record UserView(Long id,String email,String fullName,Set<Role> roles){}
 public record AccountView(Long id,String accountNumber,BigDecimal balance,AccountStatus status,Instant createdAt){}
 public record BalanceView(Long accountId,String accountNumber,BigDecimal balance,String currency){}
 public record MoneyRequest(@NotNull @Positive @Digits(integer=17,fraction=2) BigDecimal amount,@NotNull Long accountId,@Size(max=255) String description){}
 public record TransferRequest(@NotNull Long fromAccountId,@NotNull Long toAccountId,@NotNull @Positive @Digits(integer=17,fraction=2) BigDecimal amount,@Size(max=255) String description){}
 public record TransactionView(Long id,String referenceId,Long accountId,TransactionType type,BigDecimal amount,BigDecimal balanceAfter,String description,Instant createdAt){}
 public record AccountStatement(Long accountId,String accountNumber,Instant from,Instant to,BigDecimal openingBalance,BigDecimal closingBalance,Page<TransactionView> transactions){}
 public record AccountStatusRequest(@NotNull AccountStatus status){}
 public record LogoutRequest(@NotBlank String refreshToken){}
 public record ErrorResponse(Instant timestamp,int status,String error,String message,String path){}
}
