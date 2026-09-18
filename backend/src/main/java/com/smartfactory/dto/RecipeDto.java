package com.smartfactory.dto;

import java.math.BigDecimal;
import java.util.List;

public record RecipeDto(
    Long id,
    Long productId,
    String productName,
    String name,
    Integer version,
    BigDecimal yieldPercentage,
    BigDecimal outputQuantity,
    String outputUnit,
    BigDecimal batchSize,
    Boolean isActive,
    List<RecipeMaterialDto> materials
) {}
