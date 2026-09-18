package com.smartfactory.dto;

import java.util.List;

public record QualityCheckCreateRequest(
    Long batchId,
    Long phaseId,
    Long orderId,
    String checkType,
    String parameterName,
    java.math.BigDecimal observedValue,
    java.math.BigDecimal expectedMin,
    java.math.BigDecimal expectedMax,
    java.math.BigDecimal expectedValue,
    String unit,
    Boolean mandatory,
    Long machineId,
    String notes
) {}
