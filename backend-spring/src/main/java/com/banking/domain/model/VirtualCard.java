package com.banking.domain.model;

import java.time.LocalDate;

/**
 * VirtualCard domain entity.
 * Represents a virtual card associated with an account.
 */
public class VirtualCard {
    private Long id;
    private Long accountId;
    private String cardNumber;
    private String cvv;
    private LocalDate expirationDate;
    private VirtualCardStatus status;

    public VirtualCard(Long id, Long accountId, String cardNumber, String cvv, LocalDate expirationDate, VirtualCardStatus status) {
        this.id = id;
        this.accountId = accountId;
        this.cardNumber = cardNumber;
        this.cvv = cvv;
        this.expirationDate = expirationDate;
        this.status = status;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public String getCvv() {
        return cvv;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public VirtualCardStatus getStatus() {
        return status;
    }
}
