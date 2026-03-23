package com.banking.adapter.out.persistence;

import com.banking.domain.model.VirtualCard;
import com.banking.domain.model.VirtualCardStatus;
import com.banking.domain.port.VirtualCardPort;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Adapter implementing VirtualCardPort using JPA.
 */
@Component
public class VirtualCardPersistenceAdapter implements VirtualCardPort {

    private final SpringDataVirtualCardRepository repository;

    public VirtualCardPersistenceAdapter(SpringDataVirtualCardRepository repository) {
        this.repository = repository;
    }

    @Override
    public VirtualCard save(VirtualCard virtualCard) {
        VirtualCardEntity entity = toEntity(virtualCard);
        VirtualCardEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<VirtualCard> findByAccountId(Long accountId) {
        return repository.findByAccountId(accountId)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private VirtualCardEntity toEntity(VirtualCard domain) {
        return new VirtualCardEntity(
            domain.getAccountId(),
            domain.getCardNumber(),
            domain.getCvv(),
            domain.getExpirationDate(),
            domain.getStatus().name()
        );
    }

    private VirtualCard toDomain(VirtualCardEntity entity) {
        return new VirtualCard(
            entity.getId(),
            entity.getAccountId(),
            entity.getCardNumber(),
            entity.getCvv(),
            entity.getExpirationDate(),
            VirtualCardStatus.valueOf(entity.getStatus())
        );
    }
}
