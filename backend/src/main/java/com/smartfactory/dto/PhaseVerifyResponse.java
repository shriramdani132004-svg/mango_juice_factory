package com.smartfactory.dto;

import java.math.BigDecimal;

public record PhaseVerifyResponse(
    Long phaseId,
    Integer phaseNumber,
    String phaseType,
    String status,
    Long nextPhaseId,
    Integer nextPhaseNumber,
    String nextPhaseStatus,
    String orderStatus,
    String message
) {}
