package com.banking;

import com.banking.adapter.out.persistence.AccountEntity;
import com.banking.adapter.out.persistence.SpringDataAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.math.BigDecimal;

@SpringBootApplication
@EnableScheduling
public class BankingApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankingApplication.class, args);
    }

    // This creates our dummy account when the server starts!
    @Bean
    CommandLineRunner initDatabase(SpringDataAccountRepository repository) {
        return args -> {
            AccountEntity testAccount = new AccountEntity(null, "999888777", new BigDecimal("500.00"));
            repository.save(testAccount);
            System.out.println(" Test account created with ID: " + testAccount.getId() + " and Balance: $500.00");
        };
    }
}