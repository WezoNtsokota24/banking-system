package com.banking.domain.model;

import java.math.BigDecimal;

/**
 * Rich Domain Model: Transaction
 * 
 * This Transaction aggregate encapsulates transaction logic and state transitions.
 * State transitions are controlled through domain methods (e.g., complete()),
 * not through direct property setters.
 * 
 * Invariants:
 * - Status can only transition through valid paths (PENDING -> COMPLETED)
 * - Transaction properties are immutable after creation
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

    // ============ QUERY METHODS ============

    /**
     * Check if the transaction is in PENDING status.
     * @return true if pending, false otherwise
     */
    public boolean isPending() {
        return this.status == TransactionStatus.PENDING;
    }

    /**
     * Check if the transaction is COMPLETED.
     * @return true if completed, false otherwise
     */
    public boolean isCompleted() {
        return this.status == TransactionStatus.COMPLETED;
    }

    // ============ COMMAND METHODS (State Transitions) ============

    /**
     * Transition transaction from PENDING to COMPLETED.
     * Called by batch processor.
     * 
     * @throws IllegalStateException if transaction is not in PENDING status
     */
    public void complete() {
        if (!isPending()) {
            throw new IllegalStateException(
                String.format("Cannot complete transaction %d: current status is %s, expected PENDING", 
                    this.id, this.status)
            );
        }
        this.status = TransactionStatus.COMPLETED;
    }

    // ============ GETTERS (Immutable Properties) ============

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

    // ============ SETTERS (Kept for JPA/ORM) ============
    // These are package-private to discourage direct manipulation outside of domain methods

    @Deprecated
    void setId(Long id) {
        this.id = id;
    }

    @Deprecated
    void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    @Deprecated
    void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    @Deprecated
    void setType(TransactionType type) {
        this.type = type;
    }

    @Deprecated
    void setStatus(TransactionStatus status) {
        this.status = status;
    }
}
