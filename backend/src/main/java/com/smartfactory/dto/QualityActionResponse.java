package com.smartfactory.dto;

import com.smartfactory.enums.QualityCheckStatus;

public record QualityActionResponse(
    Long batchId,
    String batchCode,
    QualityCheckStatus newStatus,
    String action,
    String message
) {}
