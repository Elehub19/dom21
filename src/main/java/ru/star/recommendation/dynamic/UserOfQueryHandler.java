package ru.star.recommendation.dynamic;

import org.springframework.stereotype.Component;
import ru.star.recommendation.repository.TransactionsRepository;

import java.util.List;
import java.util.UUID;

/**
 * USER_OF: у пользователя есть хотя бы одна транзакция по продуктам типа X.
 */
@Component
public class UserOfQueryHandler extends AbstractRuleQueryHandler {

    private final TransactionsRepository repository;

    public UserOfQueryHandler(TransactionsRepository repository) {
        this.repository = repository;
    }

    @Override
    public QueryType type() {
        return QueryType.USER_OF;
    }

    @Override
    public void validate(List<String> arguments) {
        requireArgumentCount(arguments, 1);
        requireProductType(arguments.get(0));
    }

    @Override
    public boolean evaluate(UUID userId, List<String> arguments) {
        return repository.usesProductType(userId, arguments.get(0));
    }
}
