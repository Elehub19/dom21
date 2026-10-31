package ru.star.recommendation.dto;

import java.util.List;

/**
 * Ответ GET /rule.
 */
public record RuleListResponse(List<RuleResponse> data) {
}
