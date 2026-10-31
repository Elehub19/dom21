package ru.star.recommendation.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.star.recommendation.dto.RecommendationDto;
import ru.star.recommendation.dynamic.DynamicRuleEvaluator;
import ru.star.recommendation.dynamic.InvalidRuleException;
import ru.star.recommendation.entity.RuleEntity;
import ru.star.recommendation.repository.RuleRepository;
import ru.star.recommendation.rules.RecommendationRuleSet;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    private final UUID userId = UUID.randomUUID();

    @Mock
    private RuleRepository ruleRepository;

    @Mock
    private DynamicRuleEvaluator evaluator;

    @Mock
    private RuleStatsService statsService;

    private RecommendationService service(RecommendationRuleSet... ruleSets) {
        return new RecommendationService(List.of(ruleSets), ruleRepository, evaluator, statsService);
    }

    private static RuleEntity rule(Long id, String name, UUID productId, String text) {
        RuleEntity rule = new RuleEntity(name, productId, text);
        ReflectionTestUtils.setField(rule, "id", id);
        return rule;
    }

    @Test
    void collectsOnlyNonEmptyFixedResults() {
        RecommendationDto dto = new RecommendationDto("A", UUID.randomUUID(), "text");
        RecommendationRuleSet matching = id -> Optional.of(dto);
        RecommendationRuleSet notMatching = id -> Optional.empty();
        when(ruleRepository.findAll()).thenReturn(List.of());

        assertEquals(List.of(dto), service(matching, notMatching).getRecommendations(userId));
    }

    @Test
    void returnsEmptyListWhenNothingMatches() {
        when(ruleRepository.findAll()).thenReturn(List.of());

        assertTrue(service(id -> Optional.empty()).getRecommendations(userId).isEmpty());
    }

    @Test
    void addsDynamicRuleAndCountsHitWhenItMatches() {
        UUID productId = UUID.randomUUID();
        RuleEntity rule = rule(10L, "Dynamic", productId, "dynamic text");
        when(ruleRepository.findAll()).thenReturn(List.of(rule));
        when(evaluator.matches(rule, userId)).thenReturn(true);

        List<RecommendationDto> result = service().getRecommendations(userId);

        assertEquals(List.of(new RecommendationDto("Dynamic", productId, "dynamic text")), result);
        verify(statsService).registerHit(10L);
    }

    @Test
    void doesNotCountHitWhenRuleDoesNotMatch() {
        RuleEntity rule = rule(10L, "Dynamic", UUID.randomUUID(), "text");
        when(ruleRepository.findAll()).thenReturn(List.of(rule));
        when(evaluator.matches(rule, userId)).thenReturn(false);

        assertTrue(service().getRecommendations(userId).isEmpty());
        verifyNoInteractions(statsService);
    }

    @Test
    void doesNotDuplicateProductAlreadyRecommendedByFixedRuleButStillCountsHit() {
        UUID productId = UUID.randomUUID();
        RecommendationDto fixed = new RecommendationDto("Fixed", productId, "fixed text");
        RuleEntity dynamicRule = rule(5L, "Dynamic", productId, "dynamic text");
        when(ruleRepository.findAll()).thenReturn(List.of(dynamicRule));
        when(evaluator.matches(dynamicRule, userId)).thenReturn(true);

        List<RecommendationDto> result = service(id -> Optional.of(fixed)).getRecommendations(userId);

        assertEquals(List.of(fixed), result);
        verify(statsService).registerHit(5L);
    }

    @Test
    void brokenDynamicRuleIsSkippedAndOthersStillWork() {
        RuleEntity broken = rule(1L, "Broken", UUID.randomUUID(), "text");
        UUID goodId = UUID.randomUUID();
        RuleEntity good = rule(2L, "Good", goodId, "good text");
        when(ruleRepository.findAll()).thenReturn(List.of(broken, good));
        when(evaluator.matches(broken, userId)).thenThrow(new InvalidRuleException("bad"));
        when(evaluator.matches(good, userId)).thenReturn(true);

        List<RecommendationDto> result = service().getRecommendations(userId);

        assertEquals(List.of(new RecommendationDto("Good", goodId, "good text")), result);
        verify(statsService).registerHit(2L);
        verifyNoMoreInteractions(statsService);
    }
}
