package com.smartfactory.dto;

import java.math.BigDecimal;

public record RequirementItemDto(
    Long id,
    String requirementType,
    String itemName,
    String itemType,
    String itemCode,
    Long materialId,
    BigDecimal requiredQuantity,
    String requiredUnit,
    BigDecimal availableQuantity,
    String status,
    Boolean mandatory,
    String explanation
) {}
