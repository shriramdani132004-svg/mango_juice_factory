package com.smartfactory.dto;

import java.math.BigDecimal;

public record InventoryCheckRequest(
    Long materialId,
    BigDecimal requiredQuantity,
    String unit
) {}
