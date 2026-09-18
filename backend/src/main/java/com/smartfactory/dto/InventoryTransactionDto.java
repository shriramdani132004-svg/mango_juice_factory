package com.smartfactory.dto;

import java.math.BigDecimal;

public record InventoryTransactionDto(
    Long id,
    Long materialId,
    String materialName,
    String transactionType,
    BigDecimal quantity,
    String unit,
    BigDecimal previousQuantity,
    BigDecimal resultingQuantity,
    String referenceType,
    Long referenceId,
    String reason,
    String notes,
    String createdAt
) {}
