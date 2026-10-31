package ru.star.recommendation.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.star.recommendation.dto.RuleQueryDto;
import ru.star.recommendation.dto.RuleRequest;
import ru.star.recommendation.dto.RuleResponse;
import ru.star.recommendation.dynamic.DynamicRuleEvaluator;
import ru.star.recommendation.dynamic.InvalidRuleException;
import ru.star.recommendation.entity.RuleEntity;
import ru.star.recommendation.repository.RuleRepository;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleServiceTest {

    @Mock
    private RuleRepository ruleRepository;

    @Mock
    private DynamicRuleEvaluator evaluator;

    @Mock
    private RuleStatsService statsService;

    @InjectMocks
    private RuleService service;

    private RuleRequest request(UUID productId) {
        return new RuleRequest("Product", productId, "Text",
                List.of(new RuleQueryDto("USER_OF", List.of("DEBIT"), true)));
    }

    @Test
    void createSavesRuleAndCreatesZeroCounter() {
        UUID productId = UUID.randomUUID();
        when(ruleRepository.save(any(RuleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RuleResponse response = service.create(request(productId));

        assertEquals("Product", response.productName());
        assertEquals(productId, response.productId());
        assertEquals(List.of(new RuleQueryDto("USER_OF", List.of("DEBIT"), true)), response.rule());
        verify(statsService).createCounter(any(RuleEntity.class));
    }

    @Test
    void createRejectsInvalidRuleWithoutSaving() {
        doThrow(new InvalidRuleException("bad")).when(evaluator).validate(any(RuleQueryDto.class));

        assertThrows(InvalidRuleException.class, () -> service.create(request(UUID.randomUUID())));

        verifyNoInteractions(ruleRepository);
        verifyNoInteractions(statsService);
    }

    @Test
    void deleteByNumericKeyDeletesRuleById() {
        service.delete("5");

        verify(ruleRepository).deleteById(5L);
    }

    @Test
    void deleteByUuidKeyDeletesRulesOfProduct() {
        UUID productId = UUID.randomUUID();

        service.delete(productId.toString());

        verify(ruleRepository).deleteByProductId(productId);
    }

    @Test
    void deleteWithInvalidKeyIsRejected() {
        assertThrows(InvalidRuleException.class, () -> service.delete("abc"));
    }
}
