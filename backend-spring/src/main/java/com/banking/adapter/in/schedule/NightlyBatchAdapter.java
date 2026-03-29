package com.banking.adapter.in.schedule;

import com.banking.domain.service.PendingTransactionFinalizationUseCase;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * NightlyBatchAdapter: Scheduled adapter for processing pending transactions.
 * Runs every night at midnight (00:00:00) to finalize pending transactions.
 */
@Component
@EnableScheduling
public class NightlyBatchAdapter {

    private final PendingTransactionFinalizationUseCase pendingTransactionFinalizationUseCase;

    public NightlyBatchAdapter(PendingTransactionFinalizationUseCase pendingTransactionFinalizationUseCase) {
        this.pendingTransactionFinalizationUseCase = pendingTransactionFinalizationUseCase;
    }

    /**
     * Scheduled method to process pending transactions.
     * Runs daily at midnight (00:00:00).
     * Cron expression: 0 0 0 * * ?
     *   - 0: Second (0)
     *   - 0: Minute (0)
     *   - 0: Hour (midnight)
     *   - *: Day of Month (any)
     *   - *: Month (any)
     *   - ?: Day of Week (ignored)
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void processPendingTransactions() {
        pendingTransactionFinalizationUseCase.processPendingTransactions();
    }
}
