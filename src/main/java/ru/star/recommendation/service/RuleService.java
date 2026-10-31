package ru.star.recommendation.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.star.recommendation.dto.RuleQueryDto;
import ru.star.recommendation.dto.RuleRequest;
import ru.star.recommendation.dto.RuleResponse;
import ru.star.recommendation.dynamic.DynamicRuleEvaluator;
import ru.star.recommendation.dynamic.InvalidRuleException;
import ru.star.recommendation.entity.RuleEntity;
import ru.star.recommendation.entity.RuleQueryEntity;
import ru.star.recommendation.repository.RuleRepository;

import java.util.List;
import java.util.UUID;

/**
 * Управление динамическими правилами рекомендаций.
 */
@Service
public class RuleService {

    private final RuleRepository ruleRepository;
    private final DynamicRuleEvaluator evaluator;
    private final RuleStatsService statsService;

    public RuleService(RuleRepository ruleRepository,
                       DynamicRuleEvaluator evaluator,
                       RuleStatsService statsService) {
        this.ruleRepository = ruleRepository;
        this.evaluator = evaluator;
        this.statsService = statsService;
    }

    @Transactional
    public RuleResponse create(RuleRequest request) {
        // некорректное правило отклоняем сразу, чтобы оно не ломало выдачу рекомендаций
        request.rule().forEach(evaluator::validate);

        RuleEntity entity = new RuleEntity(request.productName(), request.productId(), request.productText());
        for (RuleQueryDto query : request.rule()) {
            entity.addQuery(new RuleQueryEntity(query.query(), query.negate(), query.arguments()));
        }
        RuleEntity saved = ruleRepository.save(entity);
        statsService.createCounter(saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RuleResponse> findAll() {
        return ruleRepository.findAll().stream().map(this::toResponse).toList();
    }

    /**
     * Удаляет правило (его счётчик срабатываний удаляется каскадно вместе с ним).
     * Ключ — либо сгенерированный id правила (число),
     * либо id продукта (UUID): тогда удаляются все правила этого продукта.
     * Если такого правила нет, ничего не происходит.
     */
    @Transactional
    public void delete(String key) {
        try {
            ruleRepository.deleteById(Long.parseLong(key));
            return;
        } catch (NumberFormatException ignored) {
            // не число — пробуем как UUID продукта
        }
        try {
            ruleRepository.deleteByProductId(UUID.fromString(key));
        } catch (IllegalArgumentException e) {
            throw new InvalidRuleException("Идентификатор должен быть числом или UUID: " + key);
        }
    }

    private RuleResponse toResponse(RuleEntity entity) {
        List<RuleQueryDto> rule = entity.getQueries().stream()
                .map(query -> new RuleQueryDto(query.getQueryType(), List.copyOf(query.getArguments()), query.isNegate()))
                .toList();
        return new RuleResponse(entity.getId(), entity.getProductName(), entity.getProductId(),
                entity.getProductText(), rule);
    }
}
