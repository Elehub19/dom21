package ru.star.recommendation.rules;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.star.recommendation.repository.TransactionsRepository;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Invest500RuleSetTest {

    private final UUID userId = UUID.randomUUID();

    @Mock
    private TransactionsRepository repository;

    @InjectMocks
    private Invest500RuleSet ruleSet;

    @Test
    void recommendsWhenAllRulesMatch() {
        when(repository.usesProductType(userId, Types.DEBIT)).thenReturn(true);
        when(repository.usesProductType(userId, Types.INVEST)).thenReturn(false);
        when(repository.sumAmount(userId, Types.SAVING, Types.DEPOSIT)).thenReturn(BigDecimal.valueOf(1001));

        var result = ruleSet.check(userId);

        assertTrue(result.isPresent());
        assertEquals(Invest500RuleSet.ID, result.get().id());
    }

    @Test
    void noRecommendationWhenUserHasInvest() {
        when(repository.usesProductType(userId, Types.DEBIT)).thenReturn(true);
        when(repository.usesProductType(userId, Types.INVEST)).thenReturn(true);

        assertTrue(ruleSet.check(userId).isEmpty());
    }

    @Test
    void noRecommendationWhenSavingSumIsExactlyThreshold() {
        when(repository.usesProductType(userId, Types.DEBIT)).thenReturn(true);
        when(repository.usesProductType(userId, Types.INVEST)).thenReturn(false);
        when(repository.sumAmount(userId, Types.SAVING, Types.DEPOSIT)).thenReturn(BigDecimal.valueOf(1000));

        assertTrue(ruleSet.check(userId).isEmpty());
    }
}
