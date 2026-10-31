package ru.star.recommendation.dto;

import java.util.List;

/**
 * Ответ GET /rule/stats.
 */
public record RuleStatsResponse(List<RuleStatDto> stats) {
}
