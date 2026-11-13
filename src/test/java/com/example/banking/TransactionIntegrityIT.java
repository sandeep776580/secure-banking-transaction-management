package com.example.banking;

import com.example.banking.dto.ApiDtos.*;
import com.example.banking.entity.BankAccount;
import com.example.banking.exception.ApiException;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.AuditRepository;
import com.example.banking.repository.TransactionRepository;
import com.example.banking.repository.UserRepository;
import com.example.banking.service.AccountService;
import com.example.banking.service.AuthService;
import com.example.banking.service.TransactionService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.time.Instant;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class TransactionIntegrityIT {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("banking_test")
            .withUsername("banking")
            .withPassword("banking_test_password");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("app.jwt.secret", () -> "integration-test-secret-more-than-thirty-two-bytes");
    }

    @Autowired AuthService auth;
    @Autowired AccountService accountService;
    @Autowired TransactionService transactions;
    @Autowired AccountRepository accounts;
    @Autowired TransactionRepository transactionRepository;
    @Autowired AuditRepository auditRepository;
    @Autowired UserRepository userRepository;
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;

    private String email;
    private Long sender;
    private Long receiverOne;
    private Long receiverTwo;

    @BeforeEach
    void createAccounts() {
        email = "customer-" + UUID.randomUUID() + "@example.test";
        auth.register(new RegisterRequest(email, "strong-password-for-test", "Test Customer"));
        sender = accountService.create(email).id();
        receiverOne = accountService.create(email).id();
        receiverTwo = accountService.create(email).id();
    }

    @Test
    void insufficientFundsCreatesIndependentFailureAuditAndDoesNotChangeBalances() {
        long auditCount = auditRepository.count();
        assertThrows(ApiException.class, () -> transactions.transfer(
                transfer(sender, receiverOne, "5.00", "Rejected transfer"), email, "insufficient-" + UUID.randomUUID()));
        assertEquals(0, balance(sender).compareTo(BigDecimal.ZERO.setScale(2)));
        assertEquals(auditCount + 1, auditRepository.count());
    }

    @Test
    void refreshTokenRotatesAndCannotBeReused() {
        AuthResponse first = auth.register(new RegisterRequest("refresh-" + UUID.randomUUID() + "@example.test",
                "strong-password-for-test", "Refresh Customer"));
        AuthResponse rotated = auth.refresh(new RefreshRequest(first.refreshToken()));
        assertNotEquals(first.refreshToken(), rotated.refreshToken());
        assertThrows(ApiException.class, () -> auth.refresh(new RefreshRequest(first.refreshToken())));
    }

    @Test
    void invalidJwtAndCustomerAdminAccessReturnStructuredErrors() throws Exception {
        mockMvc.perform(get("/api/v1/accounts").header("Authorization", "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        AuthResponse customer = auth.register(new RegisterRequest("admin-check-" + UUID.randomUUID() + "@example.test",
                "strong-password-for-test", "Regular Customer"));
        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + customer.accessToken()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void logoutRevokesAccessAndRefreshTokens() throws Exception {
        AuthResponse session = auth.register(new RegisterRequest("logout-" + UUID.randomUUID() + "@example.test",
                "strong-password-for-test", "Logout Customer"));
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + session.accessToken())
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"" + session.refreshToken() + "\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/accounts").header("Authorization", "Bearer " + session.accessToken()))
                .andExpect(status().isUnauthorized());
        assertThrows(ApiException.class, () -> auth.refresh(new RefreshRequest(session.refreshToken())));
    }

    @Test
    void customerCannotReadAnotherCustomersAccount() {
        String otherEmail = "other-" + UUID.randomUUID() + "@example.test";
        auth.register(new RegisterRequest(otherEmail, "strong-password-for-test", "Other Customer"));
        Long otherAccount = accountService.create(otherEmail).id();
        assertThrows(ApiException.class, () -> accountService.get(otherAccount, email, false));
    }

    @Test
    void accountStatementReportsOpeningAndClosingBalances() {
        transactions.deposit(new MoneyRequest(new BigDecimal("100.00"), sender, "Seed"), email);
        transactions.withdraw(new MoneyRequest(new BigDecimal("25.00"), sender, "Spend"), email);
        var statement = transactions.statement(sender, email, 0, 100, Instant.EPOCH, Instant.now());
        assertEquals(0, statement.openingBalance().compareTo(BigDecimal.ZERO.setScale(2)));
        assertEquals(0, statement.closingBalance().compareTo(new BigDecimal("75.00")));
        assertEquals(2, statement.transactions().getTotalElements());
    }

    @Test
    void userSearchFiltersByEmailOrName() {
        var result = userRepository.searchCustomers("customer-", org.springframework.data.domain.PageRequest.of(0, 20));
        assertTrue(result.getContent().stream().anyMatch(user -> user.getEmail().equals(email)));
    }

    @Test
    void adminSearchMonitoringAndAccountStatusEndpointsWork() throws Exception {
        String adminEmail = "operator-" + UUID.randomUUID() + "@example.test";
        auth.register(new RegisterRequest(adminEmail, "strong-password-for-test", "Test Operator"));
        Long adminId = userRepository.findByEmail(adminEmail).orElseThrow().getId();
        jdbc.update("insert into user_roles(user_id, role) values (?, 'ADMIN')", adminId);
        String adminToken = auth.login(new LoginRequest(adminEmail, "strong-password-for-test")).accessToken();

        mockMvc.perform(get("/api/v1/admin/users?q=" + email)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].email").value(email));

        transactions.deposit(new MoneyRequest(new BigDecimal("11000.00"), sender, "High-value test"), email);
        transactions.transfer(transfer(sender, receiverOne, "10000.00", "High-value test"), email,
                "admin-monitor-" + UUID.randomUUID());
        mockMvc.perform(get("/api/v1/admin/transactions/suspicious?minAmount=10000.00")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                        "/api/v1/admin/accounts/" + receiverOne + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json").content("{\"status\":\"FROZEN\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FROZEN"));
    }

    @Test
    void sqlFailureAfterDebitRollsBackBothBalancesAndTransactionRows() {
        transactions.deposit(new MoneyRequest(new BigDecimal("100.00"), sender, "Seed"), email);
        long transactionCount = transactionRepository.count();
        long auditCount = auditRepository.count();
        String invalidDescription = "x".repeat(256);
        assertThrows(RuntimeException.class, () -> transactions.transfer(
                transfer(sender, receiverOne, "10.00", invalidDescription), email, "rollback-" + UUID.randomUUID()));
        assertEquals(0, balance(sender).compareTo(new BigDecimal("100.00")));
        assertEquals(0, balance(receiverOne).compareTo(BigDecimal.ZERO.setScale(2)));
        assertEquals(transactionCount, transactionRepository.count());
        assertEquals(auditCount + 1, auditRepository.count());
    }

    @Test
    void receiverBalanceOverflowRollsBackTheSenderDebit() {
        BigDecimal maximumBalance = new BigDecimal("99999999999999999.99");
        transactions.deposit(new MoneyRequest(new BigDecimal("100.00"), sender, "Seed sender"), email);
        transactions.deposit(new MoneyRequest(maximumBalance, receiverOne, "Fill receiver"), email);
        long transactionCount = transactionRepository.count();
        long auditCount = auditRepository.count();
        assertThrows(RuntimeException.class, () -> transactions.transfer(
                transfer(sender, receiverOne, "1.00", "Overflow rollback"), email, "overflow-" + UUID.randomUUID()));
        assertEquals(0, balance(sender).compareTo(new BigDecimal("100.00")));
        assertEquals(0, balance(receiverOne).compareTo(maximumBalance));
        assertEquals(transactionCount, transactionRepository.count());
        assertEquals(auditCount + 1, auditRepository.count());
    }

    @Test
    void concurrentTransfersCannotSpendTheSameBalanceTwice() throws Exception {
        transactions.deposit(new MoneyRequest(new BigDecimal("100.00"), sender, "Seed"), email);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = pool.submit(() -> attemptTransfer(sender, receiverOne, "80.00", "race-a"));
            Future<Boolean> second = pool.submit(() -> attemptTransfer(sender, receiverTwo, "80.00", "race-b"));
            int successes = (first.get(30, TimeUnit.SECONDS) ? 1 : 0) + (second.get(30, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, successes);
            BigDecimal total = balance(sender).add(balance(receiverOne)).add(balance(receiverTwo));
            assertEquals(0, total.compareTo(new BigDecimal("100.00")));
            assertTrue(balance(sender).compareTo(BigDecimal.ZERO) >= 0);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void concurrentSameIdempotencyKeyCannotCreateTwoTransfers() throws Exception {
        transactions.deposit(new MoneyRequest(new BigDecimal("100.00"), sender, "Seed"), email);
        long initialTransactions = transactionRepository.count();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        String key = "duplicate-" + UUID.randomUUID();
        try {
            Future<Boolean> first = pool.submit(() -> attemptTransfer(sender, receiverOne, "10.00", key));
            Future<Boolean> second = pool.submit(() -> attemptTransfer(sender, receiverOne, "10.00", key));
            int successes = (first.get(30, TimeUnit.SECONDS) ? 1 : 0) + (second.get(30, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, successes);
            assertEquals(0, balance(sender).compareTo(new BigDecimal("90.00")));
            assertEquals(0, balance(receiverOne).compareTo(new BigDecimal("10.00")));
            assertEquals(initialTransactions + 2, transactionRepository.count());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void successfulIdempotencyRetryReplaysResultAndRejectsChangedPayload() {
        transactions.deposit(new MoneyRequest(new BigDecimal("100.00"), sender, "Seed"), email);
        String key = "replay-" + UUID.randomUUID();
        TransferRequest request = transfer(sender, receiverOne, "10.00", "Replay test");
        var first = transactions.transfer(request, email, key);
        var retry = transactions.transfer(request, email, key);
        assertEquals(first.referenceId(), retry.referenceId());
        assertEquals(0, balance(sender).compareTo(new BigDecimal("90.00")));
        assertThrows(ApiException.class, () -> transactions.transfer(
                transfer(sender, receiverOne, "11.00", "Changed request"), email, key));
        assertEquals(0, balance(sender).compareTo(new BigDecimal("90.00")));
    }

    private boolean attemptTransfer(Long from, Long to, String amount, String key) {
        try {
            transactions.transfer(transfer(from, to, amount, "Concurrent test"), email, key);
            return true;
        } catch (RuntimeException expectedFailure) {
            return false;
        }
    }

    private static TransferRequest transfer(Long from, Long to, String amount, String description) {
        return new TransferRequest(from, to, new BigDecimal(amount), description);
    }

    private BigDecimal balance(Long id) {
        return accounts.findById(id).map(BankAccount::getBalance).orElseThrow();
    }
}
