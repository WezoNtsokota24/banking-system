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
public class TransactionService implements PendingTransactionFinalizationUseCase {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Process pending transactions.
     * 
     * Orchestration steps:
     * 1. Fetch all PENDING transactions
     * 2. Transition each transaction from PENDING to COMPLETED
     * 3. Persist updates
     * 4. Log the number of processed transactions
     */
    @Override
    public void processPendingTransactions() {
        // Step 1: Fetch all PENDING transactions from repository
        List<Transaction> pendingTransactions = transactionRepository.findByStatus(TransactionStatus.PENDING);

        // Step 2: Process each transaction
        for (Transaction transaction : pendingTransactions) {
            // Invoke domain method to transition state
            transaction.complete();
            // Step 3: Persist the updated transaction
            transactionRepository.save(transaction);
        }

        // Step 4: Log the number of processed transactions
        System.out.println(pendingTransactions.size() + " pending transactions have been processed and marked as COMPLETED.");
    }
}
