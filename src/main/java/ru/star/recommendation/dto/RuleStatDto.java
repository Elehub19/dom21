package ru.star.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Количество срабатываний одного правила.
 */
public record RuleStatDto(
        @JsonProperty("rule_id") Long ruleId,
        @JsonProperty("count") long count) {
}
