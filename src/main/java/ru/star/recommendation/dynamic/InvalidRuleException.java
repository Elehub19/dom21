package ru.star.recommendation.dynamic;

/**
 * Динамическое правило или его запрос описаны некорректно.
 */
public class InvalidRuleException extends RuntimeException {

    public InvalidRuleException(String message) {
        super(message);
    }
}
