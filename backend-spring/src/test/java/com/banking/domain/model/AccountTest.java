package com.banking.domain.model;

import com.banking.domain.exception.InactiveAccountException;
import com.banking.domain.exception.InsufficientFundsException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    @Test
    void shouldReduceBalance_whenSuccessfulWithdrawal() {
        // Arrange
        Account account = new Account(1L, "12345", new BigDecimal("100.00"));
        BigDecimal withdrawalAmount = new BigDecimal("40.00");

        // Act
        account.withdraw(withdrawalAmount);

        // Assert
        assertEquals(new BigDecimal("60.00"), account.checkBalance());
    }

    @Test
    void shouldThrowInsufficientFundsException_whenWithdrawingMoreThanBalance() {
        // Arrange
        Account account = new Account(1L, "12345", new BigDecimal("50.00"));
        BigDecimal withdrawalAmount = new BigDecimal("100.00");

        // Act & Assert
        InsufficientFundsException exception = assertThrows(InsufficientFundsException.class, () -> {
            account.withdraw(withdrawalAmount);
        });

        // Verify the exception details
        assertEquals(1L, exception.getAccountId());
        assertEquals(new BigDecimal("100.00"), exception.getRequestedAmount());
        assertEquals(new BigDecimal("50.00"), exception.getAvailableBalance());
    }

    @Test
    void shouldThrowInactiveAccountException_whenWithdrawingFromInactiveAccount() {
        // Arrange
        Account account = new Account(1L, "12345", new BigDecimal("100.00"), AccountStatus.INACTIVE);
        BigDecimal withdrawalAmount = new BigDecimal("50.00");

        // Act & Assert
        InactiveAccountException exception = assertThrows(InactiveAccountException.class, () -> {
            account.withdraw(withdrawalAmount);
        });

        // Verify the exception details
        assertEquals(1L, exception.getAccountId());
    }
}