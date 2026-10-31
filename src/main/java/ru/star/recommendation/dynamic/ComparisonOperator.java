package ru.star.recommendation.dynamic;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

/**
 * Операции сравнения: {@code >}, {@code <}, {@code =}, {@code >=}, {@code <=}.
 */
public enum ComparisonOperator {
    GREATER(">"),
    LESS("<"),
    EQUAL("="),
    GREATER_OR_EQUAL(">="),
    LESS_OR_EQUAL("<=");

    private final String symbol;

    ComparisonOperator(String symbol) {
        this.symbol = symbol;
    }

    public boolean test(BigDecimal left, BigDecimal right) {
        int result = left.compareTo(right);
        return switch (this) {
            case GREATER -> result > 0;
            case LESS -> result < 0;
            case EQUAL -> result == 0;
            case GREATER_OR_EQUAL -> result >= 0;
            case LESS_OR_EQUAL -> result <= 0;
        };
    }

    public static Optional<ComparisonOperator> fromSymbol(String symbol) {
        return Arrays.stream(values())
                .filter(operator -> operator.symbol.equals(symbol))
                .findFirst();
    }
}
