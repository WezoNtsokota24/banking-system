package com.banking.domain.service;

/**
 * Use case interface for finalizing pending transactions.
 * Defines the contract for the nightly batch process that completes pending transactions.
 */
public interface PendingTransactionFinalizationUseCase {

    /**
     * Process and finalize pending transactions.
     * This method should fetch all pending transactions and mark them as completed.
     */
    void processPendingTransactions();
}

