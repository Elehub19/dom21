package ru.star.recommendation.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.star.recommendation.dto.RuleStatDto;
import ru.star.recommendation.entity.RuleEntity;
import ru.star.recommendation.entity.RuleStatEntity;
import ru.star.recommendation.repository.RuleRepository;
import ru.star.recommendation.repository.RuleStatRepository;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleStatsServiceTest {

    @Mock
    private RuleStatRepository statRepository;

    @Mock
    private RuleRepository ruleRepository;

    @InjectMocks
    private RuleStatsService service;

    private static RuleStatEntity stat(long ruleId, long count) {
        RuleStatEntity stat = new RuleStatEntity(new RuleEntity("n", UUID.randomUUID(), "t"));
        ReflectionTestUtils.setField(stat, "ruleId", ruleId);
        ReflectionTestUtils.setField(stat, "count", count);
        return stat;
    }

    @Test
    void statsContainAllRulesIncludingZeroCounters() {
        when(ruleRepository.findAllIds()).thenReturn(List.of(1L, 2L, 3L));
        when(statRepository.findAll()).thenReturn(List.of(stat(1L, 5), stat(3L, 0)));

        List<RuleStatDto> stats = service.getStats();

        assertEquals(List.of(
                new RuleStatDto(1L, 5),
                new RuleStatDto(2L, 0),
                new RuleStatDto(3L, 0)), stats);
    }

    @Test
    void registerHitIncrementsCounterInDatabase() {
        service.registerHit(7L);

        verify(statRepository).incrementCount(7L);
    }

    @Test
    void createCounterSavesNewStatForRule() {
        RuleEntity rule = new RuleEntity("n", UUID.randomUUID(), "t");

        service.createCounter(rule);

        verify(statRepository).save(any(RuleStatEntity.class));
    }
}
