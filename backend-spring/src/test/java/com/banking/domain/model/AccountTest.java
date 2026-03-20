package com.banking.domain.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    @Test
    void happyPath_shouldDecreaseBalance_whenFundsAreSufficient() {
        // Arrange: Set up the starting state
        Account account = new Account(1L, "12345", new BigDecimal("100.00"));
        BigDecimal withdrawalAmount = new BigDecimal("40.00");

        // Act: Perform the action
        account.withdraw(withdrawalAmount);

        // Assert: Verify the result
        assertEquals(new BigDecimal("60.00"), account.getBalance());
    }

    @Test
    void sadPath_shouldThrowException_whenFundsAreInsufficient() {
        // Arrange
        Account account = new Account(1L, "12345", new BigDecimal("50.00"));
        BigDecimal massiveWithdrawal = new BigDecimal("500.00");

        // Act & Assert: Check that the exact exception is thrown
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            account.withdraw(massiveWithdrawal);
        });

        // We can even check the error message!
        assertEquals("Insufficient funds", exception.getMessage());
    }

    @Test
    void sadPath_shouldThrowException_whenAmountIsNegativeOrZero() {
        // Arrange
        Account account = new Account(1L, "12345", new BigDecimal("100.00"));
        BigDecimal negativeAmount = new BigDecimal("-50.00");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            account.withdraw(negativeAmount);
        });

        assertEquals("Withdrawal amount must be greater than zero", exception.getMessage());
    }
}