package ru.star.recommendation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Запрос внутри динамического правила.
 */
public record RuleQueryDto(
        @NotBlank String query,
        @NotNull List<@NotNull String> arguments,
        boolean negate) {
}
