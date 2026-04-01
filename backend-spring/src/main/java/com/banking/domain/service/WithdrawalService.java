package com.banking.domain.service;

import com.banking.domain.model.Account;
import com.banking.domain.model.Transaction;
import com.banking.domain.model.TransactionStatus;
import com.banking.domain.model.TransactionType;
import com.banking.domain.port.AccountRepository;
import com.banking.domain.port.TransactionRepository;
import java.math.BigDecimal;

/**
 * WithdrawalService: Thin Orchestrator Layer
 * 
 * This service is a USE CASE orchestrator that coordinates:
 * 1. Loading aggregates from repositories
 * 2. Invoking domain operations on those aggregates
 * 3. Persisting the modified aggregates back to repositories
 * 
 * IMPORTANT: This service contains NO business logic. All business rules
 * (validation, invariants) are enforced by the Account aggregate (Rich Domain Model).
 * 
 * Domain exceptions (e.g., InsufficientFundsException) propagate to the adapter layer
 * for HTTP translation.
 */
public class WithdrawalService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public WithdrawalService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Execute withdrawal use case.
     * 
     * Orchestration steps:
     * 1. Load Account aggregate from repository
     * 2. Delegate business logic to account.withdraw(amount)
     *    - May throw InactiveAccountException, InvalidWithdrawalAmountException, or InsufficientFundsException
     * 3. Persist updated Account back to repository
     * 4. Create and persist a PENDING Transaction record
     * 
     * @param accountId The account ID to withdraw from
     * @param amount The amount to withdraw
     * @throws IllegalArgumentException If account not found
     * @throws com.banking.domain.exception.InactiveAccountException If account is inactive
     * @throws com.banking.domain.exception.InvalidWithdrawalAmountException If amount is invalid
     * @throws com.banking.domain.exception.InsufficientFundsException If insufficient balance
     */
    public void withdrawMoney(Long accountId, BigDecimal amount) {
        // Step 1: Load the Account aggregate from repository
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        // Step 2: Invoke domain operation on the aggregate
        // This enforces all invariants and may throw domain exceptions
        account.withdraw(amount);

        // Step 3: Persist the modified Account back to repository
        accountRepository.save(account);

        // Step 4: Record the transaction as PENDING for later processing by batch job
        Transaction transaction = new Transaction(
                null,
                accountId,
                amount,
                TransactionType.WITHDRAWAL,
                TransactionStatus.PENDING
        );
        transactionRepository.save(transaction);
    }
}