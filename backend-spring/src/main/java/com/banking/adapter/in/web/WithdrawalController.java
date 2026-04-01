package com.banking.adapter.in.web;

import com.banking.domain.model.Account;
import com.banking.domain.port.AccountRepository;
import com.banking.domain.service.WithdrawalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Optional;

@RestController
@RequestMapping("/api/accounts")
public class WithdrawalController {

    private static final Logger logger = LoggerFactory.getLogger(WithdrawalController.class);
    private final WithdrawalService withdrawalService;
    private final AccountRepository accountRepository; // Added so we can check the balance

    public WithdrawalController(WithdrawalService withdrawalService, AccountRepository accountRepository) {
        this.withdrawalService = withdrawalService;
        this.accountRepository = accountRepository;
    }

    @PostMapping("/{accountId}/withdraw")
    public ResponseEntity<Void> withdraw(@PathVariable Long accountId, @RequestBody WithdrawalRequest request) {
        logger.info("Received withdrawal request for account {}: {}", accountId, request.getAmount());
        withdrawalService.withdrawMoney(accountId, request.getAmount());
        return ResponseEntity.ok().build();
    }

    //check balance
    @GetMapping("/{accountId}")
    public ResponseEntity<Account> getAccount(@PathVariable Long accountId) {
        logger.info("Received request for account details: {}", accountId);
        Optional<Account> account = accountRepository.findById(accountId);
        return account.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}


class WithdrawalRequest {
    private BigDecimal amount;

    public WithdrawalRequest() {}

    public WithdrawalRequest(BigDecimal amount) {
        this.amount = amount;
    }

    // Jackson (Spring's JSON parser) specifically looks for the word "get"
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}