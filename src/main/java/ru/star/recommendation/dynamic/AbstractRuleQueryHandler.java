package ru.star.recommendation.dynamic;

import ru.star.recommendation.rules.Types;

import java.util.List;

/**
 * Общие проверки аргументов для обработчиков запросов.
 */
abstract class AbstractRuleQueryHandler implements RuleQueryHandler {

    protected void requireArgumentCount(List<String> arguments, int expected) {
        if (arguments == null || arguments.size() != expected) {
            throw new InvalidRuleException("Запрос " + type() + " принимает аргументов: " + expected);
        }
    }

    protected String requireProductType(String value) {
        if (value == null || !Types.PRODUCT_TYPES.contains(value)) {
            throw new InvalidRuleException("Недопустимый тип продукта: " + value
                    + ". Допустимые: " + Types.PRODUCT_TYPES);
        }
        return value;
    }

    protected String requireTransactionType(String value) {
        if (value == null || !Types.TRANSACTION_TYPES.contains(value)) {
            throw new InvalidRuleException("Недопустимый тип транзакции: " + value
                    + ". Допустимые: " + Types.TRANSACTION_TYPES);
        }
        return value;
    }

    protected ComparisonOperator requireOperator(String value) {
        return ComparisonOperator.fromSymbol(value)
                .orElseThrow(() -> new InvalidRuleException(
                        "Недопустимая операция сравнения: " + value + ". Допустимые: >, <, =, >=, <="));
    }

    protected int requireNonNegativeInt(String value) {
        try {
            int number = Integer.parseInt(value);
            if (number < 0) {
                throw new InvalidRuleException("Константа должна быть неотрицательной: " + value);
            }
            return number;
        } catch (NumberFormatException e) {
            throw new InvalidRuleException("Константа должна быть целым числом типа int: " + value);
        }
    }
}
