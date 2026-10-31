package ru.star.recommendation.dynamic;

import java.util.List;
import java.util.UUID;

/**
 * Обработчик одного типа запросов динамических правил.
 * Новый тип запроса = новый QueryType + новая реализация с {@code @Component}.
 */
public interface RuleQueryHandler {

    QueryType type();

    /**
     * Проверяет аргументы запроса.
     *
     * @throws InvalidRuleException если аргументы некорректны
     */
    void validate(List<String> arguments);

    /**
     * Вычисляет запрос (без учёта negate) для пользователя.
     */
    boolean evaluate(UUID userId, List<String> arguments);
}
