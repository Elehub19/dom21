package ru.star.recommendation.dynamic;

import java.util.Arrays;
import java.util.Optional;

/**
 * Поддерживаемые типы запросов динамических правил.
 */
public enum QueryType {
    USER_OF,
    ACTIVE_USER_OF,
    TRANSACTION_SUM_COMPARE,
    TRANSACTION_SUM_COMPARE_DEPOSIT_WITHDRAW;

    public static Optional<QueryType> fromName(String name) {
        return Arrays.stream(values())
                .filter(type -> type.name().equals(name))
                .findFirst();
    }
}
