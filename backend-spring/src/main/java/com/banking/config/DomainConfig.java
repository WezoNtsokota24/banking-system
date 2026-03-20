package com.banking.config;

import com.banking.domain.port.AccountRepository;
import com.banking.domain.port.TransactionRepository;
import com.banking.domain.service.WithdrawalService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    // This tells Spring: "When someone needs a WithdrawalService"
    @Bean
    public WithdrawalService withdrawalService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        return new WithdrawalService(accountRepository, transactionRepository);
    }
}