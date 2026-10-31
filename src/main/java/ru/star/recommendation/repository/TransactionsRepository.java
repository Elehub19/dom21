package ru.star.recommendation.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Запросы к базе знаний о клиентах (H2, только чтение).
 * <p>
 * Данные в базе не меняются, поэтому результаты кешируются (Caffeine, по одному кешу на метод).
 * Ключ кеша — все параметры запроса.
 */
@Repository
public class TransactionsRepository {

    private static final long MAX_CACHE_SIZE = 10_000;

    private static final String COUNT_SQL = """
            SELECT COUNT(*)
            FROM transactions t
            JOIN products p ON t.product_id = p.id
            WHERE t.user_id = ? AND p.type = ?
            """;

    private static final String SUM_SQL = """
            SELECT COALESCE(SUM(t.amount), 0)
            FROM transactions t
            JOIN products p ON t.product_id = p.id
            WHERE t.user_id = ? AND p.type = ? AND t.type = ?
            """;

    record CountKey(UUID userId, String productType) {
    }

    record SumKey(UUID userId, String productType, String transactionType) {
    }

    private final JdbcTemplate jdbcTemplate;

    private final Cache<CountKey, Long> countCache = Caffeine.newBuilder()
            .maximumSize(MAX_CACHE_SIZE)
            .build();

    private final Cache<SumKey, BigDecimal> sumCache = Caffeine.newBuilder()
            .maximumSize(MAX_CACHE_SIZE)
            .build();

    public TransactionsRepository(@Qualifier("recommendationsJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Количество транзакций пользователя по всем продуктам типа productType.
     */
    public long countTransactions(UUID userId, String productType) {
        return countCache.get(new CountKey(userId, productType), key -> {
            Long count = jdbcTemplate.queryForObject(COUNT_SQL, Long.class, key.userId(), key.productType());
            return count == null ? 0L : count;
        });
    }

    /**
     * Сумма транзакций типа transactionType по всем продуктам типа productType пользователя.
     */
    public BigDecimal sumAmount(UUID userId, String productType, String transactionType) {
        return sumCache.get(new SumKey(userId, productType, transactionType), key -> {
            BigDecimal sum = jdbcTemplate.queryForObject(SUM_SQL, BigDecimal.class,
                    key.userId(), key.productType(), key.transactionType());
            return sum == null ? BigDecimal.ZERO : sum;
        });
    }

    /**
     * Сбрасывает все закешированные результаты: следующие запросы снова обратятся к базе.
     */
    public void clearCaches() {
        countCache.invalidateAll();
        sumCache.invalidateAll();
    }

    /**
     * Пользователь использует продукт типа productType, если по нему есть хотя бы одна транзакция.
     */
    public boolean usesProductType(UUID userId, String productType) {
        return countTransactions(userId, productType) > 0;
    }
}
