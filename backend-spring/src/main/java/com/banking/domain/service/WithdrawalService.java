package com.banking.domain.service;

import com.banking.domain.model.Account;
import com.banking.domain.model.Transaction;
import com.banking.domain.model.TransactionStatus;
import com.banking.domain.model.TransactionType;
import com.banking.domain.port.AccountRepository;
import com.banking.domain.port.TransactionRepository;
import java.math.BigDecimal;

public class WithdrawalService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public WithdrawalService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    public void withdrawMoney(Long accountId, BigDecimal amount) {
        // Step 1: Find the account (or throw an error if it doesn't exist)
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        // Step 2: Tell the account to withdraw the amount
        account.withdraw(amount);

        // Step 3: Save the updated account
        accountRepository.save(account);

        // Step 4: Record the transaction
        Transaction transaction = new Transaction(null, accountId, amount, TransactionType.WITHDRAWAL, TransactionStatus.PENDING);
        transactionRepository.save(transaction);
    }
}