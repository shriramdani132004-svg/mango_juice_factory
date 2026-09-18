package com.smartfactory.dto;

import java.math.BigDecimal;

public record MachineInfoDto(
    Long machineId,
    String machineCode,
    String name,
    String capability,
    String status,
    BigDecimal capacity,
    BigDecimal productionRate,
    Integer healthScore,
    BigDecimal temperature,
    BigDecimal rpm,
    BigDecimal vibration,
    BigDecimal powerConsumption,
    BigDecimal progress
) {}
