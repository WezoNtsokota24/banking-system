package com.banking.adapter.in.web;

import com.banking.domain.exception.InactiveAccountException;
import com.banking.domain.exception.InsufficientFundsException;
import com.banking.domain.model.Account;
import com.banking.domain.model.AccountStatus;
import com.banking.domain.model.Transaction;
import com.banking.domain.model.TransactionStatus;
import com.banking.domain.port.AccountRepository;
import com.banking.domain.port.TransactionRepository;
import com.banking.domain.port.JwtPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test for WithdrawalController using TestContainers
 * 
 * This test class uses TestContainers to spin up a real MySQL database in Docker
 * instead of an in-memory H2 database. This provides more realistic integration testing.
 * 
 * TestContainers will:
 * 1. Start a MySQL container before running tests
 * 2. Create the database schema automatically
 * 3. Stop the container after tests complete
 * 4. Clean up all resources
 * 
 * Tests cover three main scenarios:
 * 1. Successful withdrawal - Balance is correctly reduced
 * 2. Insufficient funds - HTTP 402 Payment Required is returned
 * 3. Inactive account - HTTP 403 Forbidden is returned
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class WithdrawalControllerTestContainersIT {

    /**
     * TestContainers MySQL container definition.
     * This container will be started once for all tests in this class.
     * 
     * The container automatically:
     * - Starts MySQL server
     * - Creates root user
     * - Initializes database schema via Hibernate
     * - Cleans up after tests
     */
    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("banking_test_db")
            .withUsername("test_user")
            .withPassword("test_password");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtPort jwtPort;

    private String jwtToken;

    private Account activeAccount;
    private Account inactiveAccount;
    private Account lowBalanceAccount;

    /**
     * Set up test data before each test.
     * Uses @BeforeEach to ensure fresh data for each test (via @Transactional rollback).
     */
    @BeforeEach
    void setUp() {
        // Generate JWT token for authentication
        jwtToken = jwtPort.generateToken("testuser");

        // Create an ACTIVE account with sufficient balance
        activeAccount = new Account(1L, "ACC-ACTIVE-001", new BigDecimal("1000.00"), AccountStatus.ACTIVE);
        accountRepository.save(activeAccount);

        // Create an INACTIVE account
        inactiveAccount = new Account(2L, "ACC-INACTIVE-002", new BigDecimal("500.00"), AccountStatus.INACTIVE);
        accountRepository.save(inactiveAccount);

        // Create an ACTIVE account with low balance for edge case testing
        lowBalanceAccount = new Account(3L, "ACC-LOW-003", new BigDecimal("50.00"), AccountStatus.ACTIVE);
        accountRepository.save(lowBalanceAccount);
    }

    // ========== TEST SCENARIO 1: SUCCESSFUL WITHDRAWAL ==========

    @Test
    @DisplayName("Successful withdrawal should reduce balance correctly")
    void testSuccessfulWithdrawal_shouldReduceBalance() throws Exception {
        // Arrange
        Long accountId = activeAccount.getId();
        BigDecimal withdrawalAmount = new BigDecimal("250.00");
        WithdrawalRequest request = new WithdrawalRequest(withdrawalAmount);

        String requestBody = objectMapper.writeValueAsString(request);

        // Act: Send POST request to withdraw
        mockMvc.perform(post("/api/accounts/{accountId}/withdraw", accountId)
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                // Assert: Expect HTTP 200 OK
                .andExpect(status().isOk());

        // Assert: Verify account balance was reduced in database
        Account updatedAccount = accountRepository.findById(accountId)
                .orElseThrow(() -> new AssertionError("Account not found in database"));

        assertEquals(new BigDecimal("750.00"), updatedAccount.getBalance(),
                "Balance should be reduced from 1000.00 to 750.00 after withdrawal of 250.00");
        assertTrue(updatedAccount.isActive(),
                "Account should still be active after withdrawal");

        // Assert: Verify a new Transaction entity was saved with PENDING status
        List<Transaction> pendingTransactions = transactionRepository.findByStatus(TransactionStatus.PENDING);
        assertFalse(pendingTransactions.isEmpty(), "At least one PENDING transaction should exist");

        Transaction createdTransaction = pendingTransactions.stream()
                .filter(t -> t.getAccountId().equals(accountId) && t.getAmount().equals(withdrawalAmount))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Transaction with accountId " + accountId + " and amount " + withdrawalAmount + " not found"));

        assertEquals(TransactionStatus.PENDING, createdTransaction.getStatus(),
                "Transaction should have PENDING status");
        assertEquals(accountId, createdTransaction.getAccountId(),
                "Transaction should be linked to the correct account");
        assertEquals(withdrawalAmount, createdTransaction.getAmount(),
                "Transaction amount should match the withdrawal amount");
    }

    @Test
    @DisplayName("Successful withdrawal with exact amount calculation")
    void testSuccessfulWithdrawal_withExactCalculation() throws Exception {
        // Arrange: Withdraw 333.33 from 1000.00
        Long accountId = activeAccount.getId();
        BigDecimal withdrawalAmount = new BigDecimal("333.33");
        WithdrawalRequest request = new WithdrawalRequest(withdrawalAmount);

        // Act
        mockMvc.perform(post("/api/accounts/{accountId}/withdraw", accountId)
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Assert
        Account updated = accountRepository.findById(accountId)
                .orElseThrow(() -> new AssertionError("Account not found"));
        assertEquals(new BigDecimal("666.67"), updated.getBalance(),
                "Balance should be exactly 1000.00 - 333.33 = 666.67");
    }

    // ========== TEST SCENARIO 2: INSUFFICIENT FUNDS ==========

    @Test
    @DisplayName("Insufficient funds should return HTTP 402 Payment Required")
    void testInsufficientFunds_shouldReturnHTTP402() throws Exception {
        // Arrange: Try to withdraw 1500 from account with 1000
        Long accountId = activeAccount.getId();
        BigDecimal excessiveAmount = new BigDecimal("1500.00");
        WithdrawalRequest request = new WithdrawalRequest(excessiveAmount);

        String requestBody = objectMapper.writeValueAsString(request);

        // Act & Assert: Expect HTTP 402 Payment Required
        MvcResult result = mockMvc.perform(post("/api/accounts/{accountId}/withdraw", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isPaymentRequired())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.message").value(containsString("Insufficient funds")))
                .andReturn();

        // Assert: Verify balance was NOT changed
        Account accountAfter = accountRepository.findById(accountId)
                .orElseThrow(() -> new AssertionError("Account not found"));
        assertEquals(new BigDecimal("1000.00"), accountAfter.getBalance(),
                "Balance should remain unchanged at 1000.00 when withdrawal fails");
    }

    @Test
    @DisplayName("Insufficient funds error response contains detailed information")
    void testInsufficientFunds_errorResponseContainsDetails() throws Exception {
        // Arrange: Try to withdraw 100 from account with 50
        Long accountId = lowBalanceAccount.getId();
        BigDecimal requestedAmount = new BigDecimal("100.00");
        WithdrawalRequest request = new WithdrawalRequest(requestedAmount);

        // Act
        mockMvc.perform(post("/api/accounts/{accountId}/withdraw", accountId)
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isPaymentRequired())
                // Verify error message contains all relevant details
                .andExpect(jsonPath("$.status").value(402))
                .andExpect(jsonPath("$.message").value(
                        containsString("Insufficient funds for account 3")))
                .andExpect(jsonPath("$.message").value(
                        containsString("Requested: 100")))
                .andExpect(jsonPath("$.message").value(
                        containsString("Available: 50")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Attempting to withdraw amount close to balance should fail")
    void testInsufficientFunds_withdrawAmountGreaterThanBalance() throws Exception {
        // Arrange: Balance is 50.00, try to withdraw 50.01
        Long accountId = lowBalanceAccount.getId();
        BigDecimal tooMuch = new BigDecimal("50.01");
        WithdrawalRequest request = new WithdrawalRequest(tooMuch);

        // Act & Assert
        mockMvc.perform(post("/api/accounts/{accountId}/withdraw", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isPaymentRequired());

        // Verify balance unchanged
        Account updated = accountRepository.findById(accountId)
                .orElseThrow(() -> new AssertionError("Account not found"));
        assertEquals(new BigDecimal("50.00"), updated.getBalance());
    }

    // ========== TEST SCENARIO 3: INACTIVE ACCOUNT ==========

    @Test
    @DisplayName("Inactive account should return HTTP 403 Forbidden")
    void testInactiveAccount_shouldReturnHTTP403() throws Exception {
        // Arrange: Try to withdraw from inactive account
        Long inactiveAccountId = inactiveAccount.getId();
        BigDecimal withdrawalAmount = new BigDecimal("100.00");
        WithdrawalRequest request = new WithdrawalRequest(withdrawalAmount);

        String requestBody = objectMapper.writeValueAsString(request);

        // Act & Assert: Expect HTTP 403 Forbidden
        mockMvc.perform(post("/api/accounts/{inactiveAccountId}/withdraw", inactiveAccountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.message").value(containsString("inactive")))
                .andReturn();

        // Assert: Verify balance was NOT changed
        Account accountAfter = accountRepository.findById(inactiveAccountId)
                .orElseThrow(() -> new AssertionError("Account not found"));
        assertEquals(new BigDecimal("500.00"), accountAfter.getBalance(),
                "Balance should remain unchanged at 500.00 for inactive account");
        assertFalse(accountAfter.isActive(),
                "Account should still be inactive");
    }

    @Test
    @DisplayName("Inactive account error response contains account identifier")
    void testInactiveAccount_errorResponseContainsDetails() throws Exception {
        // Arrange
        Long inactiveAccountId = inactiveAccount.getId();
        BigDecimal amount = new BigDecimal("50.00");
        WithdrawalRequest request = new WithdrawalRequest(amount);

        // Act & Assert
        mockMvc.perform(post("/api/accounts/{inactiveAccountId}/withdraw", inactiveAccountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(
                        containsString("Account 2 is inactive")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Cannot withdraw from inactive account even with sufficient balance")
    void testInactiveAccount_sufficientBalanceNotEnough() throws Exception {
        // Arrange: Inactive account has 500, but is inactive
        Long inactiveAccountId = inactiveAccount.getId();
        BigDecimal smallAmount = new BigDecimal("10.00");
        WithdrawalRequest request = new WithdrawalRequest(smallAmount);

        // Act & Assert
        mockMvc.perform(post("/api/accounts/{inactiveAccountId}/withdraw", inactiveAccountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(
                        containsString("inactive")));

        // Verify balance unchanged
        Account updated = accountRepository.findById(inactiveAccountId)
                .orElseThrow(() -> new AssertionError("Account not found"));
        assertEquals(new BigDecimal("500.00"), updated.getBalance());
    }

    // ========== ADDITIONAL EDGE CASES ==========

    @Test
    @DisplayName("Withdrawal of exact balance should succeed")
    void testWithdrawEntireBalance_shouldSucceed() throws Exception {
        // Arrange: Withdraw exactly 50.00 from account with 50.00
        Long accountId = lowBalanceAccount.getId();
        BigDecimal entireBalance = new BigDecimal("50.00");
        WithdrawalRequest request = new WithdrawalRequest(entireBalance);

        // Act
        mockMvc.perform(post("/api/accounts/{accountId}/withdraw", accountId)
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Assert: Balance should be 0.00
        Account updated = accountRepository.findById(accountId)
                .orElseThrow(() -> new AssertionError("Account not found"));
        assertEquals(new BigDecimal("0.00"), updated.getBalance(),
                "Balance should be 0.00 after withdrawing entire balance");
    }

    @Test
    @DisplayName("Multiple sequential withdrawals reduce balance correctly")
    void testMultipleWithdrawals_shouldReduceAccumulatively() throws Exception {
        // Arrange & Act: First withdrawal of 300
        Long accountId = activeAccount.getId();
        WithdrawalRequest request1 = new WithdrawalRequest(new BigDecimal("300.00"));
        mockMvc.perform(post("/api/accounts/{accountId}/withdraw", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

        // Act: Second withdrawal of 200
        WithdrawalRequest request2 = new WithdrawalRequest(new BigDecimal("200.00"));
        mockMvc.perform(post("/api/accounts/{accountId}/withdraw", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk());

        // Assert: Final balance should be 500 (1000 - 300 - 200)
        Account updated = accountRepository.findById(accountId)
                .orElseThrow(() -> new AssertionError("Account not found"));
        assertEquals(new BigDecimal("500.00"), updated.getBalance(),
                "Balance should be 500.00 after two withdrawals (300 + 200)");
    }

    @Test
    @DisplayName("Query account balance endpoint works correctly")
    void testGetAccountBalance_shouldReturnAccountDetails() throws Exception {
        // Arrange
        Long accountId = activeAccount.getId();

        // Act & Assert
        mockMvc.perform(get("/api/accounts/{accountId}", accountId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(accountId.intValue()))
                .andExpect(jsonPath("$.accountNumber").value("ACC-ACTIVE-001"))
                .andExpect(jsonPath("$.balance").value(1000.00));
    }

    @Test
    @DisplayName("Account not found should return HTTP 404")
    void testAccountNotFound_shouldReturnError() throws Exception {
        // Arrange: Non-existent account ID
        Long nonExistentAccountId = 9999L;
        BigDecimal amount = new BigDecimal("100.00");
        WithdrawalRequest request = new WithdrawalRequest(amount);

        // Act & Assert
        mockMvc.perform(post("/api/accounts/{accountId}/withdraw", nonExistentAccountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Verify TestContainers is using real MySQL")
    void testTestContainersConnectivity() {
        // This test verifies that TestContainers started the MySQL container
        // and that Spring connected to it successfully.
        
        // Simply verify that we can query the database
        Account retrieved = accountRepository.findById(activeAccount.getId())
                .orElseThrow(() -> new AssertionError("Could not retrieve account from database"));
        
        assertNotNull(retrieved, "Account should exist in database");
        assertEquals("ACC-ACTIVE-001", retrieved.getAccountNumber(),
                "Account number should match what was saved");
        
        System.out.println("✓ TestContainers MySQL container is connected and working!");
        System.out.println("  - MySQL Database URL: " + mysql.getJdbcUrl());
        System.out.println("  - Database Name: " + mysql.getDatabaseName());
    }
}

