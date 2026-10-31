package ru.star.recommendation.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionsRepositoryTest {

    private final UUID userId = UUID.randomUUID();

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private TransactionsRepository repository;

    @Test
    void countIsCachedPerUserAndProductType() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(userId), eq("DEBIT")))
                .thenReturn(7L);

        assertEquals(7L, repository.countTransactions(userId, "DEBIT"));
        assertEquals(7L, repository.countTransactions(userId, "DEBIT"));

        verify(jdbcTemplate, times(1))
                .queryForObject(anyString(), eq(Long.class), eq(userId), eq("DEBIT"));
    }

    @Test
    void countForDifferentKeyHitsDatabaseAgain() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(userId), eq("DEBIT")))
                .thenReturn(7L);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(userId), eq("CREDIT")))
                .thenReturn(0L);

        assertEquals(7L, repository.countTransactions(userId, "DEBIT"));
        assertEquals(0L, repository.countTransactions(userId, "CREDIT"));

        verify(jdbcTemplate, times(1))
                .queryForObject(anyString(), eq(Long.class), eq(userId), eq("DEBIT"));
        verify(jdbcTemplate, times(1))
                .queryForObject(anyString(), eq(Long.class), eq(userId), eq("CREDIT"));
    }

    @Test
    void sumIsCachedPerAllParameters() {
        when(jdbcTemplate.queryForObject(anyString(), eq(BigDecimal.class), eq(userId), eq("DEBIT"), eq("DEPOSIT")))
                .thenReturn(BigDecimal.valueOf(500));
        when(jdbcTemplate.queryForObject(anyString(), eq(BigDecimal.class), eq(userId), eq("DEBIT"), eq("WITHDRAW")))
                .thenReturn(BigDecimal.valueOf(200));

        assertEquals(BigDecimal.valueOf(500), repository.sumAmount(userId, "DEBIT", "DEPOSIT"));
        assertEquals(BigDecimal.valueOf(500), repository.sumAmount(userId, "DEBIT", "DEPOSIT"));
        assertEquals(BigDecimal.valueOf(200), repository.sumAmount(userId, "DEBIT", "WITHDRAW"));

        verify(jdbcTemplate, times(1))
                .queryForObject(anyString(), eq(BigDecimal.class), eq(userId), eq("DEBIT"), eq("DEPOSIT"));
        verify(jdbcTemplate, times(1))
                .queryForObject(anyString(), eq(BigDecimal.class), eq(userId), eq("DEBIT"), eq("WITHDRAW"));
    }

    @Test
    void clearCachesForcesNewDatabaseQueries() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(userId), eq("DEBIT")))
                .thenReturn(7L);
        when(jdbcTemplate.queryForObject(anyString(), eq(BigDecimal.class), eq(userId), eq("DEBIT"), eq("DEPOSIT")))
                .thenReturn(BigDecimal.TEN);

        repository.countTransactions(userId, "DEBIT");
        repository.sumAmount(userId, "DEBIT", "DEPOSIT");
        repository.clearCaches();
        repository.countTransactions(userId, "DEBIT");
        repository.sumAmount(userId, "DEBIT", "DEPOSIT");

        verify(jdbcTemplate, times(2))
                .queryForObject(anyString(), eq(Long.class), eq(userId), eq("DEBIT"));
        verify(jdbcTemplate, times(2))
                .queryForObject(anyString(), eq(BigDecimal.class), eq(userId), eq("DEBIT"), eq("DEPOSIT"));
    }

    @Test
    void usesProductTypeDependsOnTransactionCount() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(userId), eq("DEBIT")))
                .thenReturn(1L);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(userId), eq("INVEST")))
                .thenReturn(0L);

        assertTrue(repository.usesProductType(userId, "DEBIT"));
        assertFalse(repository.usesProductType(userId, "INVEST"));
    }
}
