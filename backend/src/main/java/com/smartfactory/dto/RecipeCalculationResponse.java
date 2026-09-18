package com.smartfactory.dto;

import java.math.BigDecimal;
import java.util.List;

public record RecipeCalculationResponse(
    Long recipeId,
    String recipeName,
    Long productId,
    String productName,
    BigDecimal requestedQuantity,
    String outputUnit,
    List<RecipeRequirementDto> requirements
) {}
