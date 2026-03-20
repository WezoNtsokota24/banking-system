package com.banking.domain.port;

import com.banking.domain.model.Transaction;
import com.banking.domain.model.TransactionStatus;
import java.util.List;

public interface TransactionRepository {
    Transaction save(Transaction transaction);
    List<Transaction> findByStatus(TransactionStatus status);
}
