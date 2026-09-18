package com.smartfactory.dto;

import java.math.BigDecimal;
import java.util.List;

public record RecipeRequirementDto(
    Long materialId,
    String materialName,
    String materialCode,
    String materialCategory,
    BigDecimal requiredQuantity,
    BigDecimal availableQuantity,
    String unit,
    String status,
    Boolean isOptional
) {}
