package com.banking.adapter.in.web;

import com.banking.domain.model.VirtualCard;
import com.banking.domain.service.VirtualCardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * REST controller for virtual card operations.
 */
@RestController
@RequestMapping("/api/accounts")
public class VirtualCardController {

    private final VirtualCardService virtualCardService;

    public VirtualCardController(VirtualCardService virtualCardService) {
        this.virtualCardService = virtualCardService;
    }

    @PostMapping("/{accountId}/cards")
    public ResponseEntity<VirtualCard> generateCard(@PathVariable Long accountId) {
        VirtualCard card = virtualCardService.generateCard(accountId);
        return ResponseEntity.ok(card);
    }

    @GetMapping("/{accountId}/cards")
    public ResponseEntity<List<VirtualCard>> getCards(@PathVariable Long accountId) {
        List<VirtualCard> cards = virtualCardService.getCards(accountId);
        return ResponseEntity.ok(cards);
    }
}
