package ru.star.recommendation.dto;

import java.util.UUID;

/**
 * Рекомендованный продукт.
 */
public record RecommendationDto(String name, UUID id, String text) {
}
