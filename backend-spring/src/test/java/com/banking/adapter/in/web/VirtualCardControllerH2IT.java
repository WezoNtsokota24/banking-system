package com.banking.adapter.in.web;

import com.banking.adapter.out.persistence.AccountEntity;
import com.banking.adapter.out.persistence.SpringDataAccountRepository;
import com.banking.domain.model.Account;
import com.banking.domain.model.AccountStatus;
import com.banking.domain.model.VirtualCard;
import com.banking.domain.model.VirtualCardStatus;
import com.banking.domain.port.AccountRepository;
import com.banking.domain.port.JwtPort;
import com.banking.domain.port.VirtualCardPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test for VirtualCardController using H2 In-Memory Database
 *
 * This test suite validates:
 * 1. Successful virtual card creation - Card is saved and returned
 * 2. Card creation for inactive account - Still succeeds
 * 3. Retrieving cards for account - Returns list of cards
 * 4. Retrieving cards for account with no cards - Returns empty list
 * 5. Creating multiple cards - Succeeds
 *
 * This uses H2 in-memory database and can run without Docker.
 * The "local" profile is activated to use H2 instead of MySQL.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class VirtualCardControllerH2IT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private SpringDataAccountRepository springDataAccountRepository;

    @Autowired
    private VirtualCardPort virtualCardPort;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtPort jwtPort;

    private String jwtToken;

    private Account activeAccount;
    private Account inactiveAccount;

    @BeforeEach
    void setUp() {
        // Generate JWT token for authentication
        jwtToken = jwtPort.generateToken("testuser");

        // Create an ACTIVE account - use saveAndFlush to ensure ID is assigned
        AccountEntity activeEntity = springDataAccountRepository.saveAndFlush(
                new AccountEntity(null, "ACC-ACTIVE-001", new BigDecimal("1000.00"), AccountStatus.ACTIVE));
        activeAccount = accountRepository.findById(activeEntity.getId()).orElseThrow(() -> new RuntimeException("Account not found"));

        // Create an INACTIVE account - use saveAndFlush to ensure ID is assigned
        AccountEntity inactiveEntity = springDataAccountRepository.saveAndFlush(
                new AccountEntity(null, "ACC-INACTIVE-002", new BigDecimal("500.00"), AccountStatus.INACTIVE));
        inactiveAccount = accountRepository.findById(inactiveEntity.getId()).orElseThrow(() -> new RuntimeException("Account not found"));
    }

    @Test
    @DisplayName("Create virtual card should return 200 OK and save card to database")
    void testCreateVirtualCard_shouldReturn200AndSaveToDatabase() throws Exception {
        // Arrange
        Long accountId = activeAccount.getId();

        // Act: Send POST request to create virtual card
        MvcResult result = mockMvc.perform(post("/api/accounts/" + accountId + "/cards")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Assert: Parse the response
        VirtualCard createdCard = objectMapper.readValue(result.getResponse().getContentAsString(), VirtualCard.class);
        assertNotNull(createdCard, "Created card should not be null");
        assertNotNull(createdCard.getId(), "Card should have an ID");
        assertEquals(accountId, createdCard.getAccountId(), "Card should be linked to the correct account");
        assertNotNull(createdCard.getCardNumber(), "Card should have a card number");
        assertNotNull(createdCard.getExpirationDate(), "Card should have an expiry date");
        assertNotNull(createdCard.getCvv(), "Card should have a CVV");
        assertEquals("ACTIVE", createdCard.getStatus().name(), "Card should be ACTIVE by default");

        // Assert: Verify card is saved in database
        List<VirtualCard> cardsInDb = virtualCardPort.findByAccountId(accountId);
        assertFalse(cardsInDb.isEmpty(), "At least one card should exist in database for the account");

        VirtualCard dbCard = cardsInDb.stream()
                .filter(c -> c.getId().equals(createdCard.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Card not found in database"));

        assertEquals(createdCard.getCardNumber(), dbCard.getCardNumber(), "Card number should match");
        assertEquals(createdCard.getAccountId(), dbCard.getAccountId(), "Account ID should match");
        assertEquals(createdCard.getStatus(), dbCard.getStatus(), "Status should match");
    }

    @Test
    @DisplayName("Create virtual card for inactive account should still succeed")
    void testCreateVirtualCard_forInactiveAccount_shouldSucceed() throws Exception {
        // Arrange
        Long accountId = inactiveAccount.getId();

        // Act: Send POST request to create virtual card
        MvcResult result = mockMvc.perform(post("/api/accounts/" + accountId + "/cards")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Assert: Card is created and saved
        VirtualCard createdCard = objectMapper.readValue(result.getResponse().getContentAsString(), VirtualCard.class);
        assertNotNull(createdCard.getId(), "Card should be created even for inactive account");

        // Verify in database
        List<VirtualCard> cardsInDb = virtualCardPort.findByAccountId(accountId);
        assertFalse(cardsInDb.isEmpty(), "Card should be saved to database");
    }

    @Test
    @DisplayName("Get virtual cards should return list of cards for account")
    void testGetVirtualCards_shouldReturnCardsForAccount() throws Exception {
        // Arrange: Create a couple of cards
        Long accountId = activeAccount.getId();
        VirtualCard card1 = virtualCardPort.save(new VirtualCard(null, accountId, "4111111111111111", "123", LocalDate.of(2025, 12, 1), VirtualCardStatus.ACTIVE));
        VirtualCard card2 = virtualCardPort.save(new VirtualCard(null, accountId, "4222222222222222", "456", LocalDate.of(2026, 1, 1), VirtualCardStatus.ACTIVE));

        // Act: Send GET request to retrieve cards
        MvcResult result = mockMvc.perform(get("/api/accounts/" + accountId + "/cards")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Assert: Parse the response
        List<VirtualCard> cards = objectMapper.readValue(result.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, VirtualCard.class));

        assertEquals(2, cards.size(), "Should return 2 cards");
        assertTrue(cards.stream().anyMatch(c -> c.getCardNumber().equals("4111111111111111")), "Should contain first card");
        assertTrue(cards.stream().anyMatch(c -> c.getCardNumber().equals("4222222222222222")), "Should contain second card");
    }

    @Test
    @DisplayName("Get virtual cards for account with no cards should return empty list")
    void testGetVirtualCards_forAccountWithNoCards_shouldReturnEmptyList() throws Exception {
        // Arrange
        Long accountId = activeAccount.getId();

        // Act: Send GET request
        MvcResult result = mockMvc.perform(get("/api/accounts/" + accountId + "/cards")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Assert: Should return empty list
        List<VirtualCard> cards = objectMapper.readValue(result.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, VirtualCard.class));

        assertTrue(cards.isEmpty(), "Should return empty list for account with no cards");
    }

    @Test
    @DisplayName("Create multiple virtual cards for same account should succeed")
    void testCreateMultipleVirtualCards_shouldSucceed() throws Exception {
        // Arrange
        Long accountId = activeAccount.getId();

        // Act: Create first card
        mockMvc.perform(post("/api/accounts/" + accountId + "/cards")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());

        // Act: Create second card
        mockMvc.perform(post("/api/accounts/" + accountId + "/cards")
                .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());

        // Assert: Both cards should exist in database
        List<VirtualCard> cardsInDb = virtualCardPort.findByAccountId(accountId);
        assertEquals(2, cardsInDb.size(), "Should have 2 cards in database");
    }
}
