package com.smartfactory.dto;

import java.math.BigDecimal;
import java.util.List;

public record PhaseExecutionDto(
    Long phaseId,
    Long orderId,
    String orderNumber,
    String productName,
    Integer phaseNumber,
    String phaseName,
    String phaseType,
    String status,
    BigDecimal inputQuantity,
    BigDecimal processedQuantity,
    BigDecimal outputQuantity,
    BigDecimal wasteQuantity,
    BigDecimal productionRate,
    BigDecimal progress,
    Integer estimatedDurationMinutes,
    Integer elapsedSeconds,
    Integer remainingSeconds,
    String requiredMachineCapability,
    List<MachineInfoDto> assignedMachines,
    List<TelemetrySnapshotDto> telemetry,
    String mode
) {}
