package com.banking.domain.model;

import com.banking.domain.exception.InsufficientFundsException;
import com.banking.domain.exception.InactiveAccountException;
import com.banking.domain.exception.InvalidWithdrawalAmountException;
import java.math.BigDecimal;

/**
 * Rich Domain Model: Account
 * 
 * This Account aggregate embodies domain logic and invariants. It is NOT anemic.
 * All business rules related to accounts are encapsulated within this class.
 * 
 * Invariants:
 * - Balance cannot go negative
 * - Active accounts can withdraw, inactive accounts cannot
 * - Withdrawal amounts must be positive
 */
public class Account {
    private Long id;
    private String accountNumber;
    private BigDecimal balance;
    private AccountStatus status;

    public Account(Long id, String accountNumber, BigDecimal initialBalance) {
        this(id, accountNumber, initialBalance, AccountStatus.ACTIVE);
    }

    public Account(Long id, String accountNumber, BigDecimal initialBalance, AccountStatus status) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.balance = initialBalance;
        this.status = status;
    }

    // ============ QUERY METHODS (Pure, No Side Effects) ============

    /**
     * Returns the current account balance.
     * @return The current balance
     */
    public BigDecimal checkBalance() {
        return this.balance;
    }

    /**
     * Checks if the account is active.
     * @return true if account is active, false otherwise
     */
    public boolean isActive() {
        return this.status == AccountStatus.ACTIVE;
    }

    /**
     * Gets the account status.
     * @return The account status
     */
    public AccountStatus getStatus() {
        return this.status;
    }

    // ============ COMMAND METHODS (Side Effects, Domain Logic) ============

    /**
     * Withdraw money from the account.
     * 
     * This method enforces all domain invariants:
     * 1. Account must be active
     * 2. Withdrawal amount must be positive
     * 3. Account must have sufficient funds
     * 
     * @param amount The amount to withdraw
     * @throws InactiveAccountException If the account is not active
     * @throws InvalidWithdrawalAmountException If the amount is not positive
     * @throws InsufficientFundsException If there are insufficient funds
     */
    public void withdraw(BigDecimal amount) {
        // Invariant 1: Account must be active
        if (!isActive()) {
            throw new InactiveAccountException(this.id);
        }

        // Invariant 2: Withdrawal amount must be positive
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidWithdrawalAmountException(amount);
        }

        // Invariant 3: Account must have sufficient funds
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(this.id, amount, this.balance);
        }

        // If all invariants are satisfied, perform the withdrawal
        this.balance = this.balance.subtract(amount);
    }

    /**
     * Deposit money into the account.
     * 
     * @param amount The amount to deposit
     * @throws InvalidWithdrawalAmountException If the amount is not positive
     */
    public void deposit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidWithdrawalAmountException(amount);
        }
        this.balance = this.balance.add(amount);
    }

    /**
     * Deactivate the account.
     */
    public void deactivate() {
        this.status = AccountStatus.INACTIVE;
    }

    /**
     * Activate the account.
     */
    public void activate() {
        this.status = AccountStatus.ACTIVE;
    }

    // ============ GETTERS ============

    public Long getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}