package com.smartfactory.dto;

import java.math.BigDecimal;

public record MachineCapacityResponse(
    String capability,
    String phaseType,
    long totalMachines,
    long availableMachines,
    BigDecimal totalCapacity,
    BigDecimal availableCapacity,
    BigDecimal totalProductionRate,
    String status,
    String explanation
) {}
