package com.smartfactory.dto;

import java.math.BigDecimal;

public record OrderListItemDto(
    Long id,
    String orderNumber,
    String productName,
    String productCode,
    BigDecimal inputQuantity,
    String inputUnit,
    Integer bottleSizeMl,
    Integer priority,
    String status,
    BigDecimal estimatedOutputQty,
    String estimatedOutputUnit,
    Integer estimatedDurationMinutes,
    String createdAt
) {}
