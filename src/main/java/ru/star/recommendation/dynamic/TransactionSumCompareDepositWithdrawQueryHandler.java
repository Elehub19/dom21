package ru.star.recommendation.dynamic;

import org.springframework.stereotype.Component;
import ru.star.recommendation.repository.TransactionsRepository;
import ru.star.recommendation.rules.Types;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * TRANSACTION_SUM_COMPARE_DEPOSIT_WITHDRAW: сумма пополнений по продуктам типа X
 * сравнивается с суммой трат по ним же.
 * Аргументы: [тип продукта, операция сравнения].
 */
@Component
public class TransactionSumCompareDepositWithdrawQueryHandler extends AbstractRuleQueryHandler {

    private final TransactionsRepository repository;

    public TransactionSumCompareDepositWithdrawQueryHandler(TransactionsRepository repository) {
        this.repository = repository;
    }

    @Override
    public QueryType type() {
        return QueryType.TRANSACTION_SUM_COMPARE_DEPOSIT_WITHDRAW;
    }

    @Override
    public void validate(List<String> arguments) {
        requireArgumentCount(arguments, 2);
        requireProductType(arguments.get(0));
        requireOperator(arguments.get(1));
    }

    @Override
    public boolean evaluate(UUID userId, List<String> arguments) {
        String productType = arguments.get(0);
        BigDecimal deposit = repository.sumAmount(userId, productType, Types.DEPOSIT);
        BigDecimal withdraw = repository.sumAmount(userId, productType, Types.WITHDRAW);
        return requireOperator(arguments.get(1)).test(deposit, withdraw);
    }
}
