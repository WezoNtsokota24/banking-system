package com.banking.adapter.out.persistence;

import com.banking.domain.model.Account;
import com.banking.domain.port.AccountRepository;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component // Tells Spring to build this class automatically
public class AccountPersistenceAdapter implements AccountRepository {

    private final SpringDataAccountRepository springRepository;

    public AccountPersistenceAdapter(SpringDataAccountRepository springRepository) {
        this.springRepository = springRepository;
    }

    @Override
    public Optional<Account> findById(Long id) {
        // 1. Ask Spring to find the database entity
        Optional<AccountEntity> entityOptional = springRepository.findById(id);

        // 2. If found, translate the AccountEntity back into a pure Domain Account
        if (entityOptional.isPresent()) {
            AccountEntity entity = entityOptional.get();
            Account pureAccount = new Account(entity.getId(), entity.getAccountNumber(), entity.getBalance(), entity.getStatus());
            return Optional.of(pureAccount);
        }
        return Optional.empty();
    }

    @Override
    public Account save(Account account) {
        // 1. Translate the pure Domain Account into a Database Entity
        AccountEntity entity = new AccountEntity(account.getId(), account.getAccountNumber(), account.getBalance(), account.getStatus());

        // 2. Save it using Spring's magic repository
        AccountEntity savedEntity = springRepository.save(entity);

        // 3. Translate the saved result back to a pure Domain Account and return it
        return new Account(savedEntity.getId(), savedEntity.getAccountNumber(), savedEntity.getBalance(), savedEntity.getStatus());
    }
}