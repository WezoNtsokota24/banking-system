package com.banking.adapter.in.schedule;

import com.banking.domain.service.TransactionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NightlyTransactionBatch {

    private final TransactionService transactionService;

    public NightlyTransactionBatch(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Scheduled(cron = "0 59 23 * * ?")
    public void runNightlyBatch() {
        transactionService.processNightlyBatch();
    }
}
