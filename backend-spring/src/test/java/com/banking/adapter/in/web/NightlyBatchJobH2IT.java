package com.banking.adapter.in.web;

import com.banking.domain.model.Account;
import com.banking.domain.model.AccountStatus;
import com.banking.domain.model.Transaction;
import com.banking.domain.model.TransactionStatus;
import com.banking.domain.port.AccountRepository;
import com.banking.domain.port.TransactionRepository;
import com.banking.domain.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Test for Nightly Batch Job using H2 In-Memory Database
 *
 * This test class uses H2 to test the nightly batch job that processes PENDING transactions.
 * It provides a way to run integration tests without requiring Docker.
 *
 * Tests cover the nightly batch process that marks PENDING transactions as COMPLETED.
 */
@SpringBootTest
@ActiveProfiles("local")
@Transactional
class NightlyBatchJobH2IT {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    private Account account1;
    private Account account2;

    /**
     * Set up test data before each test.
     * Uses @BeforeEach to ensure fresh data for each test (via @Transactional rollback).
     */
    @BeforeEach
    void setUp() {
        // Create test accounts
        account1 = new Account(null, "ACC-001", new BigDecimal("1000.00"), AccountStatus.ACTIVE);
        account1 = accountRepository.save(account1);

        account2 = new Account(null, "ACC-002", new BigDecimal("500.00"), AccountStatus.ACTIVE);
        account2 = accountRepository.save(account2);
    }

    @Test
    @DisplayName("Nightly batch job should process PENDING transactions and mark them as COMPLETED")
    void testNightlyBatchJob_shouldProcessPendingTransactions() {
        // Arrange: Create PENDING transactions
        Transaction pendingTx1 = new Transaction(null, account1.getId(), new BigDecimal("100.00"), TransactionStatus.PENDING);
        transactionRepository.save(pendingTx1);

        Transaction pendingTx2 = new Transaction(null, account1.getId(), new BigDecimal("50.00"), TransactionStatus.PENDING);
        transactionRepository.save(pendingTx2);

        Transaction pendingTx3 = new Transaction(null, account2.getId(), new BigDecimal("25.00"), TransactionStatus.PENDING);
        transactionRepository.save(pendingTx3);

        // Verify initial state: 3 PENDING transactions
        List<Transaction> initialPending = transactionRepository.findByStatus(TransactionStatus.PENDING);
        assertEquals(3, initialPending.size(), "Should have 3 PENDING transactions initially");

        List<Transaction> initialCompleted = transactionRepository.findByStatus(TransactionStatus.COMPLETED);
        assertTrue(initialCompleted.isEmpty(), "Should have no COMPLETED transactions initially");

        // Act: Trigger the nightly batch job
        transactionService.processPendingTransactions();

        // Assert: All PENDING transactions should now be COMPLETED
        List<Transaction> finalPending = transactionRepository.findByStatus(TransactionStatus.PENDING);
        assertTrue(finalPending.isEmpty(), "Should have no PENDING transactions after batch job");

        List<Transaction> finalCompleted = transactionRepository.findByStatus(TransactionStatus.COMPLETED);
        assertEquals(3, finalCompleted.size(), "Should have 3 COMPLETED transactions after batch job");

        // Verify specific transactions are completed
        assertTrue(finalCompleted.stream().anyMatch(tx ->
                tx.getAccountId().equals(account1.getId()) && tx.getAmount().compareTo(new BigDecimal("100.00")) == 0),
                "First transaction should be completed");

        assertTrue(finalCompleted.stream().anyMatch(tx ->
                tx.getAccountId().equals(account1.getId()) && tx.getAmount().compareTo(new BigDecimal("50.00")) == 0),
                "Second transaction should be completed");

        assertTrue(finalCompleted.stream().anyMatch(tx ->
                tx.getAccountId().equals(account2.getId()) && tx.getAmount().compareTo(new BigDecimal("25.00")) == 0),
                "Third transaction should be completed");
    }

    @Test
    @DisplayName("Nightly batch job with no PENDING transactions should do nothing")
    void testNightlyBatchJob_withNoPendingTransactions_shouldDoNothing() {
        // Arrange: No PENDING transactions

        // Act: Trigger the nightly batch job
        transactionService.processPendingTransactions();

        // Assert: No transactions should exist
        List<Transaction> pending = transactionRepository.findByStatus(TransactionStatus.PENDING);
        assertTrue(pending.isEmpty(), "Should have no PENDING transactions");

        List<Transaction> completed = transactionRepository.findByStatus(TransactionStatus.COMPLETED);
        assertTrue(completed.isEmpty(), "Should have no COMPLETED transactions");
    }

    @Test
    @DisplayName("Nightly batch job should only process PENDING transactions, ignore COMPLETED ones")
    void testNightlyBatchJob_shouldOnlyProcessPendingTransactions() {
        // Arrange: Mix of PENDING and COMPLETED transactions
        Transaction pendingTx = new Transaction(null, account1.getId(), new BigDecimal("100.00"), TransactionStatus.PENDING);
        transactionRepository.save(pendingTx);

        Transaction completedTx = new Transaction(null, account2.getId(), new BigDecimal("50.00"), TransactionStatus.COMPLETED);
        transactionRepository.save(completedTx);

        // Act: Trigger the nightly batch job
        transactionService.processPendingTransactions();

        // Assert: Only the PENDING transaction should be processed
        List<Transaction> finalPending = transactionRepository.findByStatus(TransactionStatus.PENDING);
        assertTrue(finalPending.isEmpty(), "Should have no PENDING transactions");

        List<Transaction> finalCompleted = transactionRepository.findByStatus(TransactionStatus.COMPLETED);
        assertEquals(2, finalCompleted.size(), "Should have 2 COMPLETED transactions");

        // Verify the originally COMPLETED transaction is still there
        assertTrue(finalCompleted.stream().anyMatch(tx ->
                tx.getAccountId().equals(account2.getId()) && tx.getAmount().compareTo(new BigDecimal("50.00")) == 0),
                "Originally completed transaction should remain completed");
    }

    @Test
    @DisplayName("Nightly batch job should handle large number of PENDING transactions")
    void testNightlyBatchJob_shouldHandleLargeNumberOfTransactions() {
        // Arrange: Create 10 PENDING transactions
        for (int i = 1; i <= 10; i++) {
            Transaction tx = new Transaction(null, account1.getId(), new BigDecimal(i * 10), TransactionStatus.PENDING);
            transactionRepository.save(tx);
        }

        // Act: Trigger the nightly batch job
        transactionService.processPendingTransactions();

        // Assert: All should be completed
        List<Transaction> pending = transactionRepository.findByStatus(TransactionStatus.PENDING);
        assertTrue(pending.isEmpty(), "Should have no PENDING transactions");

        List<Transaction> completed = transactionRepository.findByStatus(TransactionStatus.COMPLETED);
        assertEquals(10, completed.size(), "Should have 10 COMPLETED transactions");
    }
}
