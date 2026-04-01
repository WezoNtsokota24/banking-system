package com.banking.domain.exception;

/**
 * Domain exception thrown when an operation is attempted on an inactive account.
 * This is a rich domain exception that encapsulates domain-specific business rules.
 */
public class InactiveAccountException extends DomainException {
    private final Long accountId;

    public InactiveAccountException(Long accountId) {
        super(String.format("Account %d is inactive and cannot perform this operation", accountId));
        this.accountId = accountId;
    }

    public Long getAccountId() {
        return accountId;
    }
}

