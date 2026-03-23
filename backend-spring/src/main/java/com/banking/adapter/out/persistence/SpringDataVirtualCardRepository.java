package com.banking.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Spring Data JPA repository for VirtualCardEntity.
 */
public interface SpringDataVirtualCardRepository extends JpaRepository<VirtualCardEntity, Long> {

    List<VirtualCardEntity> findByAccountId(Long accountId);
}
