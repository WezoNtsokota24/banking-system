package com.banking.domain.model;

import java.math.BigDecimal;

public class Account {
    private Long id;
    private String accountNumber;
    private BigDecimal balance;

    public Account(Long id, String accountNumber, BigDecimal initialBalance) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.balance = initialBalance;
    }

    // Getters
    public Long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public BigDecimal getBalance() { return balance; }

    // Business Logic inside the Domain!
    public void withdraw(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds");
        }
        this.balance = this.balance.subtract(amount);
    }
}