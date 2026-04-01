package com.banking.domain.model;

import java.math.BigDecimal;

/**
 * Transaction domain entity.
 * Represents a financial transaction.
 */
public class Transaction {
    private Long id;
    private Long accountId;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionStatus status;

    public Transaction(Long id, Long accountId, BigDecimal amount, TransactionType type, TransactionStatus status) {
        this.id = id;
        this.accountId = accountId;
        this.amount = amount;
        this.type = type;
        this.status = status;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransactionType getType() {
        return type;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    // Method to complete the transaction
    public void complete() {
        this.status = TransactionStatus.COMPLETED;
    }
}
