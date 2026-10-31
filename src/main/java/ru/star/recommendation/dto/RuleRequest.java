package ru.star.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Тело запроса POST /rule.
 */
public record RuleRequest(
        @JsonProperty("product_name") @NotBlank String productName,
        @JsonProperty("product_id") @NotNull UUID productId,
        @JsonProperty("product_text") @NotBlank String productText,
        @JsonProperty("rule") @NotEmpty List<@NotNull @Valid RuleQueryDto> rule) {
}
