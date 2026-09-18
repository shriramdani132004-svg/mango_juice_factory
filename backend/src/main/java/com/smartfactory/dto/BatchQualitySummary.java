package com.smartfactory.dto;

import com.smartfactory.enums.QualityCheckStatus;
import com.smartfactory.enums.QualityResult;

import java.util.List;

public record BatchQualitySummary(
    Long batchId,
    String batchCode,
    Long orderId,
    String orderNumber,
    QualityCheckStatus overallStatus,
    QualityResult overallResult,
    List<QualityPhaseResult> phaseResults,
    List<QualityCheckDto> failedCheckDetails,
    long totalChecks,
    long passedChecks,
    long failedChecks,
    String message
) {}
