package com.smartfactory.dto;

public record ProductionEventDto(
    String eventType,
    Long orderId,
    String orderNumber,
    Long batchId,
    String batchCode,
    Long phaseId,
    Integer phaseNumber,
    String phaseType,
    Long machineId,
    String machineCode,
    String message,
    Object data
) {}
