package com.smartfactory.dto;

import java.math.BigDecimal;

public record TelemetrySnapshotDto(
    Long machineId,
    String machineCode,
    BigDecimal temperature,
    BigDecimal rpm,
    BigDecimal vibration,
    BigDecimal powerConsumption,
    Integer healthScore,
    String status,
    BigDecimal jobProgress
) {}
