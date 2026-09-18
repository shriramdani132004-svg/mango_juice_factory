package com.smartfactory.dto;

import com.smartfactory.enums.QualityCheckStatus;
import com.smartfactory.enums.QualityResult;

import java.util.List;

public record QualityCheckInspectionRequest(
    Long qualityCheckId,
    java.math.BigDecimal observedValue,
    QualityResult result,
    String notes
) {}
