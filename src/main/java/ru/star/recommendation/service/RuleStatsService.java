package ru.star.recommendation.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.star.recommendation.dto.RuleStatDto;
import ru.star.recommendation.entity.RuleEntity;
import ru.star.recommendation.entity.RuleStatEntity;
import ru.star.recommendation.repository.RuleRepository;
import ru.star.recommendation.repository.RuleStatRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Статистика срабатываний динамических правил.
 */
@Service
public class RuleStatsService {

    private final RuleStatRepository statRepository;
    private final RuleRepository ruleRepository;

    public RuleStatsService(RuleStatRepository statRepository, RuleRepository ruleRepository) {
        this.statRepository = statRepository;
        this.ruleRepository = ruleRepository;
    }

    /**
     * Создаёт нулевой счётчик для нового правила. Вызывается в той же транзакции, что и создание правила.
     */
    @Transactional
    public void createCounter(RuleEntity rule) {
        statRepository.save(new RuleStatEntity(rule));
    }

    /**
     * Увеличивает счётчик правила на 1 (атомарно, на уровне БД).
     */
    @Transactional
    public void registerHit(Long ruleId) {
        statRepository.incrementCount(ruleId);
    }

    /**
     * Статистика по всем правилам; у правил без срабатываний count = 0.
     */
    @Transactional(readOnly = true)
    public List<RuleStatDto> getStats() {
        Map<Long, Long> counts = new HashMap<>();
        for (RuleStatEntity stat : statRepository.findAll()) {
            counts.put(stat.getRuleId(), stat.getCount());
        }
        return ruleRepository.findAllIds().stream()
                .map(ruleId -> new RuleStatDto(ruleId, counts.getOrDefault(ruleId, 0L)))
                .toList();
    }
}
