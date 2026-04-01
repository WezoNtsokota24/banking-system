package com.banking.domain.service;

import com.banking.domain.exception.InsufficientFundsException;
import com.banking.domain.model.Account;
import com.banking.domain.model.Transaction;
import com.banking.domain.model.TransactionStatus;
import com.banking.domain.port.AccountRepository;
import com.banking.domain.port.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WithdrawalServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private Account account;

    private WithdrawalService withdrawalService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        withdrawalService = new WithdrawalService(accountRepository, transactionRepository);
    }

    @Test
    void shouldWithdrawSuccessfully_whenAccountExistsAndHasFunds() {
        // Arrange
        Long accountId = 1L;
        BigDecimal amount = new BigDecimal("50.00");

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        doNothing().when(account).withdraw(amount);
        when(accountRepository.save(account)).thenReturn(account);

        // Act
        withdrawalService.withdrawMoney(accountId, amount);

        // Assert
        verify(accountRepository).findById(accountId);
        verify(account).withdraw(amount);
        verify(accountRepository).save(account);

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());

        Transaction savedTransaction = transactionCaptor.getValue();
        assertEquals(accountId, savedTransaction.getAccountId());
        assertEquals(amount, savedTransaction.getAmount());
        assertEquals(TransactionStatus.PENDING, savedTransaction.getStatus());
    }

    @Test
    void shouldThrowIllegalArgumentException_whenAccountNotFound() {
        // Arrange
        Long accountId = 1L;
        BigDecimal amount = new BigDecimal("50.00");

        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            withdrawalService.withdrawMoney(accountId, amount);
        });

        assertEquals("Account not found", exception.getMessage());
        verify(accountRepository).findById(accountId);
        verifyNoInteractions(account);
        verifyNoMoreInteractions(accountRepository);
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void shouldPropagateDomainExceptions_whenWithdrawFails() {
        // Arrange
        Long accountId = 1L;
        BigDecimal amount = new BigDecimal("100.00");

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        doThrow(new InsufficientFundsException(accountId, amount, new BigDecimal("50.00")))
                .when(account).withdraw(amount);

        // Act & Assert
        InsufficientFundsException exception = assertThrows(InsufficientFundsException.class, () -> {
            withdrawalService.withdrawMoney(accountId, amount);
        });

        verify(accountRepository).findById(accountId);
        verify(account).withdraw(amount);
        verifyNoMoreInteractions(accountRepository);
        verifyNoInteractions(transactionRepository);
    }
}
