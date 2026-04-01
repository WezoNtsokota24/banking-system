package com.banking.domain.port;

import com.banking.domain.model.VirtualCard;
import java.util.List;

/**
 * Port for VirtualCard repository operations.
 */
public interface VirtualCardPort {

    /**
     * Saves a VirtualCard.
     *
     * @param virtualCard the card to save
     * @return the saved card
     */
    VirtualCard save(VirtualCard virtualCard);

    /**
     * Finds all VirtualCards for a given account ID.
     *
     * @param accountId the account ID
     * @return list of virtual cards
     */
    List<VirtualCard> findByAccountId(Long accountId);
}
