package com.banking.domain.service;

import com.banking.domain.model.Transaction;
import com.banking.domain.model.TransactionStatus;
import com.banking.domain.port.TransactionRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public void processNightlyBatch() {
        // Fetch all PENDING transactions
        List<Transaction> pendingTransactions = transactionRepository.findByStatus(TransactionStatus.PENDING);

        // Print mock report to console
        System.out.println("=== NIGHTLY TRANSACTION BATCH REPORT ===");
        System.out.println("Processing " + pendingTransactions.size() + " pending transactions:");

        for (Transaction transaction : pendingTransactions) {
            System.out.println("Transaction ID: " + transaction.getId() +
                             ", Account: " + transaction.getAccountId() +
                             ", Type: " + transaction.getType() +
                             ", Amount: " + transaction.getAmount());
        }

        // Change status to COMPLETED and save
        for (Transaction transaction : pendingTransactions) {
            transaction.setStatus(TransactionStatus.COMPLETED);
            transactionRepository.save(transaction);
        }

        System.out.println("All pending transactions have been processed and marked as COMPLETED.");
        System.out.println("=====================================");
    }
}
