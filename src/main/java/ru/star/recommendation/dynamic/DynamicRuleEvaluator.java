package ru.star.recommendation.dynamic;

import org.springframework.stereotype.Component;
import ru.star.recommendation.dto.RuleQueryDto;
import ru.star.recommendation.entity.RuleEntity;
import ru.star.recommendation.entity.RuleQueryEntity;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Проверяет и вычисляет динамические правила. Правило выполняется,
 * если все его запросы (с учётом negate) вернули true.
 */
@Component
public class DynamicRuleEvaluator {

    private final Map<QueryType, RuleQueryHandler> handlers = new EnumMap<>(QueryType.class);

    public DynamicRuleEvaluator(List<RuleQueryHandler> handlerList) {
        for (RuleQueryHandler handler : handlerList) {
            handlers.put(handler.type(), handler);
        }
    }

    /**
     * Проверяет, что запрос известного типа и с корректными аргументами.
     */
    public void validate(RuleQueryDto query) {
        handlerFor(query.query()).validate(query.arguments());
    }

    /**
     * @throws InvalidRuleException если в сохранённом правиле встретился некорректный запрос
     */
    public boolean matches(RuleEntity rule, UUID userId) {
        return rule.getQueries().stream().allMatch(query -> evaluate(query, userId));
    }

    private boolean evaluate(RuleQueryEntity query, UUID userId) {
        RuleQueryHandler handler = handlerFor(query.getQueryType());
        handler.validate(query.getArguments());
        boolean result = handler.evaluate(userId, query.getArguments());
        return query.isNegate() ? !result : result;
    }

    private RuleQueryHandler handlerFor(String queryName) {
        QueryType type = QueryType.fromName(queryName)
                .orElseThrow(() -> new InvalidRuleException("Неизвестный тип запроса: " + queryName));
        RuleQueryHandler handler = handlers.get(type);
        if (handler == null) {
            throw new InvalidRuleException("Нет обработчика для типа запроса: " + queryName);
        }
        return handler;
    }
}
