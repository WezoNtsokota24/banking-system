package com.banking.adapter.out.persistence;

import com.banking.domain.model.AccountStatus;
import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
public class AccountEntity {

    @Id // This tells MySQL this is the Primary Key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increments the ID (1, 2, 3...)
    private Long id;

    @Column(unique = true, nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private AccountStatus status;

    protected AccountEntity() {}

    public AccountEntity(Long id, String accountNumber, BigDecimal balance) {
        this(id, accountNumber, balance, AccountStatus.ACTIVE);
    }

    public AccountEntity(Long id, String accountNumber, BigDecimal balance, AccountStatus status) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.status = status;
    }

    // Getters and Setters...
    public Long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public BigDecimal getBalance() { return balance; }
    public AccountStatus getStatus() { return status; }
}