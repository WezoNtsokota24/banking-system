package com.banking.adapter.in.web;

import com.banking.domain.model.Account;
import com.banking.domain.port.AccountRepository;
import com.banking.domain.service.WithdrawalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Optional;

@RestController
@RequestMapping("/api/accounts")
public class WithdrawalController {

    private final WithdrawalService withdrawalService;
    private final AccountRepository accountRepository; // Added so we can check the balance

    public WithdrawalController(WithdrawalService withdrawalService, AccountRepository accountRepository) {
        this.withdrawalService = withdrawalService;
        this.accountRepository = accountRepository;
    }

    @PostMapping("/{accountId}/withdraw")
    public void withdraw(@PathVariable Long accountId, @RequestBody WithdrawalRequest request) {

        withdrawalService.withdrawMoney(accountId, request.getAmount());
    }

    //check balance
    @GetMapping("/{accountId}")
    public ResponseEntity<Account> getAccount(@PathVariable Long accountId) {
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