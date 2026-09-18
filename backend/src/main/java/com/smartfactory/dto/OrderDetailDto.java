package com.smartfactory.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderDetailDto(
    Long id,
    String orderNumber,
    Long productId,
    String productName,
    String productCode,
    Long recipeId,
    String recipeName,
    BigDecimal inputQuantity,
    String inputUnit,
    Integer bottleSizeMl,
    Integer priority,
    String deadline,
    String status,
    BigDecimal estimatedOutputQty,
    String estimatedOutputUnit,
    BigDecimal estimatedProductionRate,
    Integer estimatedDurationMinutes,
    List<RequirementItemDto> requirements,
    List<PhasePreviewDto> phases,
    String overallStatus,
    String createdAt
) {}
