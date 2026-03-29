package com.banking.domain.service;

import com.banking.domain.model.VirtualCard;
import com.banking.domain.model.VirtualCardStatus;
import com.banking.domain.port.VirtualCardPort;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

/**
 * VirtualCardService: Domain service for virtual card operations.
 * Handles generating new cards and retrieving existing ones.
 */
@Service
public class VirtualCardService {

    private final VirtualCardPort virtualCardPort;
    private final Random random = new Random();

    public VirtualCardService(VirtualCardPort virtualCardPort) {
        this.virtualCardPort = virtualCardPort;
    }

    /**
     * Generates a new virtual card for the given account ID.
     *
     * @param accountId the account ID
     * @return the generated virtual card
     */
    public VirtualCard generateCard(Long accountId) {
        String cardNumber = generateCardNumber();
        String cvv = generateCvv();
        LocalDate expirationDate = LocalDate.now().plusYears(5); // 5 years from now

        VirtualCard virtualCard = new VirtualCard(
            null, // ID will be set by persistence
            accountId,
            cardNumber,
            cvv,
            expirationDate,
            VirtualCardStatus.ACTIVE
        );

        return virtualCardPort.save(virtualCard);
    }

    /**
     * Retrieves all virtual cards for the given account ID.
     *
     * @param accountId the account ID
     * @return list of virtual cards
     */
    public List<VirtualCard> getCards(Long accountId) {
        return virtualCardPort.findByAccountId(accountId);
    }

    /**
     * Generates a random 16-digit card number.
     *
     * @return 16-digit string
     */
    private String generateCardNumber() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * Generates a random 3-digit CVV.
     *
     * @return 3-digit string
     */
    private String generateCvv() {
        return String.format("%03d", random.nextInt(1000));
    }
}
