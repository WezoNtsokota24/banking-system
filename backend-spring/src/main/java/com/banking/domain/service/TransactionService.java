package com.banking.domain.service;

import com.banking.domain.model.Transaction;
import com.banking.domain.model.TransactionStatus;
import com.banking.domain.port.TransactionRepository;
import java.util.List;

/**
 * TransactionService: Use Case Orchestrator for Batch Processing
 * 
 * This service orchestrates the nightly batch process:
 * 1. Fetch all PENDING transactions from the repository
 * 2. Process them through domain state transitions
 * 3. Persist the updated transactions back
 * 
 * This is a thin orchestrator that delegates domain logic to the Transaction aggregate.
 */
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Process the nightly batch of pending transactions.
     * 
     * Orchestration steps:
     * 1. Fetch all PENDING transactions
     * 2. Print audit report
     * 3. Transition each transaction from PENDING to COMPLETED
     * 4. Persist updates
     */
    public void processNightlyBatch() {
        // Step 1: Fetch all PENDING transactions from repository
        List<Transaction> pendingTransactions = transactionRepository.findByStatus(TransactionStatus.PENDING);

        // Step 2: Print audit report
        printBatchReport(pendingTransactions);

        // Step 3: Process each transaction
        for (Transaction transaction : pendingTransactions) {
            // Invoke domain method to transition state
            transaction.complete();
            // Step 4: Persist the updated transaction
            transactionRepository.save(transaction);
        }

        System.out.println("All pending transactions have been processed and marked as COMPLETED.");
        System.out.println("=====================================");
    }

    /**
     * Print audit report for the batch process.
     * 
     * @param transactions The list of transactions to report on
     */
    private void printBatchReport(List<Transaction> transactions) {
        System.out.println("=== NIGHTLY TRANSACTION BATCH REPORT ===");
        System.out.println("Processing " + transactions.size() + " pending transactions:");

        for (Transaction transaction : transactions) {
            System.out.println("Transaction ID: " + transaction.getId() +
                             ", Account: " + transaction.getAccountId() +
                             ", Type: " + transaction.getType() +
                             ", Amount: " + transaction.getAmount() +
                             ", Status: " + transaction.getStatus());
        }
    }
}
