package com.banking.domain.exception;

/**
 * Domain exception thrown when a withdrawal amount is invalid (zero or negative).
 * This is a rich domain exception that encapsulates domain-specific business rules.
 */
public class InvalidWithdrawalAmountException extends DomainException {
    private final java.math.BigDecimal amount;

    public InvalidWithdrawalAmountException(java.math.BigDecimal amount) {
        super(String.format("Withdrawal amount must be greater than zero. Received: %s", amount));
        this.amount = amount;
    }

    public java.math.BigDecimal getAmount() {
        return amount;
    }
}

