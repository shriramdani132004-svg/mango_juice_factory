package com.smartfactory.dto;

import java.math.BigDecimal;

public record InventoryDto(
    Long id,
    Long materialId,
    String materialName,
    String materialCode,
    String materialType,
    BigDecimal currentQuantity,
    BigDecimal reservedQuantity,
    BigDecimal availableQuantity,
    BigDecimal minimumThreshold,
    String unit,
    String location,
    String status
) {}
