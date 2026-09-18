package com.smartfactory.dto;

import java.math.BigDecimal;

public record ProductDto(
    Long id,
    String name,
    String productCode,
    String description,
    Integer bottleSizeMl,
    Boolean isActive
) {}
