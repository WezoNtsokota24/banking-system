package com.banking.adapter.config;

import com.banking.domain.port.AccountRepository;
import com.banking.domain.port.JwtPort;
import com.banking.domain.port.PasswordEncoderPort;
import com.banking.domain.port.TransactionRepository;
import com.banking.domain.port.UserRepository;
import com.banking.domain.service.AuthService;
import com.banking.domain.service.TransactionService;
import com.banking.domain.service.WithdrawalService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    public WithdrawalService withdrawalService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        return new WithdrawalService(accountRepository, transactionRepository);
    }

    @Bean
    public TransactionService transactionService(TransactionRepository transactionRepository) {
        return new TransactionService(transactionRepository);
    }

    @Bean
    public AuthService authService(UserRepository userRepository, PasswordEncoderPort passwordEncoder, JwtPort jwtPort) {
        return new AuthService(userRepository, passwordEncoder, jwtPort);
    }
}
