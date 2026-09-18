package com.smartfactory.dto;

import com.smartfactory.enums.QualityCheckStatus;
import com.smartfactory.enums.QualityCheckType;
import com.smartfactory.enums.QualityResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record QualityCheckDto(
    Long id,
    Long batchId,
    String batchCode,
    Long orderId,
    String orderNumber,
    Long phaseId,
    Integer phaseNumber,
    String phaseType,
    QualityCheckType checkType,
    String parameterName,
    BigDecimal observedValue,
    BigDecimal expectedMin,
    BigDecimal expectedMax,
    BigDecimal expectedValue,
    String unit,
    QualityCheckStatus status,
    QualityResult result,
    Boolean mandatory,
    String notes,
    String inspectorName,
    Long machineId,
    String machineCode,
    LocalDateTime createdAt,
    LocalDateTime completedAt,
    LocalDateTime verifiedAt
) {}
