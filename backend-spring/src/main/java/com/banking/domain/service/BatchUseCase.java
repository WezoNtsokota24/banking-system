package com.banking.domain.service;

/**
 * Use case interface for batch processing operations.
 */
public interface BatchUseCase {

    /**
     * Process pending transactions.
     * This method should finalize all pending transactions.
     */
    void processPendingTransactions();
}
