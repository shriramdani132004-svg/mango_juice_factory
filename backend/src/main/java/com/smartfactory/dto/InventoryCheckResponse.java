package com.smartfactory.dto;

import java.math.BigDecimal;

public record InventoryCheckResponse(
    Long materialId,
    String materialName,
    String materialCode,
    BigDecimal required,
    BigDecimal available,
    String unit,
    String status,
    String explanation
) {}
