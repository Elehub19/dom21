package ru.star.recommendation.dynamic;

import org.springframework.stereotype.Component;
import ru.star.recommendation.repository.TransactionsRepository;

import java.util.List;
import java.util.UUID;

/**
 * ACTIVE_USER_OF: у пользователя не меньше пяти транзакций по продуктам типа X.
 */
@Component
public class ActiveUserOfQueryHandler extends AbstractRuleQueryHandler {

    static final int MIN_TRANSACTIONS = 5;

    private final TransactionsRepository repository;

    public ActiveUserOfQueryHandler(TransactionsRepository repository) {
        this.repository = repository;
    }

    @Override
    public QueryType type() {
        return QueryType.ACTIVE_USER_OF;
    }

    @Override
    public void validate(List<String> arguments) {
        requireArgumentCount(arguments, 1);
        requireProductType(arguments.get(0));
    }

    @Override
    public boolean evaluate(UUID userId, List<String> arguments) {
        return repository.countTransactions(userId, arguments.get(0)) >= MIN_TRANSACTIONS;
    }
}
