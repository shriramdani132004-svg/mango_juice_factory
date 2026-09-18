package com.smartfactory.dto;

import com.smartfactory.enums.QualityCheckStatus;
import com.smartfactory.enums.QualityResult;

import java.util.List;

public record QualityPhaseResult(
    Long phaseId,
    Integer phaseNumber,
    Long batchId,
    String batchCode,
    QualityCheckStatus overallStatus,
    QualityResult overallResult,
    long totalChecks,
    long passedChecks,
    long failedChecks,
    long pendingChecks,
    List<QualityCheckDto> checks,
    String message
) {}
