package com.banking.domain.exception;

/**
 * Base domain exception for all domain-driven design exceptions.
 * This represents violations of domain invariants and business rules.
 */
public abstract class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}

