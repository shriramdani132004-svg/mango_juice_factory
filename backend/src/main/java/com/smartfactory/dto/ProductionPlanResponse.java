package com.smartfactory.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductionPlanResponse(
    Long orderId,
    String orderNumber,
    String productName,
    BigDecimal inputQuantity,
    String inputUnit,
    BigDecimal expectedJuiceVolume,
    BigDecimal expectedBottleCount,
    BigDecimal productionRateBottlesPerHour,
    Integer estimatedDurationMinutes,
    Integer batchCount,
    BigDecimal wastePercentage,
    List<PhasePreviewDto> phases,
    String overallStatus,
    List<RequirementItemDto> requirements
) {}
