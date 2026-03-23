package com.banking.adapter.out.persistence;

import com.banking.domain.model.Transaction;
import com.banking.domain.model.TransactionStatus;
import com.banking.domain.port.TransactionRepository;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TransactionPersistenceAdapter implements TransactionRepository {

    private final SpringDataTransactionRepository springRepository;

    public TransactionPersistenceAdapter(SpringDataTransactionRepository springRepository) {
        this.springRepository = springRepository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionEntity entity = new TransactionEntity(
            transaction.getId(),
            transaction.getAccountId(),
            transaction.getAmount(),
            transaction.getStatus()
        );

        TransactionEntity savedEntity = springRepository.save(entity);

        return new Transaction(
            savedEntity.getId(),
            savedEntity.getAccountId(),
            savedEntity.getAmount(),
            savedEntity.getStatus()
        );
    }

    @Override
    public List<Transaction> findByStatus(TransactionStatus status) {
        return springRepository.findByStatus(status)
            .stream()
            .map(entity -> new Transaction(
                entity.getId(),
                entity.getAccountId(),
                entity.getAmount(),
                entity.getStatus()
            ))
            .collect(Collectors.toList());
    }
}
