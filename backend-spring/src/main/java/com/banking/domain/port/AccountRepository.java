package com.banking.domain.port;

import com.banking.domain.model.Account;
import java.util.Optional;

public interface AccountRepository {
    Optional<Account> findById(Long id);
    Account save(Account account);
}