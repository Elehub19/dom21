package ru.star.recommendation.rules;

import java.util.Set;

/**
 * Общие константы типов продуктов и транзакций.
 */
public final class Types {

    public static final String DEBIT = "DEBIT";
    public static final String SAVING = "SAVING";
    public static final String INVEST = "INVEST";
    public static final String CREDIT = "CREDIT";

    public static final String DEPOSIT = "DEPOSIT";
    public static final String WITHDRAW = "WITHDRAW";

    public static final Set<String> PRODUCT_TYPES = Set.of(DEBIT, SAVING, INVEST, CREDIT);
    public static final Set<String> TRANSACTION_TYPES = Set.of(DEPOSIT, WITHDRAW);

    private Types() {
    }
}
