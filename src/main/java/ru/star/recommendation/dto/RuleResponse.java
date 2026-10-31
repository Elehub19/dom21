package ru.star.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

/**
 * Динамическое правило в ответах API.
 */
public record RuleResponse(
        @JsonProperty("id") Long id,
        @JsonProperty("product_name") String productName,
        @JsonProperty("product_id") UUID productId,
        @JsonProperty("product_text") String productText,
        @JsonProperty("rule") List<RuleQueryDto> rule) {
}
