package ru.star.recommendation.dynamic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.star.recommendation.dto.RuleQueryDto;
import ru.star.recommendation.entity.RuleEntity;
import ru.star.recommendation.entity.RuleQueryEntity;
import ru.star.recommendation.repository.TransactionsRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamicRuleEvaluatorTest {

    private final UUID userId = UUID.randomUUID();

    @Mock
    private TransactionsRepository repository;

    private DynamicRuleEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new DynamicRuleEvaluator(List.of(
                new UserOfQueryHandler(repository),
                new ActiveUserOfQueryHandler(repository),
                new TransactionSumCompareQueryHandler(repository),
                new TransactionSumCompareDepositWithdrawQueryHandler(repository)));
    }

    private static RuleEntity rule(RuleQueryEntity... queries) {
        RuleEntity rule = new RuleEntity("name", UUID.randomUUID(), "text");
        for (RuleQueryEntity query : queries) {
            rule.addQuery(query);
        }
        return rule;
    }

    private static RuleQueryEntity query(String type, boolean negate, String... arguments) {
        return new RuleQueryEntity(type, negate, List.of(arguments));
    }

    private static RuleQueryDto dto(String type, String... arguments) {
        return new RuleQueryDto(type, List.of(arguments), false);
    }

    // ---- вычисление ----

    @Test
    void userOfIsTrueWhenUserHasTransactions() {
        when(repository.usesProductType(userId, "CREDIT")).thenReturn(true);

        assertTrue(evaluator.matches(rule(query("USER_OF", false, "CREDIT")), userId));
    }

    @Test
    void negateInvertsResult() {
        when(repository.usesProductType(userId, "CREDIT")).thenReturn(true);

        assertFalse(evaluator.matches(rule(query("USER_OF", true, "CREDIT")), userId));
    }

    @Test
    void activeUserNeedsAtLeastFiveTransactions() {
        when(repository.countTransactions(userId, "DEBIT")).thenReturn(5L);
        when(repository.countTransactions(userId, "SAVING")).thenReturn(4L);

        assertTrue(evaluator.matches(rule(query("ACTIVE_USER_OF", false, "DEBIT")), userId));
        assertFalse(evaluator.matches(rule(query("ACTIVE_USER_OF", false, "SAVING")), userId));
    }

    @Test
    void transactionSumCompareUsesOperatorAndConstant() {
        when(repository.sumAmount(userId, "DEBIT", "DEPOSIT")).thenReturn(BigDecimal.valueOf(100_001));

        assertTrue(evaluator.matches(
                rule(query("TRANSACTION_SUM_COMPARE", false, "DEBIT", "DEPOSIT", ">", "100000")), userId));
        assertFalse(evaluator.matches(
                rule(query("TRANSACTION_SUM_COMPARE", false, "DEBIT", "DEPOSIT", "<=", "100000")), userId));
        assertTrue(evaluator.matches(
                rule(query("TRANSACTION_SUM_COMPARE", false, "DEBIT", "DEPOSIT", "=", "100001")), userId));
    }

    @Test
    void depositWithdrawCompareComparesTwoSums() {
        when(repository.sumAmount(userId, "DEBIT", "DEPOSIT")).thenReturn(BigDecimal.valueOf(300));
        when(repository.sumAmount(userId, "DEBIT", "WITHDRAW")).thenReturn(BigDecimal.valueOf(200));

        assertTrue(evaluator.matches(
                rule(query("TRANSACTION_SUM_COMPARE_DEPOSIT_WITHDRAW", false, "DEBIT", ">")), userId));
        assertFalse(evaluator.matches(
                rule(query("TRANSACTION_SUM_COMPARE_DEPOSIT_WITHDRAW", false, "DEBIT", "<")), userId));
    }

    @Test
    void ruleRequiresAllQueriesToBeTrue() {
        when(repository.usesProductType(userId, "CREDIT")).thenReturn(false);
        when(repository.sumAmount(userId, "DEBIT", "WITHDRAW")).thenReturn(BigDecimal.valueOf(50));

        RuleEntity allTrue = rule(
                query("USER_OF", true, "CREDIT"),
                query("TRANSACTION_SUM_COMPARE", false, "DEBIT", "WITHDRAW", ">", "10"));
        RuleEntity oneFalse = rule(
                query("USER_OF", true, "CREDIT"),
                query("TRANSACTION_SUM_COMPARE", false, "DEBIT", "WITHDRAW", ">", "100"));

        assertTrue(evaluator.matches(allTrue, userId));
        assertFalse(evaluator.matches(oneFalse, userId));
    }

    @Test
    void storedRuleWithUnknownQueryThrows() {
        assertThrows(InvalidRuleException.class,
                () -> evaluator.matches(rule(query("UNKNOWN", false, "DEBIT")), userId));
    }

    // ---- валидация ----

    @Test
    void validQueriesPassValidation() {
        assertDoesNotThrow(() -> evaluator.validate(dto("USER_OF", "DEBIT")));
        assertDoesNotThrow(() -> evaluator.validate(dto("ACTIVE_USER_OF", "INVEST")));
        assertDoesNotThrow(() -> evaluator.validate(dto("TRANSACTION_SUM_COMPARE", "DEBIT", "DEPOSIT", ">=", "0")));
        assertDoesNotThrow(() -> evaluator.validate(dto("TRANSACTION_SUM_COMPARE_DEPOSIT_WITHDRAW", "SAVING", "=")));
    }

    @Test
    void unknownQueryTypeIsRejected() {
        assertThrows(InvalidRuleException.class, () -> evaluator.validate(dto("SOMETHING", "DEBIT")));
    }

    @Test
    void wrongArgumentCountIsRejected() {
        assertThrows(InvalidRuleException.class, () -> evaluator.validate(dto("USER_OF")));
        assertThrows(InvalidRuleException.class, () -> evaluator.validate(dto("USER_OF", "DEBIT", "CREDIT")));
        assertThrows(InvalidRuleException.class,
                () -> evaluator.validate(dto("TRANSACTION_SUM_COMPARE", "DEBIT", "DEPOSIT", ">")));
    }

    @Test
    void unknownProductOrTransactionTypeIsRejected() {
        assertThrows(InvalidRuleException.class, () -> evaluator.validate(dto("USER_OF", "GOLD")));
        assertThrows(InvalidRuleException.class,
                () -> evaluator.validate(dto("TRANSACTION_SUM_COMPARE", "DEBIT", "TRANSFER", ">", "1")));
    }

    @Test
    void badOperatorOrConstantIsRejected() {
        assertThrows(InvalidRuleException.class,
                () -> evaluator.validate(dto("TRANSACTION_SUM_COMPARE", "DEBIT", "DEPOSIT", "!=", "1")));
        assertThrows(InvalidRuleException.class,
                () -> evaluator.validate(dto("TRANSACTION_SUM_COMPARE", "DEBIT", "DEPOSIT", ">", "-5")));
        assertThrows(InvalidRuleException.class,
                () -> evaluator.validate(dto("TRANSACTION_SUM_COMPARE", "DEBIT", "DEPOSIT", ">", "abc")));
        assertThrows(InvalidRuleException.class,
                () -> evaluator.validate(dto("TRANSACTION_SUM_COMPARE", "DEBIT", "DEPOSIT", ">", "99999999999")));
        assertThrows(InvalidRuleException.class,
                () -> evaluator.validate(dto("TRANSACTION_SUM_COMPARE_DEPOSIT_WITHDRAW", "DEBIT", "<>")));
    }
}
