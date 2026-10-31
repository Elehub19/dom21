package ru.star.recommendation.rules;

import ru.star.recommendation.dto.RecommendationDto;

import java.util.Optional;
import java.util.UUID;

/**
 * Набор правил одной рекомендации.
 */
public interface RecommendationRuleSet {

    /**
     * @return рекомендацию, если пользователь подходит под все правила, иначе пустой Optional
     */
    Optional<RecommendationDto> check(UUID userId);
}
