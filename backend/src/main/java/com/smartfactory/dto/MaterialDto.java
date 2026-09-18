package com.smartfactory.dto;

import java.math.BigDecimal;

public record MaterialDto(
    Long id,
    String name,
    String materialCode,
    String materialType,
    String category,
    String unit,
    String description,
    Boolean isActive,
    BigDecimal density,
    BigDecimal conversionFactor
) {}
