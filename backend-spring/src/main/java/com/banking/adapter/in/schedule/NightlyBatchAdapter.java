package com.banking.adapter.in.schedule;

import com.banking.domain.service.BatchUseCase;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * NightlyBatchAdapter: Scheduled adapter for processing pending transactions.
 * Runs every 60 seconds to finalize pending transactions.
 */
@Component
@EnableScheduling
public class NightlyBatchAdapter {

    private final BatchUseCase batchUseCase;

    public NightlyBatchAdapter(BatchUseCase batchUseCase) {
        this.batchUseCase = batchUseCase;
    }

    /**
     * Scheduled method to process pending transactions.
     * Runs every 60 seconds (60000 milliseconds).
     */
    @Scheduled(fixedRate = 60000)
    public void processPendingTransactions() {
        batchUseCase.processPendingTransactions();
    }
}
