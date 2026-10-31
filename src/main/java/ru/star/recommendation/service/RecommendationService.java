package ru.star.recommendation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.star.recommendation.dto.RecommendationDto;
import ru.star.recommendation.dynamic.DynamicRuleEvaluator;
import ru.star.recommendation.dynamic.InvalidRuleException;
import ru.star.recommendation.entity.RuleEntity;
import ru.star.recommendation.repository.RuleRepository;
import ru.star.recommendation.rules.RecommendationRuleSet;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Собирает рекомендации: сначала фиксированные наборы правил, затем динамические правила из БД.
 * Каждое сработавшее динамическое правило увеличивает свой счётчик срабатываний.
 * Один и тот же продукт в ответ попадает не больше одного раза.
 */
@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private final List<RecommendationRuleSet> ruleSets;
    private final RuleRepository ruleRepository;
    private final DynamicRuleEvaluator evaluator;
    private final RuleStatsService statsService;

    public RecommendationService(List<RecommendationRuleSet> ruleSets,
                                 RuleRepository ruleRepository,
                                 DynamicRuleEvaluator evaluator,
                                 RuleStatsService statsService) {
        this.ruleSets = ruleSets;
        this.ruleRepository = ruleRepository;
        this.evaluator = evaluator;
        this.statsService = statsService;
    }

    public List<RecommendationDto> getRecommendations(UUID userId) {
        Map<UUID, RecommendationDto> result = new LinkedHashMap<>();

        for (RecommendationRuleSet ruleSet : ruleSets) {
            ruleSet.check(userId).ifPresent(dto -> result.putIfAbsent(dto.id(), dto));
        }

        for (RuleEntity rule : ruleRepository.findAll()) {
            if (matches(rule, userId)) {
                statsService.registerHit(rule.getId());
                result.putIfAbsent(rule.getProductId(),
                        new RecommendationDto(rule.getProductName(), rule.getProductId(), rule.getProductText()));
            }
        }

        return List.copyOf(result.values());
    }

    private boolean matches(RuleEntity rule, UUID userId) {
        try {
            return evaluator.matches(rule, userId);
        } catch (InvalidRuleException e) {
            log.warn("Динамическое правило {} пропущено: {}", rule.getId(), e.getMessage());
            return false;
        }
    }
}
