package ru.star.recommendation.dynamic;

import org.springframework.stereotype.Component;
import ru.star.recommendation.repository.TransactionsRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * TRANSACTION_SUM_COMPARE: сумма транзакций типа Y по продуктам типа X сравнивается с константой C.
 * Аргументы: [тип продукта, тип транзакции, операция сравнения, константа].
 */
@Component
public class TransactionSumCompareQueryHandler extends AbstractRuleQueryHandler {

    private final TransactionsRepository repository;

    public TransactionSumCompareQueryHandler(TransactionsRepository repository) {
        this.repository = repository;
    }

    @Override
    public QueryType type() {
        return QueryType.TRANSACTION_SUM_COMPARE;
    }

    @Override
    public void validate(List<String> arguments) {
        requireArgumentCount(arguments, 4);
        requireProductType(arguments.get(0));
        requireTransactionType(arguments.get(1));
        requireOperator(arguments.get(2));
        requireNonNegativeInt(arguments.get(3));
    }

    @Override
    public boolean evaluate(UUID userId, List<String> arguments) {
        BigDecimal sum = repository.sumAmount(userId, arguments.get(0), arguments.get(1));
        ComparisonOperator operator = requireOperator(arguments.get(2));
        BigDecimal constant = BigDecimal.valueOf(requireNonNegativeInt(arguments.get(3)));
        return operator.test(sum, constant);
    }
}
