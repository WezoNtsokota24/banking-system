package com.banking.domain.exception;

/**
 * Domain exception thrown when an account lacks sufficient funds for a withdrawal.
 * This is a rich domain exception that encapsulates domain-specific business rules.
 */
public class InsufficientFundsException extends DomainException {
    private final Long accountId;
    private final java.math.BigDecimal requestedAmount;
    private final java.math.BigDecimal availableBalance;

    public InsufficientFundsException(Long accountId, java.math.BigDecimal requestedAmount, java.math.BigDecimal availableBalance) {
        super(String.format(
            "Insufficient funds for account %d. Requested: %s, Available: %s",
            accountId, requestedAmount, availableBalance
        ));
        this.accountId = accountId;
        this.requestedAmount = requestedAmount;
        this.availableBalance = availableBalance;
    }

    public Long getAccountId() {
        return accountId;
    }

    public java.math.BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public java.math.BigDecimal getAvailableBalance() {
        return availableBalance;
    }
}

