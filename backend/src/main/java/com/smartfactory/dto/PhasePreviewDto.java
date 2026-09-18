package com.smartfactory.dto;

import java.math.BigDecimal;

public record PhasePreviewDto(
    Long id,
    Long batchId,
    Integer phaseNumber,
    String phaseName,
    String phaseType,
    String status,
    String requiredMachineCapability,
    Integer estimatedDurationMinutes,
    BigDecimal productionRate
) {}
