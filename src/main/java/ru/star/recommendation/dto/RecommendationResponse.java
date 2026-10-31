package ru.star.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

/**
 * Ответ REST API: id пользователя и список рекомендаций.
 */
public record RecommendationResponse(
        @JsonProperty("user_id") UUID userId,
        @JsonProperty("recommendations") List<RecommendationDto> recommendations) {
}
