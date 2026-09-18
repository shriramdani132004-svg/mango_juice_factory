package com.smartfactory.dto;

import java.math.BigDecimal;

public record PhaseCompletionDto(
    Long phaseId,
    Integer phaseNumber,
    String phaseName,
    String phaseType,
    BigDecimal inputQuantity,
    BigDecimal processedQuantity,
    BigDecimal outputQuantity,
    BigDecimal wasteQuantity,
    BigDecimal rejectedQuantity,
    Long machinesUsed,
    BigDecimal averageProductionRate,
    Integer processingTimeSeconds,
    String outputBatchCode,
    String status
) {}
